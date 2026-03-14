package com.vfu.chatbot.service.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoapifyProperties {
    @JsonProperty("name")
    private String name;

//    @JsonProperty("state")
//    private String state;
//
//    @JsonProperty("county")
//    private String county;
//
//    @JsonProperty("city")
//    private String city;
//
//    @JsonProperty("postcode")
//    private String postcode;
//
//    @JsonProperty("district")
//    private String district;
//
//    @JsonProperty("street")
//    private String street;

    @JsonProperty("formatted")
    private String formattedAddress;

    @JsonProperty("address_line1")
    private String addressLine1;

    @JsonProperty("address_line2")
    private String addressLine2;

//    @JsonProperty("categories")
//    private List<String> categories;
//
//    @JsonProperty("details")
//    private List<String> details;

    @JsonProperty("description")
    private String description;

//    @JsonProperty("distance")
//    private Double distance;

//    @JsonProperty("place_id")
//    private String placeId;

    @JsonProperty("artwork")
    private Map<String, String> artwork;

//    @JsonProperty("datasource")
//    private GeoapifyDatasource datasource;


}
