package com.longing.longing.location.service;

import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.location.controller.port.LocationService;
import com.longing.longing.location.domain.Location;
import com.longing.longing.location.service.port.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;


    @Override
    public Page<Location> getLocationList(String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return locationRepository.findAll(pageable);
        }
        return locationRepository.findAllwithLikeCountAndSearch(keyword, pageable);

    }

    @Override
    public Location getLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locations", id));
    }
}
