package com.longing.longing.location.controller;

import com.longing.longing.location.controller.port.LocationService;
import com.longing.longing.location.domain.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/location")
public class LocationController {


    private final LocationService locationService;

    @GetMapping("/")
    public ResponseEntity<Page<Location>> getLocationList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Page<Location> locationList = locationService.getLocationList(keyword, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(locationList);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocation(
            @PathVariable("id") Long locationId
    ) {
        Location location = locationService.getLocation(locationId);
        return ResponseEntity.ok(location);
    }

}
