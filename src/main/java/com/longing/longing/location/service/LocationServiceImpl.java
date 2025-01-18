package com.longing.longing.location.service;

import com.longing.longing.category.domain.Category;
import com.longing.longing.category.infrastructure.CategoryEntity;
import com.longing.longing.category.infrastructure.CategoryJpaRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.location.controller.port.LocationService;
import com.longing.longing.location.domain.Location;
import com.longing.longing.location.domain.LocationCreate;
import com.longing.longing.location.domain.LocationUpdate;
import com.longing.longing.location.infrastructure.LocationEntity;
import com.longing.longing.location.infrastructure.LocationJpaRepository;
import com.longing.longing.location.service.port.LocationRepository;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationJpaRepository locationJpaRepository;
    private final UserRepository userRepository;
    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Location createLocation(String oauthId, LocationCreate locationCreate) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));
        Category category = categoryJpaRepository.findById(locationCreate.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId)).toModel();
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
    public Location updateLocation(String oauthId, Long locationId, LocationUpdate locationUpdate) {
        LocationEntity locationEntity = locationJpaRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Locations", locationId));
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));
        if (!locationEntity.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("you can not modify this location");
        }

        CategoryEntity categoryEntity = categoryJpaRepository.findById(locationUpdate.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categories", locationUpdate.getCategoryId()));

        LocationEntity updatedLocation = locationEntity.update(categoryEntity, locationUpdate);
        return updatedLocation.toModel();
    }

    @Override
    public void deleteLocation(Long locationId) {
        locationRepository.deleteById(locationId);
    }


}
