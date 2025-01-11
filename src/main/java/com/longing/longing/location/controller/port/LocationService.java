package com.longing.longing.location.controller.port;

import com.longing.longing.location.domain.Location;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LocationService {
    Page<Location> getLocationList(String keyword, int page, int size, String sortBy, String sortDirection);
    Location getLocation(Long id);
}
