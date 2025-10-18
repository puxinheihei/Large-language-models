package com.puxinheihei.backend.service;

import com.puxinheihei.backend.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GaodeWebApiService {
    private final AppProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GaodeWebApiService(AppProperties props, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.props = props;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Query POIs using Gaode Web API place/text endpoint.
     */
    public String searchPOIs(String keywords, String city) {
        String url = UriComponentsBuilder
                .fromHttpUrl(props.getGaode().getBaseUrl() + "/v3/place/text")
                .queryParam("key", props.getGaode().getWebApiKey())
                .queryParam("keywords", keywords)
                .queryParam("city", city)
                .build()
                .toUriString();
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * Get walking directions.
     */
    public String walkingRoute(String origin, String destination) {
        String url = UriComponentsBuilder
                .fromHttpUrl(props.getGaode().getBaseUrl() + "/v3/direction/walking")
                .queryParam("key", props.getGaode().getWebApiKey())
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .build()
                .toUriString();
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * Geocode an address to [lng, lat]. Returns null if not found.
     */
    public double[] geocodeAddress(String address, String city) {
        if (address == null || address.isBlank()) return null;
        String url = UriComponentsBuilder
                .fromHttpUrl(props.getGaode().getBaseUrl() + "/v3/geocode/geo")
                .queryParam("key", props.getGaode().getWebApiKey())
                .queryParam("address", address)
                .queryParam("city", city)
                .build()
                .toUriString();
        String resp = restTemplate.getForObject(url, String.class);
        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isArray() && geocodes.size() > 0) {
                String loc = geocodes.get(0).path("location").asText();
                if (loc != null && !loc.isBlank() && loc.contains(",")) {
                    String[] parts = loc.split(",");
                    return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * Get top POI location for keywords via place/text. Returns [lng, lat] or null.
     */
    public double[] topLocationFromPlaceText(String keywords, String city) {
        String resp = searchPOIs(keywords, city);
        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode pois = root.path("pois");
            if (pois.isArray() && pois.size() > 0) {
                String loc = pois.get(0).path("location").asText();
                if (loc != null && !loc.isBlank() && loc.contains(",")) {
                    String[] parts = loc.split(",");
                    return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
}