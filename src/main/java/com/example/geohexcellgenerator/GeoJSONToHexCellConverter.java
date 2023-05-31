package com.example.geohexcellgenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryExtracter;
import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.uber.h3core.H3Core.newInstance;

public class GeoJSONToHexCellConverter {
    // Create an instance of H3Core
    static H3Core h3;

    static {
        try {
            h3 = newInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GeoJSONToHexCellConverter() throws IOException {
    }

    public static void main(String[] args) throws IOException, ParseException {

        // Path to the GeoJSON file
        String geoJSONFilePath = "D:\\cluster\\india_district.geojson";

        // Read the GeoJSON file
        String geoJSON = new String(Files.readAllBytes(Paths.get(geoJSONFilePath)));

        // Parse the GeoJSON using Gson
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(geoJSON, JsonObject.class);

        GeoJsonValidation(gson, jsonObject);
    }

    private static void GeoJsonValidation(Gson gson, JsonObject jsonObject) throws ParseException, IOException {

        // Create a new FeatureCollection object
        JsonObject featureCollection = new JsonObject();
        featureCollection.addProperty("type", "FeatureCollection");
        JsonArray features = new JsonArray();
        featureCollection.add("features", features);

        // Check if the parsed JSON object is a valid FeatureCollection
        if (jsonObject.has("type") && jsonObject.get("type").getAsString().equals("FeatureCollection")) {
            JsonArray inputFeatures = jsonObject.getAsJsonArray("features");

            // Process each feature
            for (int i = 0; i < inputFeatures.size(); i++) {
                JtsUtil jtsUtil = JtsUtil.getInstance();
                JsonObject inputFeature = inputFeatures.get(i).getAsJsonObject();
                JsonObject outputFeature = new JsonObject();

                GeoJsonGeometry geoJsonGeometry = gson.fromJson(inputFeature.get("geometry"), GeoJsonGeometry.class);
                Geometry geometry = jtsUtil.convertToJtsGeometry(geoJsonGeometry);



                // Convert the geometry to H3 hexagons of resolution 9 based on the geometry
                List<String> geoHexCells = convertGeometryToGeoHexCells(geometry, 9);

                // Write the H3 hexagons to a file
                String name = inputFeature.getAsJsonObject("properties").get("NAME_2").getAsString()+" "+inputFeature.getAsJsonObject("geometry").get("type").getAsString();
                //System.out.println(geoHexCells);

                writeGeoHexCellsToFile(geoHexCells, "D:\\cluster\\hexcel_district_wise", name);
            }
        }
    }

    private static List<String> convertGeometryToGeoHexCells(Geometry geometry, int i) {
        List<String> geoHexCells = new ArrayList<>();

        if(geometry instanceof Polygon){
            List<String> cellsFromGeometry = getCellsFromGeometry(geometry, i);
            geoHexCells.addAll(cellsFromGeometry);
        }else if(geometry instanceof MultiPolygon){
            List<String> cellsFromGeometry = getCellsFromMGeometry(geometry, i);
            geoHexCells.addAll(cellsFromGeometry);
        }
        return geoHexCells;
    }

    public static void writeGeoHexCellsToFile(List<String> geoHexCells, String filePath, String name) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            GeoHexCellsWrapper geoHexCellsWrapper = new GeoHexCellsWrapper(geoHexCells);
            mapper.writeValue(Paths.get(filePath + "\\" + name + ".json").toFile(), geoHexCellsWrapper);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static List<String> getCellsFromGeometry(Geometry geometry, int resolution) {
        List<String> cells = new ArrayList<>();
        List<Polygon> polygons = GeometryExtracter.extract(geometry, Geometry.TYPENAME_POLYGON, new ArrayList<>());
        for (Polygon polygon : polygons) {
            cells.addAll(getCellsFromPolygon(polygon, resolution));
        }
        return cells;
    }

    public static List<String> getCellsFromPolygon(Polygon polygon, int resolution) {
        List<LatLng> exteriorRing = Optional.of(Arrays.asList(polygon.getExteriorRing().getCoordinates()))
                .orElseGet(Collections::emptyList).stream()
                .map(coordinate -> new LatLng(coordinate.getY(), coordinate.getX())).collect(Collectors.toList());

        List<List<LatLng>> holes = new ArrayList<>();
        for (int n = 0; n < polygon.getNumInteriorRing(); n++) {
            holes.add(Arrays.stream(polygon.getInteriorRingN(n).getCoordinates())
                    .map(coordinate -> new LatLng(coordinate.getY(), coordinate.getX())).collect(Collectors.toList()));

        }
        return h3.polygonToCellAddresses(exteriorRing, holes, resolution);
    }


    public static List<String> getCellsFromMGeometry(Geometry geometry, int resolution) {
        List<String> cells = new ArrayList<>();
        List<MultiPolygon> multiPolygons = GeometryExtracter.extract(geometry, Geometry.TYPENAME_MULTIPOLYGON, new ArrayList<>());
        for (MultiPolygon multiPolygon : multiPolygons) {
            cells.addAll(getCellsFromMultiPolygon(multiPolygon, resolution));
        }
        return cells;
    }

    private static Collection<String> getCellsFromMultiPolygon(MultiPolygon multiPolygon, int resolution) {
        List<String> cells = new ArrayList<>();
        for (int n = 0; n < multiPolygon.getNumGeometries(); n++) {
            Polygon polygon = (Polygon) multiPolygon.getGeometryN(n);
            cells.addAll(getCellsFromPolygon(polygon, resolution));
        }
        return cells;
    }
    // Helper class for wrapping the list of GeoHex cells
    public static class GeoHexCellsWrapper {
        private List<String> geoCells;

        public GeoHexCellsWrapper(List<String> geoCells) {
            this.geoCells = geoCells;
        }

        public List<String> getGeoCells() {
            return geoCells;
        }

        public void setGeoCells(List<String> geoCells) {
            this.geoCells = geoCells;
        }
    }
}
