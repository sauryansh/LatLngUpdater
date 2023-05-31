package com.example.geohexcellgenerator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonBoundary {
	@JsonProperty("type")
    private String type;
	@JsonProperty("geometry")
    private GeoJsonGeometry geometry;
    @JsonProperty("properties")
    private Map<String, Object> properties;
    @JsonProperty("bbox")
    private GeoJsonGeometry bbox;
}
