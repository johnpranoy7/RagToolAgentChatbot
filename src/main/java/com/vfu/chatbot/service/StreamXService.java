package com.vfu.chatbot.service;

import com.vfu.chatbot.service.domain.AmenityResponse;
import com.vfu.chatbot.service.domain.LocationInfoResponse;
import com.vfu.chatbot.service.domain.PropertyResponse;
import com.vfu.chatbot.service.domain.ReservationResponse;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class StreamXService {

    private static @NonNull PropertyResponse getDummyPropertyInfo() {
        PropertyResponse property = new PropertyResponse();

        property.setId("28254");
        property.setParentId(null);
        property.setSearchPosition("3");
        property.setLocalPhone("123-321-1234");
        property.setWifiSecurityKey("Network: Resort_Pro  Password: Stream12345678987654321");
        property.setSeoTitle("Streamline Demo Company: Blue Creek Cabin in Area 1");
        property.setSeoDescription("Streamline Demo Company: Blue Creek Cabin in Area 1. Blue creek cabin demo description.");
        property.setSeoKeywords("Blue Creek Cabin, Blue Creek Cabin in Area 1,");
        property.setFloor(null);
        property.setLocationId("16268");
        property.setBuildingId("16549");
        property.setCondoTypeId("18715");
        property.setName("Home");
        property.setStatusId("1");
        property.setUnitCode("Blue Creek Cabin");
        property.setMaxAdults("10");
        property.setMaxOccupants("12");
        property.setMaxPets("0");
        property.setVirtualTourUrl("http://www.virtually-anywhere.com/portfolio/lakeaustinspa/");
        property.setLatitude(null);
        property.setLongitude(null);
        property.setComment("Demo Home Update Info");
        property.setLocationLatitude("33.3502878");
        property.setLocationLongitude("-111.9111680");
        property.setSquareFoots("100000");
        property.setWebName("Blue Creek Cabin");
        property.setStatusName("Active");
        property.setLocationName("Blue Creek Cabin");
        property.setCompanyId("348");
        property.setLodgingTypeId("3");
        property.setLocationAreaId("2309");
        property.setNeighborhoodAreaId("2337");
        property.setResortAreaId("2807");
        property.setHomeTypeId("1011");
        property.setAddress("7519 S. McClintock Dr. Suite 105");
        property.setCity("Tempe");
        property.setZip("85283");
        property.setStateName("AZ");
        property.setStateDescription("Arizona");
        property.setCountryName("US");
        property.setLocationAreaName("Huntington Beach");
        property.setNeighborhoodName("La Jolla");
        property.setLocationResortName("Resort A");
        property.setShortDescription("Used by XML/API sites. Not used for sites utilizing our WP plugin. test0");
        property.setCondoTypeName("3 Bedroom Blue Creek Cabin");
        property.setDefaultUnitId(null);
        property.setCondoTypeGroupId("2755");
        property.setBathroomsNumber("5.5");
        property.setBedroomsNumber("5");
        property.setCondoTypeGroupName("3 Bedroom");
        property.setBuildingShortName("Def");
        property.setBuildingName("Default");
        property.setOwningTypeId("1");
        property.setSeoPageName("blue-creek-cabin");
        property.setViewName("Pool View");
        property.setRatingCount("6");
        property.setRatingAverage("74.17");
        property.setPropertyRatingName(null);
        property.setPropertyRatingPoints(null);
        property.setHomeType("Adobe");
        property.setDescription("unit description");
        property.setGlobalDescription("This description will show up on your units landing page within your website. test1");
        property.setFloorName("Ground");
        property.setCreationDate("10/16/2013 02:33:33");
        property.setPropertyCode("BlueCreekCabin");
        property.setHousekeepingZoneId(null);
        property.setRegion(null);
        property.setCommunity(null);
        property.setOriginalDescription(null);
        property.setVariableGateCode("19458324");
        property.setVariableDrivingDirections("Very Nice");
        property.setVariableGolfCart("yes");
        property.setVariableTrashPickUpDays("Monday");
        property.setVariableNumberOfVehicles("3");
        property.setNightGapLogicEnabled("yes");
        property.setNightGapLogicMinimalNights("1");

        return property;
    }

    private static @NonNull ReservationResponse getDummyReservationInfo() {
        ReservationResponse res = new ReservationResponse();
        res.setId("8646528");
        res.setConfirmationId("8788");
        res.setCrossReferenceCode(null);
        res.setTaxExempt("0");
        res.setClientId("4435997");
        res.setCreationDate("09/18/2017 01:02:47");
        res.setStartDate("10/05/2018");
        res.setEndDate("10/08/2018");
        res.setOccupants("2");
        res.setOccupantsSmall("0");
        res.setPets("0");
        res.setEmail("test@test.com");
        res.setEmail1(null);
        res.setEmail2(null);
        res.setTitle(null);
        res.setFirstName("Darth");
        res.setMiddleName(null);
        res.setLastName("Vader");
        res.setAddress(null);
        res.setAddress2(null);
        res.setCity("Tempe");
        res.setZip(null);
        res.setCountryId("227");
        res.setStateId(null);
        res.setPhone(null);
        res.setFax(null);
        res.setMobilePhone(null);
        res.setWorkPhone(null);
        res.setClientComments("client comments");
        res.setDaysNumber("3");
        res.setMaketypeName("I");
        res.setMaketypeDescription("Internet Reservation");
        res.setTypeName("STA");
        res.setStatusCode("***IGNORE-UNSUPPORTED***");
        res.setLocationId("16268");
        res.setCondoTypeId("18715");
        res.setCouponId(null);
        res.setUnitId("28254");
        res.setLongtermEnabled("1");
        res.setUnitName("Home");
        res.setUnitCode("Blue Creek Cabin");
        res.setLocationName("Blue Creek Cabin");
        res.setLodgingTypeId("3");
        res.setCondoTypeName("3 Bedroom Blue Creek Cabin");
        res.setCountryName("US");
        res.setStateName(null);
        res.setPriceNightly("4200.00");
        res.setPriceTotal("7356.33");
        res.setPricePaidsum("0.00");
        res.setPriceCommon("7356.33");
        res.setPriceBalance("7356.33");
        res.setCouponCode(null);
        res.setCompanyId("348");
        res.setStatusId("5");
        res.setHearAboutName("System default");
        res.setLastUpdated("09/18/2017 01:40:16.323686 EDT");
        res.setCommissionedAgentName(null);
        res.setTravelagentName(null);
        return res;
    }

    private static @NonNull List<AmenityResponse> getDummyAmenityInfo() {
        List<AmenityResponse> amenities = new ArrayList<>();

        AmenityResponse amenity1 = new AmenityResponse();
        amenity1.setId("170106");
        amenity1.setGroupName("Amenities");
        amenity1.setName("Internet");
        amenities.add(amenity1);

        AmenityResponse amenity2 = new AmenityResponse();
        amenity2.setId("170102");
        amenity2.setGroupName("Amenities");
        amenity2.setName("Fireplace");
        amenities.add(amenity2);

        AmenityResponse amenity3 = new AmenityResponse();
        amenity3.setId("170101");
        amenity3.setGroupName("Amenities");
        amenity3.setName("Wood Stove");
        amenities.add(amenity3);

        AmenityResponse amenity4 = new AmenityResponse();
        amenity4.setId("170100");
        amenity4.setGroupName("Amenities");
        amenity4.setName("Air Conditioning");
        amenities.add(amenity4);
        return amenities;
    }

    private static @NonNull LocationInfoResponse getDummyLocationInfo() {
        LocationInfoResponse location = new LocationInfoResponse();

        location.setId("16268");
        location.setName("Blue Creek Cabin");
        location.setLocationAreaName("Huntington Beach");
        location.setNeighborhoodName("La Jolla");
        location.setLocationResortName("Resort A");
        location.setCity("Tempe");
        location.setCountryName("US");
        location.setStateName("AZ");
        location.setZip("85283");
        location.setLatitude("33.3502878");
        location.setLongitude("-111.9111680");
        return location;
    }

    public ReservationResponse getReservationInfo(int confirmationId) {
        //TODO: REST Call to StreamX
        return getDummyReservationInfo();
    }

    public PropertyResponse getPropertyInfo(int propertyId) {
        return getDummyPropertyInfo();
    }

    public List<AmenityResponse> getAmenitiesInfo() {
        return getDummyAmenityInfo();
    }

    public LocationInfoResponse getLocationInfo(int locationId) {
        return getDummyLocationInfo();
    }

}
