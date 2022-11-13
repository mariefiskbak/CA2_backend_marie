package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dtos.ConventusResourceDTO;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class PingURL implements Callable<String> {
    String url;

    PingURL(String url) {
        this.url = url;
    }

    @Override
    public String call() throws Exception {
        String result = "Error";
        String resourcesJSON = "";
        try {
            URL siteURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) siteURL
                    .openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            resourcesJSON = HttpUtils.fetchData(url);

        } catch (Exception e) {
            result = "RED";
        }
        return resourcesJSON;
    }
}

public class ConventusResourcesParallelFetcher {
    private static long timeStart;
    private static long firstFetchEnd;

    public static List<String> getResource() throws IOException {
        timeStart = System.nanoTime();
        List<String> idsList = new ArrayList<>();

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
                        String id = jArrayResource.getAsJsonObject().get("id").getAsString();
                        idsList.add(id);
                    }
                }
            } else if (external.getAsJsonObject().has("resources")) {
                JsonArray jArray = external.getAsJsonObject().get("resources").getAsJsonArray();

                for (JsonElement jsonElement : jArray) {
                    String id = jsonElement.getAsJsonObject().get("id").toString();
                    idsList.add(id);
                }
            }
        }

        System.out.println("Parallel: " + idsList.toString());
        firstFetchEnd = System.nanoTime();
        return idsList;
    }

    public List<ConventusResourceDTO> getBFFInfo(String selectedDate) throws IOException, ExecutionException, InterruptedException {
        List<ConventusResourceDTO> conventusResourceDTOList = new ArrayList<>();
        List<String> resources = getResource();
        List<String> urls = new ArrayList<>();
        String onlyDate = selectedDate.substring(0, 10);
        LocalDate date = LocalDate.parse(onlyDate);
        System.out.println("Valgte dag: " + date.toString());

        String startDate = date.minusDays(6).toString();
        String endDate = date.plusDays(6).toString();

        long timeBeforeSecondFetch = System.nanoTime();
        for (String resource : resources) {
            String url = String.format("https://www.conventus.dk/publicBooking/api/bookings?organization=13688&from=%s&to=%s&resources=%s", startDate, endDate, resource);
            urls.add(url);
        }

        //HER der skal forskllige tråde til de forskellige url'er
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<String>> futures = new ArrayList<>();
        List<String> results = new ArrayList<>();
        for (String url : urls) {
            Future<String> fut = executor.submit(new PingURL(url));
            futures.add(fut);
        }
        for (Future<String> future : futures) {
            String futureString = future.get();
            results.add(futureString);
        }


        long timeAfterParrallelFetches = System.nanoTime();
        //results indeholder... det der skal behandles nedenfor..?
        int id = 1;
        for (String result : results) {
            String resourcesJSON = result;
            JsonArray json = JsonParser.parseString(resourcesJSON).getAsJsonArray();
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



        }

        long timeEnd = System.nanoTime();
        long firstFetch = (firstFetchEnd - timeStart) / 1_000_000;
        System.out.println("Parallel: Time to do first fetch: " + firstFetch + "ms.");

        long beforeSecondFetch = (timeBeforeSecondFetch - timeStart) / 1_000_000;
        System.out.println("Parallel: Before second Fetch the parallel ones: " + beforeSecondFetch + "ms.");

        long afterSecondFetch = (timeAfterParrallelFetches - timeStart) / 1_000_000;
        System.out.println("Parallel: Just after parallel Fetches: " + afterSecondFetch + "ms.");

        long total = (timeEnd - timeStart) / 1_000_000;
        System.out.println("Parallel: Total time parrallel fetch: " + total + "ms.");
        return conventusResourceDTOList;
    }
}
