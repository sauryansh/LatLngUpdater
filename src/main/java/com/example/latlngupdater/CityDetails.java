package com.example.latlngupdater;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(collection = "geo_intelligence_city_details")
public class CityDetails {
    @Id
    private String id;
    private Integer code;
    private HubDetails hubdetails;

}
