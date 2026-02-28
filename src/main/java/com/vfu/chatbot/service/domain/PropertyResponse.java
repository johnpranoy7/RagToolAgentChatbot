package com.vfu.chatbot.service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyResponse {
    private String id;
    private String parentId;
    private String searchPosition;
    private String localPhone;
    private String wifiSecurityKey;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private String floor;

    private String locationId;
    private String buildingId;
    private String condoTypeId;
    private String name;
    private String statusId;

    private String unitCode;
    private String maxAdults;
    private String maxOccupants;
    private String maxPets;
    private String virtualTourUrl;

    private String latitude;
    private String longitude;
    private String comment;

    private String locationLatitude;
    private String locationLongitude;
    private String squareFoots;

    private String webName;
    private String statusName;
    private String locationName;
    private String companyId;
    private String lodgingTypeId;
    private String locationAreaId;
    private String neighborhoodAreaId;
    private String resortAreaId;
    private String homeTypeId;
    private String address;
    private String city;
    private String zip;
    private String stateName;
    private String stateDescription;
    private String countryName;
    private String locationAreaName;
    private String neighborhoodName;
    private String locationResortName;
    private String shortDescription;

    private String condoTypeName;
    private String defaultUnitId;
    private String condoTypeGroupId;
    private String bathroomsNumber;
    private String bedroomsNumber;
    private String condoTypeGroupName;
    private String buildingShortName;
    private String buildingName;
    private String owningTypeId;
    private String seoPageName;
    private String viewName;
    private String ratingCount;
    private String ratingAverage;
    private String propertyRatingName;
    private String propertyRatingPoints;
    private String homeType;
    private String description;
    private String globalDescription;
    private String floorName;
    private String creationDate;
    private String propertyCode;
    private String housekeepingZoneId;
    private String region;
    private String community;

    private String originalDescription;
    private String variableGateCode;
    private String variableDrivingDirections;
    private String variableGolfCart;
    private String variableTrashPickUpDays;
    private String variableNumberOfVehicles;
    private String nightGapLogicEnabled;
    private String nightGapLogicMinimalNights;
}
