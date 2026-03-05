package com.vfu.chatbot.service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyResponse {
    private String id;
    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("search_position")
    private String searchPosition;

    @JsonProperty("local_phone")
    private String localPhone;

    @JsonProperty("wifi_security_key")
    private String wifiSecurityKey;

    @JsonProperty("seo_title")
    private String seoTitle;

    @JsonProperty("seo_description")
    private String seoDescription;

    @JsonProperty("seo_keywords")
    private String seoKeywords;

    @JsonProperty("floor")
    private String floor;

    @JsonProperty("location_id")
    private String locationId;

    @JsonProperty("building_id")
    private String buildingId;

    @JsonProperty("condo_type_id")
    private String condoTypeId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status_id")
    private String statusId;

    @JsonProperty("unit_code")
    private String unitCode;

    @JsonProperty("max_adults")
    private String maxAdults;

    @JsonProperty("max_occupants")
    private String maxOccupants;

    @JsonProperty("max_pets")
    private String maxPets;

    @JsonProperty("virtual_tour_url")
    private String virtualTourUrl;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("location_latitude")
    private String locationLatitude;

    @JsonProperty("location_longitude")
    private String locationLongitude;

    @JsonProperty("square_foots")
    private String squareFoots;

    @JsonProperty("web_name")
    private String webName;

    @JsonProperty("status_name")
    private String statusName;

    @JsonProperty("location_name")
    private String locationName;

    @JsonProperty("company_id")
    private String companyId;

    @JsonProperty("lodging_type_id")
    private String lodgingTypeId;

    @JsonProperty("location_area_id")
    private String locationAreaId;

    @JsonProperty("neighborhood_area_id")
    private String neighborhoodAreaId;

    @JsonProperty("resort_area_id")
    private String resortAreaId;

    @JsonProperty("home_type_id")
    private String homeTypeId;

    @JsonProperty("address")
    private String address;

    @JsonProperty("city")
    private String city;

    @JsonProperty("zip")
    private String zip;

    @JsonProperty("state_name")
    private String stateName;

    @JsonProperty("state_description")
    private String stateDescription;

    @JsonProperty("country_name")
    private String countryName;

    @JsonProperty("location_area_name")
    private String locationAreaName;

    @JsonProperty("neighborhood_name")
    private String neighborhoodName;

    @JsonProperty("location_resort_name")
    private String locationResortName;

    @JsonProperty("short_description")
    private String shortDescription;

    @JsonProperty("condo_type_name")
    private String condoTypeName;

    @JsonProperty("default_unit_id")
    private String defaultUnitId;

    @JsonProperty("condo_type_group_id")
    private String condoTypeGroupId;

    @JsonProperty("bathrooms_number")
    private String bathroomsNumber;

    @JsonProperty("bedrooms_number")
    private String bedroomsNumber;

    @JsonProperty("condo_type_group_name")
    private String condoTypeGroupName;

    @JsonProperty("building_short_name")
    private String buildingShortName;

    @JsonProperty("building_name")
    private String buildingName;

    @JsonProperty("owning_type_id")
    private String owningTypeId;

    @JsonProperty("seo_page_name")
    private String seoPageName;

    @JsonProperty("view_name")
    private String viewName;

    @JsonProperty("rating_count")
    private String ratingCount;

    @JsonProperty("rating_average")
    private String ratingAverage;

    @JsonProperty("property_rating_name")
    private String propertyRatingName;

    @JsonProperty("property_rating_points")
    private String propertyRatingPoints;

    @JsonProperty("home_type")
    private String homeType;

    @JsonProperty("description")
    private String description;

    @JsonProperty("global_description")
    private String globalDescription;

    @JsonProperty("floor_name")
    private String floorName;

    @JsonProperty("creation_date")
    private String creationDate;

    @JsonProperty("property_code")
    private String propertyCode;

    @JsonProperty("housekeeping_zone_id")
    private String housekeepingZoneId;

    @JsonProperty("region")
    private String region;

    @JsonProperty("community")
    private String community;

    @JsonProperty("original_description")
    private String originalDescription;

    @JsonProperty("variable_gate_code")
    private String variableGateCode;

    @JsonProperty("variable_driving_directions")
    private String variableDrivingDirections;

    @JsonProperty("variable_golf_cart")
    private String variableGolfCart;

    @JsonProperty("variable_trash_pick_up_days")
    private String variableTrashPickUpDays;

    @JsonProperty("variable_number_of_vehicles")
    private String variableNumberOfVehicles;

    @JsonProperty("night_gap_logic_enabled")
    private String nightGapLogicEnabled;

    @JsonProperty("night_gap_logic_minimal_nights")
    private String nightGapLogicMinimalNights;

}
