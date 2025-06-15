package com.longing.longing.api.location.controller;

import com.longing.longing.api.location.domain.Location;
import com.longing.longing.api.location.domain.LocationCreate;
import com.longing.longing.api.location.domain.LocationUpdate;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.location.controller.port.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
     * @param userDetails
     * @return
     */
    @PatchMapping("/{id}")
    public ApiResponse<Location> updateLocation(
            @PathVariable("id") Long locationId,
            @RequestBody LocationUpdate locationUpdate,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        Location location = locationService.updateLocation(userDetails, locationId, locationUpdate);
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
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        Location location = locationService.createLocation(userDetails, locationCreate);
        return ApiResponse.created(location);
    }
}
