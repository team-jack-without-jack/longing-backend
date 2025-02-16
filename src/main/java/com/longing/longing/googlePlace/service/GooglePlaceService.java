package com.longing.longing.googlePlace.service;

import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GooglePlaceService {

    private final GeoApiContext context;

    public GooglePlaceService(@Value("${google.api.key}") String apiKey) {
        this.context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

//    public PlacesSearchResponse searchPlaces(String query, String location) throws Exception {
//        return PlacesApi.textSearchQuery(context, query)
//                .location(location)
//                .radius(1000) // 1km 범위 내 검색
//                .await();
//    }

//    public PlacesSearchResponse searchPlaces(String query, String lat, String lng) throws Exception {
//        LatLng location = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
//        return PlacesApi.textSearchQuery(context, query)
//                .location(location)
//                .radius(1000) // 1km 범위 내 검색
//                .await();
//    }


    @Value("${google.api.key}")
    private String apiKey;

    private static final String PLACES_API_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json";

    public String searchPlaces(String query) {
        RestTemplate restTemplate = new RestTemplate();

        // URL 파라미터로 검색어와 API Key 추가
        String url = UriComponentsBuilder.fromHttpUrl(PLACES_API_URL)
                .queryParam("query", query)
                .queryParam("key", apiKey)
                .toUriString();

        // Google Places API에 요청 보내기
        return restTemplate.getForObject(url, String.class);
    }
}
