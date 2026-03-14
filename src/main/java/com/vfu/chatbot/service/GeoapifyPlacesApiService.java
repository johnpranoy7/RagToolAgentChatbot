package com.vfu.chatbot.service;

import com.vfu.chatbot.service.domain.GeoapifyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class GeoapifyPlacesApiService {

    private final RestClient restClient;

    @Value("${geoapify.apiKey}")
    private String GEOAPIFY_APIKEY;

    public GeoapifyPlacesApiService(@Value("${geoapify.baseurl}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public GeoapifyResponse findNearbyPlaces(String categoriesString, double lon, double lat, int radiusMeters) {
        String url = UriComponentsBuilder.fromPath("/places")
                .queryParam("filter", "circle:%f,%f,%d".formatted(lon, lat, radiusMeters))
                .queryParam("limit", 5)
                .queryParam("apiKey", GEOAPIFY_APIKEY)
                .queryParam("categories", categoriesString)
                .toUriString();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(GeoapifyResponse.class);

    }
}

