package com.longing.longing.api.location.service;

import com.longing.longing.api.category.domain.Category;
import com.longing.longing.api.category.service.port.CategoryRepository;
import com.longing.longing.api.location.domain.Location;
import com.longing.longing.api.location.domain.LocationCreate;
import com.longing.longing.api.location.domain.LocationUpdate;
import com.longing.longing.api.location.service.port.LocationRepository;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.location.controller.port.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Location createLocation(CustomUserDetails userDetails, LocationCreate locationCreate) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        Category category = categoryRepository.findById(locationCreate.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categories", locationCreate.getCategoryId()));

        Location location = Location.from(user, category, locationCreate);

        return locationRepository.save(location);
    }

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

    @Override
    public Location updateLocation(CustomUserDetails userDetails, Long locationId, LocationUpdate locationUpdate) {
        // location data 있는지 확인
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Locations", locationId));

        // user data 있는지 확인
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));
        if (!location.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("you can not modify this location");
        }


        Category category = categoryRepository.findById(locationUpdate.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categories", locationUpdate.getCategoryId()));

        location.update(category, locationUpdate);
        locationRepository.save(location);

        return location;
    }

    @Override
    public void deleteLocation(Long locationId) {
        locationRepository.deleteById(locationId);
    }


}
