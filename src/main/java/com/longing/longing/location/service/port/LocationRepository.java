package com.longing.longing.location.service.port;

import com.longing.longing.location.domain.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LocationRepository {
    Optional<Location> findById(Long id);

    Location save(Location location);

    Page<Location> findAll(Pageable pageable);

    void deleteById(Long locationId);

    Page<Location> findAllwithLikeCountAndSearch(String keyword, Pageable pageable);
}
