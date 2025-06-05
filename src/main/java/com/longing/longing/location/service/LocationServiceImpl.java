package com.longing.longing.location.service;

import com.longing.longing.category.domain.Category;
import com.longing.longing.category.infrastructure.CategoryEntity;
import com.longing.longing.category.infrastructure.CategoryJpaRepository;
import com.longing.longing.category.service.port.CategoryRepository;
import com.longing.longing.common.domain.LocationImage;
import com.longing.longing.common.domain.PostImage;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.common.service.port.LocationImageRepository;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.location.controller.port.LocationService;
import com.longing.longing.location.domain.Location;
import com.longing.longing.location.domain.LocationCreate;
import com.longing.longing.location.domain.LocationUpdate;
import com.longing.longing.location.infrastructure.LocationEntity;
import com.longing.longing.location.infrastructure.LocationJpaRepository;
import com.longing.longing.location.service.port.LocationRepository;
import com.longing.longing.post.domain.Post;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.Provider;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final S3ImageService s3ImageService;
    private final CategoryRepository categoryRepository;
    private final LocationImageRepository locationImageRepository;

    private void uploadAndSaveImage(MultipartFile image, Location location, User user) {
        String s3Dir = "location_images/location_" + location.getId() + "/";
        String imageUrl = s3ImageService.upload(image, s3Dir);
        LocationImage locationImage = LocationImage.from(imageUrl, location, user);
        locationImageRepository.save(locationImage);
    }

    @Override
    public Location createLocation(CustomUserDetails userDetails, LocationCreate locationCreate, List<MultipartFile> images) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        Category category = categoryRepository.findById(locationCreate.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categories", locationCreate.getCategoryId()));

        Location location = Location.from(user, category, locationCreate);

        location = locationRepository.save(location);
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                uploadAndSaveImage(image, location, user);
            }
        }

        return location;
    }

    @Override
    public Page<Location> getLocationList(String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return locationRepository.findAll(pageable);
        }
        return locationRepository.findAllWithSearch(keyword, pageable);
    }

    @Override
    public Location getLocation(Long id) {
        return locationRepository.findByIdWithImages(id)
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
