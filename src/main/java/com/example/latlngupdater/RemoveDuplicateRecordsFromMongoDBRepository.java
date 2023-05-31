package com.example.latlngupdater;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveDuplicateRecordsFromMongoDBRepository {
    public static void main(String[] args) {
        int count = 0;
        // Connect to MongoDB
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("geointelligence");
        MongoCollection<Document> collection = database.getCollection("geo_intelligence_city_details");

        // Create the aggregation pipeline
        Document groupFields = new Document();
        groupFields.put("hubid", "$hubdetails.hubid");
        groupFields.put("hubname", "$hubdetails.hubname");
        groupFields.put("hubcode", "$hubdetails.hubcode");
        groupFields.put("city", "$hubdetails.city");
        groupFields.put("state", "$hubdetails.state");

        Document groupStage = new Document("$group", new Document("_id", groupFields)
                .append("records", new Document("$push", "$$ROOT")));

        // Execute the aggregation pipeline
        Iterable<Document> result = collection.aggregate(Arrays.asList(groupStage));
        List<ObjectId> recordIds = new ArrayList<>();

        // Process the results
        for (Document document : result) {
            List<Document> records = document.get("records", List.class);
            if (records.size() > 1) {
                for (int i = 1; i < records.size(); i++) {
                    Document record = records.get(i);
                    Object id = record.get("_id");
                    if (id instanceof String) {
                        recordIds.add(new ObjectId((String) id));
                    } else if (id instanceof ObjectId) {
                        recordIds.add((ObjectId) id);
                    }
                    collection.deleteOne(new Document("_id", id));
                    count++;
                }
            }
        }
        System.out.println("Total records: " + recordIds.size());

        // Close the MongoDB client
        mongoClient.close();
    }
}
