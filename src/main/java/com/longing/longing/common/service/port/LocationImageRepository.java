package com.longing.longing.common.service.port;

import com.longing.longing.common.domain.LocationImage;
import com.longing.longing.location.infrastructure.LocationEntity;

public interface LocationImageRepository {
    LocationImage save(LocationImage locationImage);
    void deleteAllByLocation(LocationEntity locationEntity);
}
