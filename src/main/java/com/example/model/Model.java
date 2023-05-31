package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Model {
    private Object _id;
    private Integer HubID;
    private String HubName;
    private String AreaCode;
    private String CenterType;
    private String hubzonename;
    private Double Latitude;
    private Double Longitude;
    private String cityname;
    private String statename;
    private String country;
    private Integer pincode;
    private String Status;
    private Boolean IsActive;

}
