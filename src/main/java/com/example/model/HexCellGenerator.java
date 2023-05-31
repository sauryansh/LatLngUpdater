package com.example.model;

import lombok.Builder;

/**
 * {
 *
 *   "hexId": "89608856067ffff",
 *   "pincode": "411025",
 *   "city": "Pune District",
 *   "state": "Maharashtra",
 *   "deleted": false
 * }
 */

@Builder

public class HexCellGenerator {
    private String hexId;
    private String pincode;
    private String city;
    private String state;
    private boolean deleted;

    public HexCellGenerator(String hexId, String pincode, String city, String state, boolean deleted) {
        this.hexId = hexId;
        this.pincode = pincode;
        this.city = city;
        this.state = state;
        this.deleted = deleted;
    }

    public HexCellGenerator() {
    }

    public String getHexId() {
        return hexId;
    }

    public void setHexId(String hexId) {
        this.hexId = hexId;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
