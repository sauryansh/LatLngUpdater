package com.example.bbox;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class FindClusterDetailsUsingBbox {
    public static void main(String[] args) {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("geointelligence");
        MongoCollection<Document> collection = database.getCollection("geo_intelligence_geo_cell");
    }
}
