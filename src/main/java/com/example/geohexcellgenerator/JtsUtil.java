package com.example.geohexcellgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xb.logger.XBLogger;
import com.xb.logger.XBLoggerFactory;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.util.GeometricShapeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * This service provides operations for GeoJsonBoundary.
 * KNOWN ISSUES (till jts:18.2):
 * 1. For Geometry objects created with precisionScale, geometry.buffer(distance) result in Polygon.EMPTY
 * 2. g1.difference(g2) or g1.union(g2) on Geometry objects created without precisionScale sometimes result in Geometry with self-intersecting edges. This cannot be stored in mongodb collection with geospatial index.
 */

public class JtsUtil {
    private static final JtsUtil INSTANCE = new JtsUtil();

    public final double bufferDistance;
    private final GeoJsonReader geoJsonReaderWithPrecision;
    private final GeoJsonReader geoJsonReader;
    private final GeoJsonWriter geoJsonWriter;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory;
    XBLogger logger = XBLoggerFactory.getXBLogger(JtsUtil.class);
    Properties properties;

    private JtsUtil() {
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties = new Properties();
            properties.load(input);
            bufferDistance = Double.valueOf(properties.get("app.bufferDistance").toString());
            PrecisionModel precisionModel = new PrecisionModel(Double.valueOf(properties.get("app.precisionScale").toString()));
            objectMapper = new ObjectMapper();
            geometryFactory = new GeometryFactory(precisionModel);
            geoJsonReaderWithPrecision = new GeoJsonReader(geometryFactory);
            geoJsonReader = new GeoJsonReader();
            geoJsonWriter = new GeoJsonWriter();
            geoJsonWriter.setEncodeCRS(false);
        }
        catch (IOException e) {
            throw new JtsException(e.getMessage());
        }
    }

    public static JtsUtil getInstance() {
        return INSTANCE;
    }

    /***
     * Creates JtsGeometry from a GeoJsonGeometry.
     *
     * @param geoJsonGeometry GeoJsonGeometry
     * @return Geometry
     */
    public Geometry convertToJtsGeometry(GeoJsonGeometry geoJsonGeometry)
            throws ParseException, JsonProcessingException {
        return geoJsonReader.read(objectMapper.writeValueAsString(geoJsonGeometry));
    }

    public Geometry convertToBufferedGeometry(GeoJsonGeometry geoJsonGeometry, Double distance)
            throws ParseException, JsonProcessingException {
        return geoJsonReader.read(objectMapper.writeValueAsString(geoJsonGeometry)).buffer(distance);
    }

    public GeoJsonGeometry bufferedGeoJsonGeometry(GeoJsonGeometry geoJsonGeometry, Double distance)
            throws ParseException, JsonProcessingException {
        return convertToJsonGeometry(geoJsonReader.read(objectMapper.writeValueAsString(geoJsonGeometry)).buffer(distance));
    }

    public GeoJsonGeometry makePrecise(GeoJsonGeometry geoJsonGeometry) throws JsonProcessingException, ParseException {
        Geometry preciseJtsGeometry = geoJsonReaderWithPrecision.read(objectMapper.writeValueAsString(geoJsonGeometry));
        return convertToJsonGeometry(preciseJtsGeometry);
    }

    /***
     * Creates GeoJsonGeometry from Jts Geometry.
     */
    public GeoJsonGeometry convertToJsonGeometry(Geometry geometry) throws JsonProcessingException {
        return objectMapper.readValue(geoJsonWriter.write(geometry), GeoJsonGeometry.class);
    }

    /***
     * Checks if the GeoJsonGeometry provided is valid and normalizes it.
     */
    public GeoJsonGeometry normaliseGeoJsonGeometry(GeoJsonGeometry geoJsonGeometry)
            throws JtsException {
        try {
            Geometry geometry = convertToJtsGeometry(geoJsonGeometry);
            geometry.normalize();
            return convertToJsonGeometry(geometry);
        }
        catch (ParseException | JsonProcessingException e) {
            logger.error("{}", e);
            throw new JtsException(e.getMessage());
        }
    }

    /***
     * To add two JtsGeometries
     */
    public Geometry addGeometries(Geometry g1, Geometry g2) {
        Geometry result = g1.union(g2);
        result.normalize();
        return result;
    }

    public GeoJsonGeometry addGeometries(GeoJsonGeometry g1, GeoJsonGeometry g2) throws ParseException, JsonProcessingException {
        return convertToJsonGeometry(addGeometries(convertToJtsGeometry(makePrecise(g1)), convertToJtsGeometry(makePrecise(g2))));
    }

    /**
     * Subtracts
     * @param g1
     * @param g2
     * @return g1 - g2
     */
    public Geometry subtractGeometries(Geometry g1, Geometry g2) {
        Geometry result = g1.difference(g2);
        result.normalize();
        return result;
    }

    public GeoJsonGeometry subtractGeometries(GeoJsonGeometry g1, GeoJsonGeometry g2) throws ParseException, JsonProcessingException {
        return convertToJsonGeometry(subtractGeometries(convertToJtsGeometry(makePrecise(g1)), convertToJtsGeometry(makePrecise(g2))));
    }

    public boolean isGeometryContained(GeoJsonGeometry g1, GeoJsonGeometry g2) throws ParseException, JsonProcessingException {
        return (this.convertToBufferedGeometry(g1, bufferDistance)).covers(this.convertToJtsGeometry(g2));
    }

    public boolean isCoordinateInGeometry(GeoJsonGeometry geometry, Double lat, Double lng) {
        Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
        try {
            return point.intersects(convertToJtsGeometry(geometry));
        }
        catch (ParseException | JsonProcessingException e) {
            logger.error("Error in JtsUtil.isCoordinateIntersecting(): {}", e.getMessage());
            return false;
        }
    }

    public GeoJsonGeometry getBoundingBox(GeoJsonBoundary geoJsonBoundary) {
        try {
            if (geoJsonBoundary.getGeometry().getType().equals(Geometry.TYPENAME_POLYGON) || geoJsonBoundary.getGeometry().getType().equals(Geometry.TYPENAME_MULTIPOLYGON)) {
                return convertToJsonGeometry(convertToJtsGeometry(geoJsonBoundary.getGeometry()).getEnvelope());
            }

            Map<String, Object> geoJsonBoundaryProperties = geoJsonBoundary.getProperties();
            String shape = Optional.ofNullable(geoJsonBoundaryProperties.get("shape")).orElse("").toString();

            if (shape.equalsIgnoreCase("circle")) {

                Double radiusMetre = Double.parseDouble(Optional.ofNullable(geoJsonBoundaryProperties.get("radius")).orElse(0d).toString());
                Double longitude = (Double) geoJsonBoundary.getGeometry().getCoordinates().get(0);
                Double latitude = (Double) geoJsonBoundary.getGeometry().getCoordinates().get(1);

                Coordinate centre = new Coordinate(longitude, latitude);

                GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
                shapeFactory.setCentre(centre);

                // Length in meters of 1° of latitude = always 111.32 km
                //adding 1% margin for scale
                shapeFactory.setHeight((radiusMetre * 2 /111320d)*1.01d);

                // Length in meters of 1° of longitude = 40075 km * cos( latitude ) / 360
                //adding 1% margin for scale
                shapeFactory.setWidth((radiusMetre * 2 / (40075000 * Math.cos(Math.toRadians(latitude)) / 360)) * 1.01d);

                return convertToJsonGeometry(shapeFactory.createCircle().getEnvelope());
            }
        }
        catch (Exception ex) {
            logger.error("error creating boundingBox: {}", ex.getLocalizedMessage());
        }
        return null;
    }

    public double haversine(double lat1, double lon1, double lat2, double lon2){
        // distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        // convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        // apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double radiusInMeters = 6371000;
        double c = 2 * Math.asin(Math.sqrt(a));
        return radiusInMeters * c;
    }
}
