package com.vfu.chatbot.service.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private String id;
    private String confirmationId;

    private String crossReferenceCode;
    private String taxExempt;

    private String clientId;
    private String creationDate;
    private String startDate;
    private String endDate;
    private String occupants;
    private String occupantsSmall;
    private String pets;
    private String email;
    private String email1;
    private String email2;
    private String title;
    private String firstName;
    private String middleName;
    private String lastName;
    private String address;
    private String address2;
    private String city;
    private String zip;
    private String countryId;
    private String stateId;
    private String phone;
    private String fax;
    private String mobilePhone;
    private String workPhone;
    private String clientComments;
    private String daysNumber;
    private String maketypeName;
    private String maketypeDescription;
    private String typeName;
    private String statusCode;
    private String locationId;
    private String condoTypeId;
    private String couponId;
    private String unitId;
    private String longtermEnabled;
    private String unitName;
    private String unitCode;
    private String locationName;
    private String lodgingTypeId;
    private String condoTypeName;
    private String countryName;
    private String stateName;
    private String priceNightly;
    private String priceTotal;
    private String pricePaidsum;
    private String priceCommon;
    private String priceBalance;
    private String couponCode;
    private String companyId;
    private String statusId;
    private String hearAboutName;
    private String lastUpdated;
    private String commissionedAgentName;
    private String travelagentName;
}
