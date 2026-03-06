package com.vfu.chatbot.service.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private String id;
    @JsonProperty("confirmation_id")
    private String confirmationId;

    @JsonProperty("cross_reference_code")
    private String crossReferenceCode;

    @JsonProperty("tax_exempt")
    private String taxExempt;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("creation_date")
    private String creationDate;

    @JsonProperty("start_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private String startDate;

    @JsonProperty("end_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private String endDate;

    @JsonProperty("occupants")
    private String occupants;

    @JsonProperty("occupants_small")
    private String occupantsSmall;

    @JsonProperty("pets")
    private String pets;

    @JsonProperty("email")
    private String email;

    @JsonProperty("email1")
    private String email1;

    @JsonProperty("email2")
    private String email2;

    @JsonProperty("title")
    private String title;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("address")
    private String address;

    @JsonProperty("address2")
    private String address2;

    @JsonProperty("city")
    private String city;

    @JsonProperty("zip")
    private String zip;

    @JsonProperty("country_id")
    private String countryId;

    @JsonProperty("state_id")
    private String stateId;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("fax")
    private String fax;

    @JsonProperty("mobile_phone")
    private String mobilePhone;

    @JsonProperty("work_phone")
    private String workPhone;

    @JsonProperty("client_comments")
    private String clientComments;

    @JsonProperty("days_number")
    private String daysNumber;

    @JsonProperty("maketype_name")
    private String maketypeName;

    @JsonProperty("maketype_description")
    private String maketypeDescription;

    @JsonProperty("type_name")
    private String typeName;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("location_id")
    private String locationId;

    @JsonProperty("condo_type_id")
    private String condoTypeId;

    @JsonProperty("coupon_id")
    private String couponId;

    @JsonProperty("unit_id")
    private String unitId;

    @JsonProperty("longterm_enabled")
    private String longtermEnabled;

    @JsonProperty("unit_name")
    private String unitName;

    @JsonProperty("unit_code")
    private String unitCode;

    @JsonProperty("location_name")
    private String locationName;

    @JsonProperty("lodging_type_id")
    private String lodgingTypeId;

    @JsonProperty("condo_type_name")
    private String condoTypeName;

    @JsonProperty("country_name")
    private String countryName;

    @JsonProperty("state_name")
    private String stateName;

    @JsonProperty("price_nightly")
    private String priceNightly;

    @JsonProperty("price_total")
    private String priceTotal;

    @JsonProperty("price_paidsum")
    private String pricePaidsum;

    @JsonProperty("price_common")
    private String priceCommon;

    @JsonProperty("price_balance")
    private String priceBalance;

    @JsonProperty("coupon_code")
    private String couponCode;

    @JsonProperty("company_id")
    private String companyId;

    @JsonProperty("status_id")
    private String statusId;

    @JsonProperty("hear_about_name")
    private String hearAboutName;

    @JsonProperty("last_updated")
    private String lastUpdated;

    @JsonProperty("commissioned_agent_name")
    private String commissionedAgentName;

    @JsonProperty("travelagent_name")
    private String travelagentName;

}
