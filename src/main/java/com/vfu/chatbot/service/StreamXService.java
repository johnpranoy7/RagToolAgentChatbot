package com.vfu.chatbot.service;

import com.newrelic.api.agent.Trace;
import com.vfu.chatbot.exception.AiToolException;
import com.vfu.chatbot.service.domain.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
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

    @Trace
    public ReservationResponse getReservationInfo(String reservationId) {
        try {
            HashMap<String, String> paramsMap = new HashMap<>();
            paramsMap.put("confirmation_id", reservationId);

            //TODO: Remove this Extra String type API Call.
            log.info(createAndCallStreamXEndpointString("GetReservationInfo", paramsMap));
            StreamxData streamxData = createAndCallStreamXEndpoint("GetReservationInfo", paramsMap).getData();
            return streamxData.getReservation();
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

    @Trace
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

    @Trace
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
