package com.example.latlngupdater;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.mongodb.client.model.IndexOptions;

public class MongoDBIndexCreation {
    public static void main(String[] args) {
        // Connect to MongoDB
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase("geointelligence");

        // Get the collection
        MongoCollection<Document> collection = database.getCollection("master_data_city_pincode_details");

        // Create an index on the "cityname" field
        IndexOptions indexOptions = new IndexOptions().name("cityname_index");
        collection.createIndex(new Document("cityname", 1), indexOptions);

        // Create an index on the "pincode" field
        indexOptions = new IndexOptions().name("pincode_index");
        collection.createIndex(new Document("pincode", 1), indexOptions);

        // Close the MongoDB connection
        mongoClient.close();
    }
}
