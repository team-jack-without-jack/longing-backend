package com.longing.longing.location.controller;

import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.location.controller.port.LocationService;
import com.longing.longing.location.domain.Location;
import com.longing.longing.location.domain.LocationCreate;
import com.longing.longing.location.domain.LocationUpdate;
import com.longing.longing.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/location")
public class LocationController {


    private final LocationService locationService;

    @GetMapping()
    public ApiResponse<Page<Location>> getLocationList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Page<Location> locationList = locationService.getLocationList(keyword, page, size, sortBy, sortDirection);
        return ApiResponse.ok(locationList);
    }


    @GetMapping("/{id}")
    public ApiResponse<Location> getLocation(
            @PathVariable("id") Long locationId
    ) {
        Location location = locationService.getLocation(locationId);
        return ApiResponse.ok(location);
    }

    /**
     *
     * @param locationId
     * @param locationUpdate
     * @param authentication
     * @return
     */
    @PatchMapping("/{id}")
    public ApiResponse<Location> updateLocation(
            @PathVariable("id") Long locationId,
            @RequestBody LocationUpdate locationUpdate,
            Authentication authentication
    ) {
        String oauthId = authentication.getName();
        Location location = locationService.updateLocation(oauthId, locationId, locationUpdate);
        return ApiResponse.ok(location);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLocation(
            @PathVariable("id") Long locationId
    ) {
        locationService.deleteLocation(locationId);
        return ApiResponse.ok(null);
    }

    @PostMapping()
    public ApiResponse<Location> createLocation(
            @RequestBody LocationCreate locationCreate,
            Authentication authentication
            ) {
        String oauthId = authentication.getName();
        Location location = locationService.createLocation(oauthId, locationCreate);
        return ApiResponse.created(location);
    }
}
