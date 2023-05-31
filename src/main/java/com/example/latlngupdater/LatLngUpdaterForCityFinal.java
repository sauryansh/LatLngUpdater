package com.example.latlngupdater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LatLngUpdaterForCityFinal {
    private RestTemplate restTemplate;
    private HashMap<String, Document> resultMap = new HashMap<>();
    private HashMap<String, String> cityWithLatLng = new HashMap<>();
    private HashMap<String, String> cityWithNoLatLng = new HashMap<>();

    public LatLngUpdaterForCityFinal(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static void main(String[] args) {
        // Create RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Create LatLngUpdater instance with RestTemplate
        LatLngUpdaterForCityFinal example = new LatLngUpdaterForCityFinal(restTemplate);
        example.updateLatLong();
    }

    private void updateLatLong() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("geointelligence");
        MongoCollection<Document> collection = database.getCollection("geo_intelligence_city_details");
        Document query = new Document();
        query.append("hubdetails.state", new Document("$regex", "maharashtra").append("$options", "i"));
        collection.find(query).forEach((Consumer<? super Document>) document -> {
            String id = document.getObjectId("_id").toString();
            Document hubDetails = document.get("hubdetails", Document.class);
            resultMap.put(id, hubDetails);
        });

        // Create a thread pool with a fixed number of threads
        int numThreads = Runtime.getRuntime().availableProcessors(); // Adjust the number of threads as per your requirements
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (Map.Entry<String, Document> entry : resultMap.entrySet()) {
            executor.execute(() -> {
                Document hubDetails = entry.getValue();
                String city = hubDetails.getString("city");
                String state = hubDetails.getString("state");
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

                        Document updateQuery = new Document();
                        updateQuery.append("hubdetails.latitude", lat);
                        updateQuery.append("hubdetails.longitude", lon);
                        ObjectId objectId = new ObjectId(entry.getKey());

                        Document updateDocument = new Document("$set", updateQuery);

                        try {
                            UpdateResult updateResult = collection.updateOne(new Document("_id", objectId), updateDocument);
                            System.out.println("ObjectId: " + objectId + " : " + updateResult);
                            if (updateResult.getModifiedCount() > 0) {
                                cityWithLatLng.put(city, "Lat Lng Available");
                            }
                        } catch (MongoException e) {
                            // Handle the exception or log the error
                            System.out.println("Error updating document for " + city + ": " + e.getMessage());
                        }
                    } else {
                        System.out.println("Lat Lng Not Available for " + city);
                        cityWithNoLatLng.put(city, "Lat Lng Not Available");
                        //writeToFile

                    }
                }
            });
        }

        // Shutdown the thread pool and wait for all threads to complete
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            // Handle the exception or log the error
            e.printStackTrace();
        }

        System.out.println("Cities with Lat Lng Available: " + cityWithLatLng);
        System.out.println("Cities with Lat Lng Not Available: " + cityWithNoLatLng);
    }
}
