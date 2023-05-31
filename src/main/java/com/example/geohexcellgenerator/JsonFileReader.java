package com.example.geohexcellgenerator;

import com.example.model.Model;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.List;

public class JsonFileReader {
    public static void main(String[] args) {
        String filePath = "src/main/resources/PincodeBoundaryServiceTest_autocompleteListV2.json";
        Gson gson = new Gson();

        try {
            Type listType = new TypeToken<List<Model>>(){}.getType();
            List<Model> yourObjects = gson.fromJson(new FileReader(filePath), listType);
            System.out.println(yourObjects.get(0).getPincode());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
