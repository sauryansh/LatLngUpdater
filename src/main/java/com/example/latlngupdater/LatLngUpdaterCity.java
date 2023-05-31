package com.example.latlngupdater;

import com.example.model.CityDetails;
import com.example.model.HubDetails;
import com.google.gson.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.web.client.RestTemplate;
import com.mongodb.client.model.UpdateOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LatLngUpdaterCity {
    private RestTemplate restTemplate;
    private HashMap<String, Document> resultMap = new HashMap<>();
    private HashMap<String, String> cityWithLatLng = new HashMap<>();
    private HashMap<String, String> cityWithNoLatLng = new HashMap<>();

    public LatLngUpdaterCity(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static void main(String[] args) {
        // Create RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Create LatLngUpdater instance with RestTemplate
        LatLngUpdaterCity example = new LatLngUpdaterCity(restTemplate);
        example.updateLatLongByCity();
    }

    private void updateLatLongByCity() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("geointelligence");
        MongoCollection<Document> collection = database.getCollection("geo_intelligence_pincode_boundaries");
        Document query = new Document();
        // Fetching all the cities from the database
        collection.find(query).forEach((Consumer<? super Document>) document -> {
            String id = document.getObjectId("_id").toString();
            Document properties = document.get("properties", Document.class);
            if (properties != null) {
                resultMap.put(id, properties);
            }
        });

        // Create a thread pool with a fixed number of threads
        int numThreads = Runtime.getRuntime().availableProcessors(); // Adjust the number of threads as per your requirements
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (Map.Entry<String, Document> entry : resultMap.entrySet()) {
            executor.execute(() -> {
                String id = entry.getKey();
                Document properties = entry.getValue();
                String city = !properties.getString("city").isEmpty() ? properties.getString("city").replace(" ", "+") : properties.getString("subDistrict");

                String state = "Maharashtra"; // You can change the state as per your requirements
                String apiUrl = "http://localhost:8080/search?q=" + city + "&state=" + state + "&country=india&format=json&addressdetails=1&limit=1";
                String response = restTemplate.getForObject(apiUrl, String.class);
                JsonParser parser = new JsonParser();
                JsonElement jsonResponse = parser.parse(response);

                if (jsonResponse.isJsonArray()) {
                    JsonArray jsonArray = jsonResponse.getAsJsonArray();
                    if (jsonArray.size() > 0) {
                        JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
                        String lat = jsonObject.get("lat").getAsString();
                        String lon = jsonObject.get("lon").getAsString();
                        String pincodeString= (String) properties.get("pincode");
                        Integer pincode = pincodeString != null ? Integer.parseInt(pincodeString) : null;
                        System.out.println("City: " + city + ", Pincode: " + pincode + ", Latitude: " + lat + ", Longitude: " + lon);
                        CityDetails cityDetails = CityDetails.builder()
                                .code(100)
                                .hubdetails(
                                        HubDetails.builder()
                                                .hubid(null)
                                                .hubname(city)
                                                .hubcode(null)
                                                .areacode(null)
                                                .center_type(null)
                                                .hubzonename(null)
                                                .latitude(jsonObject.get("lat").getAsString())
                                                .longitude(jsonObject.get("lon").getAsString())
                                                .city(city)
                                                .state(state)
                                                .country("India")
                                                .pincode(pincode)
                                                .status("Pending")
                                                .isactive(false)
                                                .build()
                                )
                                .build();

                        // Upsert the record in MongoDB
                        Document document = new Document("$set", Document.parse(new Gson().toJson(cityDetails)));
                        collection.updateOne(new Document("_id", id), document, new UpdateOptions().upsert(true));
                        cityWithLatLng.put(id, lat + "," + lon);
                    } else {
                        cityWithNoLatLng.put(id, city);
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // Handle the exception or log the error
            e.printStackTrace();
        }

        System.out.println("Cities with Lat Lng Not Available: " + cityWithNoLatLng);
    }
}
