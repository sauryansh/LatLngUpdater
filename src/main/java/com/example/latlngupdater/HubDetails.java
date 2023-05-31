package com.example.latlngupdater;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HubDetails {
    private String hubid;
    private String hubname;
    private String hubcode;
    private String areacode;
    private String center_type;
    private String hubzonename;
    private String latitude;
    private String longitude;
    private String city;
    private String state;
    private String country;
    private Integer pincode;
    private String status;
    private Boolean isactive;
}
