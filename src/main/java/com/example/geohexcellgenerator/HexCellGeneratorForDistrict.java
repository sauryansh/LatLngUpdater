package com.example.geohexcellgenerator;

import com.example.model.HexCellGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class HexCellGeneratorForDistrict {

    public static void main(String[] args) throws IOException {
        String geoJSONFilePath = "D:\\cluster\\550_Ahmadabad_Polygon_474_Gujarat_24_2011_c.json";
        // Read the GeoJSON file
        String geoJSON = new String(Files.readAllBytes(Paths.get(geoJSONFilePath)));

        JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(geoJSON);

        ArrayNode arrayNode = (ArrayNode) jsonNode.get("geoCells");

        ArrayList<String> geoCells = new ArrayList<>();
        arrayNode.forEach(node -> geoCells.add(node.asText()));
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("geointelligence");
        MongoCollection<Document> collection = database.getCollection("geo_intelligence_geo_cell_Ahmadabad");


        for(String geoCell : geoCells) {
            HexCellGenerator hexCellGenerator = HexCellGenerator.builder()
                    .hexId(geoCell)
                    .city("Ahmadabad")
                    .state("Gujarat")
                    .deleted(false)
                    .build();



            Document document = new Document();
            document.append("hexId", hexCellGenerator.getHexId());
            document.append("city", hexCellGenerator.getCity());
            document.append("state", hexCellGenerator.getState());
            document.append("deleted", hexCellGenerator.isDeleted());
            collection.insertOne(document);
        }
        mongoClient.close();

    }
}
