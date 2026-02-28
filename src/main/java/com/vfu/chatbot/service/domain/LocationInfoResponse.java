package com.vfu.chatbot.service.domain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationInfoResponse {
    private String id;
    private String name;
    private String locationAreaName;
    private String neighborhoodName;
    private String locationResortName;
    private String city;
    private String countryName;
    private String stateName;
    private String zip;
    private String latitude;
    private String longitude;
}

