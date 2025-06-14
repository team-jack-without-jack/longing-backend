package com.longing.longing.common.infrastructure;

import com.longing.longing.common.domain.LocationImage;
import com.longing.longing.common.service.port.LocationImageRepository;
import com.longing.longing.location.infrastructure.LocationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@RequiredArgsConstructor
@Repository
public class LocationImageRepositoryImpl implements LocationImageRepository {

    private final LocationImageJpaRepoository locationImageJpaRepoository;

    @Override
    public LocationImage save(LocationImage locationImage) {
        return locationImageJpaRepoository.save(LocationImageEntity.fromModel(locationImage)).toModel();
    }

    @Override
    public void deleteAllByLocation(LocationEntity locationEntity) {

    }
}
