package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dtos.ConventusResourceDTO;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConventusResourcesFetcher {

    //TODO, så skal den returnerede streng bygges ind i URLen til et andet fetch, sammen med dagens dato og en uge frem, eller lignende.
    public static String getResource() throws IOException {
        String resourcesJSON = HttpUtils.fetchData("https://www.conventus.dk/publicBooking/api/resources?organization=13688");
        //SKal finde external, directories, resources, id
        //System.out.println("JSON Conventus: " + resourcesJSON);

        JsonObject json = JsonParser.parseString(resourcesJSON).getAsJsonObject();
        //System.out.println("parsed JSON: " + json.toString());

        StringBuilder resourceIds = new StringBuilder();

        JsonArray externals = json.get("external").getAsJsonArray();
        //System.out.println("get parsed as string: " + externals.toString());
        for (JsonElement external : externals) {
            if (external.getAsJsonObject().has("directories")) {
                JsonArray jArray = external.getAsJsonObject().get("directories").getAsJsonArray();
                //System.out.println("jArray: " + jArray);
                for (JsonElement jsonElement : jArray) {
                    JsonArray jArrayResources = jsonElement.getAsJsonObject().get("resources").getAsJsonArray();
                    //          System.out.println("nested resources;: " + jArrayResources);
                    for (JsonElement jArrayResource : jArrayResources) {
                        String ids = jArrayResource.getAsJsonObject().get("id").getAsString();
                        resourceIds.append(ids);
                        resourceIds.append(";");
                        //            System.out.println("IDs: " + ids);
                    }
                }
            } else if (external.getAsJsonObject().has("resources")) {
                JsonArray jArray = external.getAsJsonObject().get("resources").getAsJsonArray();
                //System.out.println("jArrayResources: " + jArray);
                for (JsonElement jsonElement : jArray) {
                    String ids = jsonElement.getAsJsonObject().get("id").getAsString();
                    resourceIds.append(ids);
                    resourceIds.append(";");
                    //System.out.println("IDs: " + ids);
                }
            }
        }

        String resourceIdsString = resourceIds.substring(0, resourceIds.length() - 1);
        //System.out.println(resourceIdsString);
        return resourceIdsString;
    }

    public List<ConventusResourceDTO> getBFFInfo(String selectedDate) throws IOException {
        List<ConventusResourceDTO> conventusResourceDTOList = new ArrayList<>();
        String resources = getResource();
        String onlyDate = selectedDate.substring(0,10);
        LocalDate date = LocalDate.parse(onlyDate);
        System.out.println("Valgte dag: " + date.toString());

        String startDate = date.minusDays(7).toString();
        String endDate = date.plusDays(14).toString();

        String resourcesJSON = HttpUtils.fetchData(String.format("https://www.conventus.dk/publicBooking/api/bookings?organization=13688&from=%s&to=%s&resources=%s", startDate, endDate, resources));

        JsonArray json = JsonParser.parseString(resourcesJSON).getAsJsonArray();
        int id = 1;
        for (JsonElement jsonElement : json) {
            String text = "";
            String start = "";
            String end = "";
            //System.out.println("jsomElement: " + jsonElement);
            String organizationId = jsonElement.getAsJsonObject().get("organization").getAsJsonObject().get("id").getAsString();
            //Get object where organization.id = 13688
            if (organizationId.equals("13688")) {

                if (jsonElement.getAsJsonObject().get("resource").getAsJsonObject().has("resourceGroup")) {
                    String resourceName = jsonElement.getAsJsonObject().get("resource").getAsJsonObject().get("resourceGroup").getAsJsonObject().get("title").getAsString() + " " + jsonElement.getAsJsonObject().get("resource").getAsJsonObject().get("name").getAsString();
                    text = resourceName;
                    System.out.println(resourceName);
                } else {
                    String resourceName = jsonElement.getAsJsonObject().get("resource").getAsJsonObject().get("name").getAsString();
                    text = resourceName;
                    System.out.println(resourceName);
                }
                Long timeStart = Long.parseLong(jsonElement.getAsJsonObject().get("start").getAsString());
                //Date timeS = new Date(timeStart);
                //TODO, ikke sikker på at den der tid duer når det skifter til sommertid
                LocalDateTime startTime = LocalDateTime.ofEpochSecond(timeStart / 1000, 0, ZoneOffset.ofHours(1));
                start = startTime.toString() +":00";
                System.out.println(startTime);
                Long timeEnd = Long.parseLong(jsonElement.getAsJsonObject().get("end").getAsString());
                //TODO, ikke sikker på at den der tid duer når det skifter til sommertid
                LocalDateTime time = LocalDateTime.ofEpochSecond(timeEnd / 1000, 0, ZoneOffset.ofHours(1));
                end = time.toString() + ":00";
                System.out.println(time);
            }
            if(text != "") {

                ConventusResourceDTO conventusResourceDTO = new ConventusResourceDTO("" + id, text, start, end);
                id ++;
                conventusResourceDTOList.add(conventusResourceDTO);
            }
        }

        //JsonObject organization = json.get("organization").getAsJsonObject();

        //https://www.conventus.dk/publicBooking/api/bookings?organization=13688&from=2022-11-09&to=2022-11-16&resources=7070;7033;7208;7206;6925;27655

        // så skal jeg fiske resource.name resource.resourceGroup.title og start og end ud.
        return conventusResourceDTOList;
    }
}
