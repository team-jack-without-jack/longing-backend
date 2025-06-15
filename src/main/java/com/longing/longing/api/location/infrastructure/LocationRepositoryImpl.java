package com.longing.longing.api.location.infrastructure;

import com.longing.longing.api.location.domain.Location;
import com.longing.longing.api.location.service.port.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LocationRepositoryImpl implements LocationRepository {

    private final LocationJpaRepository locationJpaRepository;

    @Override
    public Optional<Location> findById(Long id) {
        return locationJpaRepository.findById(id).map(LocationEntity::toModel);
    }

    @Override
    public Location save(Location location) {
        return locationJpaRepository.save(LocationEntity.fromModel(location)).toModel();
    }

    @Override
    public Page<Location> findAll(Pageable pageable) {
        List<Location> locations = locationJpaRepository.findAll(pageable)
                .stream()
                .map(LocationEntity::toModel)
                .collect(Collectors.toList());

        return new PageImpl<>(locations, pageable, locations.size());
    }

    @Override
    public void deleteById(Long locationId) {
        locationJpaRepository.deleteById(locationId);
    }

    @Override
    public Page<Location> findAllwithLikeCountAndSearch(String keyword, Pageable pageable) {
        List<Location> locationEntities = locationJpaRepository.findAllWithLikeCountAndSearch(keyword, pageable)
                .stream()
                .map(LocationEntity::toModel)
                .collect(Collectors.toList());

        return new PageImpl<>(locationEntities, pageable, locationEntities.size());
    }
}
