package com.longing.longing.location.controller.port;

import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.location.domain.Location;
import com.longing.longing.location.domain.LocationCreate;
import com.longing.longing.location.domain.LocationUpdate;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.domain.PostCreate;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LocationService {
    Location createLocation(CustomUserDetails userDetails, LocationCreate locationCreate, List<MultipartFile> images);
    Page<Location> getLocationList(String keyword, int page, int size, String sortBy, String sortDirection);
    Location getLocation(Long id);

    Location updateLocation(CustomUserDetails userDetails, Long locationId, LocationUpdate locationUpdate);

    void deleteLocation(Long locationId);
}
