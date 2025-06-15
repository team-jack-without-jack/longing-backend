package com.longing.longing.api.location.controller.port;

import com.longing.longing.api.location.domain.Location;
import com.longing.longing.api.location.domain.LocationCreate;
import com.longing.longing.api.location.domain.LocationUpdate;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import org.springframework.data.domain.Page;

public interface LocationService {
    Location createLocation(CustomUserDetails userDetails, LocationCreate locationCreate);
    Page<Location> getLocationList(String keyword, int page, int size, String sortBy, String sortDirection);
    Location getLocation(Long id);

    Location updateLocation(CustomUserDetails userDetails, Long locationId, LocationUpdate locationUpdate);

    void deleteLocation(Long locationId);
}
