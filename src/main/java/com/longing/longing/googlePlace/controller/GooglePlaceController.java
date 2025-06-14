package com.longing.longing.googlePlace.controller;

import com.google.maps.model.PlacesSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.longing.longing.googlePlace.service.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GooglePlaceController {
    private final GooglePlaceService googlePlaceService;

//    public GooglePlaceCont(GooglePlaceService googlePlaceService) {
//        this.googlePlaceService = googlePlaceService;
//    }

//    @GetMapping("google/places")
//    public PlacesSearchResponse searchPlaces(
//            @RequestParam String query,
//            @RequestParam String lat,
//            @RequestParam String lng) throws Exception {
//        return googlePlaceService.searchPlaces(query, lat, lng);
//    }

    @GetMapping("google/places")
    public String searchPlaces(@RequestParam String query) {
        String ret = googlePlaceService.searchPlaces(query);
        return ret;
    }
}
