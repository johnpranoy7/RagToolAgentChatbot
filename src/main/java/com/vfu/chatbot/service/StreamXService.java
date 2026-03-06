package com.vfu.chatbot.service;

import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.service.domain.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StreamXService {

    private final RestClient restClient;
    @Value("${streamx.endpoint.tokenKey}")
    private String STREAMX_ENDPOINT_TOKEN_KEY;
    @Value("${streamx.endpoint.tokenSecret}")
    private String STREAMX_ENDPOINT_TOKEN_SECRET;

    public StreamXService(@Value("${streamx.endpoint.baseurl}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static @NonNull PropertyResponse getDummyPropertyInfo() {
        PropertyResponse property = new PropertyResponse();

        property.setId("777");
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
        res.setId("123456");
        res.setConfirmationId("8788");
        res.setCrossReferenceCode(null);
        res.setTaxExempt("0");
        res.setClientId("4435997");
//        res.setCreationDate(LocalDate.now());
//        res.setStartDate(LocalDate.now().minusDays(10));
//        res.setEndDate(LocalDate.now().plusDays(5));
        res.setOccupants("2");
        res.setOccupantsSmall("0");
        res.setPets("0");
        res.setEmail("test@test.com");
        res.setEmail1(null);
        res.setEmail2(null);
        res.setTitle(null);
        res.setFirstName("Seth");
        res.setMiddleName("Frekin");
        res.setLastName("Rollins");
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

    public ReservationResponse getReservationInfo(String reservationId) {
        try {
            HashMap<String, String> paramsMap = new HashMap<>();
            paramsMap.put("confirmation_id", reservationId);

            //TODO: Remove this Extra String type API Call.
            log.info(createAndCallStreamXEndpointString("GetReservationInfo", paramsMap));
            StreamXData streamXData = createAndCallStreamXEndpoint("GetReservationInfo", paramsMap).getData();
            return streamXData.getReservation();
        } catch (Exception ex) {
            log.error("Error Fetching ReservationInfo, {}", ex.getMessage());
        }
        return null;
    }

    private String createAndCallStreamXEndpointString(String methodName, Map<String, String> params) throws
            AiToolException {
        StreamxRequest sr = new StreamxRequest();
        sr.methodName = methodName;
        sr.params = params;
        sr.params.put("token_key", STREAMX_ENDPOINT_TOKEN_KEY);
        sr.params.put("token_secret", STREAMX_ENDPOINT_TOKEN_SECRET);
        return restClient.post().body(sr).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        (req, res) -> {
                            try {
                                log.info("4xx error");
                                throw new AiToolException("StreamX Client error: " + res.getStatusText());
                            } catch (AiToolException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .onStatus(HttpStatusCode::is5xxServerError,
                        (req, res) -> {
                            try {
                                log.info("5xx error");
                                throw new AiToolException("StreamX Server error");
                            } catch (AiToolException e) {

                                throw new RuntimeException(e);
                            }
                        })
                .body(String.class);
    }

    @SneakyThrows
    private StreamxResponse createAndCallStreamXEndpoint(String methodName, Map<String, String> params) throws
            AiToolException {

        String requestId = UUID.randomUUID().toString();
        log.info("Adding More Params to StreamX API Call. methodName = {}, params: {}, requestId:{}", methodName, params, requestId);
        try {
            StreamxRequest sr = new StreamxRequest();
            sr.methodName = methodName;
            sr.params = params;

            sr.params.put("token_key", STREAMX_ENDPOINT_TOKEN_KEY);
            sr.params.put("token_secret", STREAMX_ENDPOINT_TOKEN_SECRET);
            return restClient.post().body(sr).retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                try {
                                    // Read full error response body
                                    String errorBody = new BufferedReader(new InputStreamReader(response.getBody()))
                                            .lines().collect(Collectors.joining("\n"));

                                    log.error("STREAMX 4XX ERROR - Status: {}, URI: {}, Body: '{}'",
                                            response.getStatusCode(), request.getURI(), errorBody);

                                    throw new AiToolException(
                                            "StreamX 4xx error (" + response.getStatusCode() + "): " + errorBody
                                    );
                                } catch (AiToolException e) {
                                    log.error("Failed to read 4xx error body", e);
                                    throw new AiToolException("StreamX client error: " + response.getStatusText());
                                }
                            }
                    )
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                try {
                                    String errorBody = new BufferedReader(new InputStreamReader(response.getBody()))
                                            .lines().collect(Collectors.joining("\n"));

                                    log.error("STREAMX 5XX ERROR - Status: {}, URI: {}, Body: '{}'",
                                            response.getStatusCode(), request.getURI(), errorBody);

                                    throw new AiToolException(
                                            "StreamX server error (" + response.getStatusCode() + "): " + errorBody
                                    );
                                } catch (Exception e) {
                                    log.error("Failed to read 5xx error body", e);
                                    throw new AiToolException("StreamX server error: " + response.getStatusText());
                                }
                            })
                    .body(StreamxResponse.class);
        } catch (Exception ex) {
            log.error("STREAMX UNEXPECTED FAILURE - method:'{}', requestId:'{}': {}",
                    methodName, requestId, ex.getMessage(), ex);
            throw new AiToolException("StreamX internal error: " + ex.getMessage());
        }
    }

    public PropertyResponse getPropertyInfo(String propertyId) {
        try {
            HashMap<String, String> paramsMap = new HashMap<>();
            paramsMap.put("unit_id", propertyId);
            return createAndCallStreamXEndpoint("GetPropertyInfo", paramsMap).getData();
        } catch (Exception ex) {
            log.error("Error Fetching PropertyInfo, {}", ex.getMessage());
        }
        return null;
    }

}
