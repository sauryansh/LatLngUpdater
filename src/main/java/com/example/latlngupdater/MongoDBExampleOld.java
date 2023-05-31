package com.example.latlngupdater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MongoDBExampleOld {
    private RestTemplate restTemplate;
    HashMap<String, Document> resultMap = new HashMap<>();
    HashMap<String, String> cityWithLatLng = new HashMap<>();
    HashMap<String, String> cityWithNoLatLng = new HashMap<>();

    public MongoDBExampleOld(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static void main(String[] args) {
        // Create RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Create MongoDBExample instance with RestTemplate
        MongoDBExampleOld example = new MongoDBExampleOld(restTemplate);
        example.updateLatLong();
    }

    private void updateLatLong() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("geointelligence");
        MongoCollection<Document> collection = database.getCollection("geo_hub_active_details");
        Document query = new Document();
        query.append("hubdetails.state", new Document("$regex", "MAHARASHTRA").append("$options", "i"));
        //query.append("hubdetails.city", new Document("$regex", "pune").append("$options", "i"));
        collection.find(query).forEach((Consumer<? super Document>) document -> {

            // Extract the _id and hubdetails fields from the document
            String id = document.getObjectId("_id").toString();
            Document hubDetails = document.get("hubdetails", Document.class);
            // Store the id and hubdetails in the HashMap
            resultMap.put(id, hubDetails);
        });

        //Iterate over HashMap, build the API request, and update the lat long from the response from OpenStreetMap

        for (Map.Entry<String, Document> entry : resultMap.entrySet()) {
            // Fetch city and state from hubdetails
            Document hubDetails = entry.getValue();
            String city = hubDetails.getString("city");
            String state = hubDetails.getString("state");

            // Build the API request URL
            String apiUrl = "http://localhost:8080/search?q=" + city + "&state=" + state + "&country=india&format=json&addressdetails=1&limit=1";

            // Make the API request
            String response = restTemplate.getForObject(apiUrl, String.class);

            // Parse the response and extract lat long
            JsonParser parser = new JsonParser();
            JsonElement jsonResponse = parser.parse(response);

            if (jsonResponse.isJsonArray()) {
                JsonArray jsonArray = jsonResponse.getAsJsonArray();
                if (jsonArray.size() > 0) {
                    JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
                    String lat = jsonObject.get("lat").getAsString();
                    String lon = jsonObject.get("lon").getAsString();

                    // Update the lat long in the document
                    Document updateQuery = new Document();
                    updateQuery.append("hubdetails.latitude", lat);
                    updateQuery.append("hubdetails.longitude", lon);
                    //System.out.println(updateQuery);
                    //System.out.println("Lat Lng Available for " + city + " there lat is " + lat + " and long is " + lon);
                    cityWithLatLng.put(city, "Lat Lng Available");
                    // Convert the key from String to ObjectId
                    ObjectId objectId = new ObjectId(entry.getKey());

                    System.out.println("Mongodb Key: " + entry.getKey() + " " + collection.updateOne(new Document("_id", objectId), new Document("$set", updateQuery)));
                } else {
                    System.out.println("Lat Lng Not Available for " + city);
                    cityWithNoLatLng.put(city, "Lat Lng Not Available");
                }
            }
        }

        System.out.println("Cities with Lat Lng Available: " + cityWithLatLng);
        System.out.println("Cities with Lat Lng Not Available: " + cityWithNoLatLng);
    }
}
