package com.longing.longing.api.location.controller;

import com.longing.longing.api.location.controller.port.LocationService;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.location.domain.Location;
import com.longing.longing.api.location.domain.LocationCreate;
import com.longing.longing.api.location.domain.LocationUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@Slf4j
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
            @AuthenticationPrincipal User user
            ) {
        Location location = locationService.updateLocation(user, locationId, locationUpdate);
        return ApiResponse.ok(location);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLocation(
            @PathVariable("id") Long locationId
    ) {
        locationService.deleteLocation(locationId);
        return ApiResponse.ok(null);
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ApiResponse<Location> createLocation(
            @RequestPart @Valid LocationCreate locationCreate,
            @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages,
            @AuthenticationPrincipal User user
            ) {
        Location location = locationService.createLocation(user, locationCreate, thumbnailImage, detailImages);
        return ApiResponse.created(location);
    }
}
