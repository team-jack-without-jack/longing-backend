package com.longing.longing.api.location.service;

import com.longing.longing.api.category.domain.Category;
import com.longing.longing.api.category.service.port.CategoryRepository;
import com.longing.longing.api.location.controller.port.LocationService;
import com.longing.longing.common.domain.LocationImage;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.common.service.S3ImageService;
import com.longing.longing.common.service.port.LocationImageRepository;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.location.domain.Location;
import com.longing.longing.api.location.domain.LocationCreate;
import com.longing.longing.api.location.domain.LocationUpdate;
import com.longing.longing.api.location.service.port.LocationRepository;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
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
    private final Integer thumbnailIndex = 0;

    private String createLocationImageName(MultipartFile image, Boolean isThumbnail, int imageIndex) {
        String fileOriginalFilenameName = image.getOriginalFilename();
        String ext = fileOriginalFilenameName.substring(fileOriginalFilenameName.lastIndexOf("."));

        String locationImageName;
        if (isThumbnail) {
            locationImageName = "thumbnail" + ext;
        } else {
            locationImageName = "detail_" + imageIndex + ext;
        }
        return locationImageName;
    }

    private void uploadAndSaveImage(MultipartFile image, Location location, User user, Boolean isThumbnail, int imageIndex) {
        String s3Dir = "location_images/location_" + location.getId() + "/";
        String locationImageName = createLocationImageName(image, isThumbnail, imageIndex);
        String imageUrl = s3ImageService.uploadLocationImage(image, s3Dir, locationImageName);
        LocationImage locationImage = LocationImage.from(imageUrl, location, user, isThumbnail);
        locationImageRepository.save(locationImage);
    }

    @Override
    @Transactional
    public Location createLocation(
            CustomUserDetails userDetails,
            LocationCreate locationCreate,
            MultipartFile thumbnailImage,
            List<MultipartFile> detailImages) {
        String email = userDetails.getEmail();
        Provider provider = userDetails.getProvider();
        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResourceNotFoundException("Users", email));

        Category category = categoryRepository.findById(locationCreate.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categories", locationCreate.getCategoryId()));

        Location location = Location.from(user, category, locationCreate);

        location = locationRepository.save(location);

        // 상세 이미지 저장
        if (detailImages != null && !detailImages.isEmpty()) {
            for (MultipartFile image : detailImages) {
                int imageIndex = detailImages.indexOf(image);
                uploadAndSaveImage(image, location, user, false, imageIndex);
            }
        }

        // 썸네일 이미지 저장
        if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
            uploadAndSaveImage(thumbnailImage, location, user, true, thumbnailIndex);
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
