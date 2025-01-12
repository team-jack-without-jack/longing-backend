package com.longing.longing.location.controller.port;

import com.longing.longing.location.domain.Location;
import com.longing.longing.location.domain.LocationCreate;
import com.longing.longing.location.domain.LocationUpdate;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LocationService {
    Location createLocation(String oauthId, LocationCreate locationCreate);
    Page<Location> getLocationList(String keyword, int page, int size, String sortBy, String sortDirection);
    Location getLocation(Long id);

    Location updateLocation(String oauthId, Long locationId, LocationUpdate locationUpdate);

    void deleteLocation(Long locationId);
}
