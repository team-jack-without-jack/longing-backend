package com.longing.longing.location.infrastructure;

import com.longing.longing.location.domain.Location;
import com.longing.longing.location.service.port.LocationRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
    public Optional<Location> findByIdWithImages(Long id) {
        return locationJpaRepository.findByIdWithImages(id).map(LocationEntity::toModel);
    }

//    @Override
//    public Page<Location> findAll(Pageable pageable) {
//        List<Location> locations = locationJpaRepository.findAll(pageable)
//                .stream()
//                .map(LocationEntity::toModel)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(locations, pageable, locations.size());
//    }

    @Override
    public void deleteById(Long locationId) {
        locationJpaRepository.deleteById(locationId);
    }

//    @Override
//    public Page<Location> findAllWithSearch(String keyword, Pageable pageable) {
//        List<Location> locationEntities = locationJpaRepository.findAllwithSearch(keyword, pageable)
//                .stream()
//                .map(LocationEntity::toModel)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(locationEntities, pageable, locationEntities.size());
//    }


    @Override
    public Page<Location> findAll(Pageable pageable) {
        Page<LocationEntity> locationList = locationJpaRepository.findAll(pageable);

        // 엔티티 목록 → 도메인 객체 목록으로 변환
        List<Location> content = locationList.getContent()
                .stream()
                .map(LocationEntity::toModel)
                .collect(Collectors.toList());

        // Page<Location> 객체로 다시 만들어서 반환
        return new PageImpl<>(
                content,
                pageable,
                locationList.getTotalElements()
        );
    }

    @Override
    public Page<Location> findAllWithSearch(String keyword, Pageable pageable) {
        Page<LocationEntity> locationList = locationJpaRepository.findAllWithSearch(keyword, pageable);

        // 엔티티 목록 → 도메인 객체 목록으로 변환
        List<Location> content = locationList.getContent()
                .stream()
                .map(LocationEntity::toModel)
                .collect(Collectors.toList());

        // Page<Location> 객체로 다시 만들어서 반환
        return new PageImpl<>(
                content,
                pageable,
                locationList.getTotalElements()
        );
    }

}
