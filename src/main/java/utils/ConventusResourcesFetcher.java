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
    private static long timeStart;
    private static long firstFetchEnd;

    public static String getResource() throws IOException {
        timeStart = System.nanoTime();

        String resourcesJSON = HttpUtils.fetchData("https://www.conventus.dk/publicBooking/api/resources?organization=13688");

        JsonObject json = JsonParser.parseString(resourcesJSON).getAsJsonObject();
        StringBuilder resourceIds = new StringBuilder();

        JsonArray externals = json.get("external").getAsJsonArray();
        for (JsonElement external : externals) {
            if (external.getAsJsonObject().has("directories")) {
                JsonArray jArray = external.getAsJsonObject().get("directories").getAsJsonArray();
                for (JsonElement jsonElement : jArray) {
                    JsonArray jArrayResources = jsonElement.getAsJsonObject().get("resources").getAsJsonArray();
                    for (JsonElement jArrayResource : jArrayResources) {
                        String ids = jArrayResource.getAsJsonObject().get("id").getAsString();
                        resourceIds.append(ids);
                        resourceIds.append(";");
                    }
                }
            } else if (external.getAsJsonObject().has("resources")) {
                JsonArray jArray = external.getAsJsonObject().get("resources").getAsJsonArray();
                for (JsonElement jsonElement : jArray) {
                    String ids = jsonElement.getAsJsonObject().get("id").getAsString();
                    resourceIds.append(ids);
                    resourceIds.append(";");
                }
            }
        }

        String resourceIdsString = resourceIds.substring(0, resourceIds.length() - 1);
        //System.out.println(resourceIdsString);

        firstFetchEnd = System.nanoTime();
        return resourceIdsString;
    }

    public List<ConventusResourceDTO> getBFFInfo(String selectedDate) throws IOException {
        List<ConventusResourceDTO> conventusResourceDTOList = new ArrayList<>();
        String resources = getResource();
        String onlyDate = selectedDate.substring(0, 10);
        LocalDate date = LocalDate.parse(onlyDate);
        System.out.println("Valgte dag: " + date.toString());

        String startDate = date.minusDays(6).toString();
        String endDate = date.plusDays(6).toString();

        long timeBeforeSecondFetch = System.nanoTime();
        //Næsten al tiden bliver brugt her,
        //Så hvis jeg skal bruge tråde, så skal kaldet deles op i mindre, som hver tråd tager sig af
        String url = String.format("https://www.conventus.dk/publicBooking/api/bookings?organization=13688&from=%s&to=%s&resources=%s", startDate, endDate, resources);
        String resourcesJSON = HttpUtils.fetchData(url);
        System.out.println(url);
        long timeJustSecondFetch = System.nanoTime();
        JsonArray json = JsonParser.parseString(resourcesJSON).getAsJsonArray();
        long timeAfterSecondFetchAndParse = System.nanoTime();

        int id = 1;
        for (JsonElement jsonElement : json) {
            String text = "";
            String start = "";
            String end = "";
            String backColor = "";
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
                try {
                    backColor = "#" + jsonElement.getAsJsonObject().get("category").getAsJsonObject().get("color").getAsString();
                } catch (Exception e) {
                    backColor = "#007500";
                }

                Long timeStart = Long.parseLong(jsonElement.getAsJsonObject().get("start").getAsString());
                //Date timeS = new Date(timeStart);
                //TODO, ikke sikker på at den der tid duer når det skifter til sommertid
                LocalDateTime startTime = LocalDateTime.ofEpochSecond(timeStart / 1000, 0, ZoneOffset.ofHours(1));
                start = startTime.toString() + ":00";
                System.out.println(startTime);
                Long timeEnd = Long.parseLong(jsonElement.getAsJsonObject().get("end").getAsString());
                //TODO, ikke sikker på at den der tid duer når det skifter til sommertid
                LocalDateTime time = LocalDateTime.ofEpochSecond(timeEnd / 1000, 0, ZoneOffset.ofHours(1));
                end = time.toString() + ":00";
                System.out.println(time);

                System.out.println(backColor);
            }
            if (text != "") {

                ConventusResourceDTO conventusResourceDTO = new ConventusResourceDTO("" + id, text, start, end, backColor);
                id++;
                conventusResourceDTOList.add(conventusResourceDTO);
            }
        }

        //JsonObject organization = json.get("organization").getAsJsonObject();

        //https://www.conventus.dk/publicBooking/api/bookings?organization=13688&from=2022-11-09&to=2022-11-16&resources=7070;7033;7208;7206;6925;27655

        // så skal jeg fiske resource.name resource.resourceGroup.title og start og end ud.
        long timeEnd = System.nanoTime();
        long firstFetch = (firstFetchEnd - timeStart) / 1_000_000;
        System.out.println("Time to do first fetch: " + firstFetch + "ms.");

        long beforeSecondFetch = (timeBeforeSecondFetch - timeStart) / 1_000_000;
        System.out.println("Before second Fetch: " + beforeSecondFetch + "ms.");

        long justSecondFecth = (timeJustSecondFetch - timeStart) / 1_000_000;
        System.out.println("Just after second Fetch: " + justSecondFecth + "ms.");

        long afterSecondFetch = (timeAfterSecondFetchAndParse - timeStart) / 1_000_000;
        System.out.println("After second fetch and parse: " + afterSecondFetch + "ms.");

        long total = (timeEnd - timeStart) / 1_000_000;
        System.out.println("Total time: " + total + "ms.");
        return conventusResourceDTOList;
    }
}
