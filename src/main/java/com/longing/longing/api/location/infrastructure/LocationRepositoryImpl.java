package com.longing.longing.api.location.infrastructure;

import com.longing.longing.api.location.service.port.LocationRepository;
import com.longing.longing.api.location.domain.Location;
import com.longing.longing.common.infrastructure.QLocationImageEntity;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
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

import static com.longing.longing.api.location.infrastructure.QLocationEntity.locationEntity;
import static com.longing.longing.common.infrastructure.QLocationImageEntity.locationImageEntity;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LocationRepositoryImpl implements LocationRepository {

    private final LocationJpaRepository locationJpaRepository;
    private final JPAQueryFactory queryFactory;

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
//        return locationJpaRepository.findByIdWithImages(id).map(LocationEntity::toModel);

        /**
         * to querydsl
         */
        LocationEntity location = queryFactory
                .select(locationEntity)
                .from(locationEntity)
                .leftJoin(locationEntity.locationImageEntities, locationImageEntity)
                .where(locationEntity.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(location)
                .map(LocationEntity::toModel);
    }

    @Override
    public void deleteById(Long locationId) {
        locationJpaRepository.deleteById(locationId);
    }

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
        Predicate searchKeyword = ExpressionUtils.anyOf(
                nameLike(keyword),
                addressLike(keyword)
        );

        List<LocationEntity> locationList = queryFactory
                .select(locationEntity)
                .from(locationEntity)
                .leftJoin(locationEntity.locationImageEntities, locationImageEntity)
                .where(searchKeyword)
                .fetch();

        long totalCount = queryFactory
                .select(locationEntity)
                .from(locationEntity)
                .leftJoin(locationEntity.locationImageEntities, locationImageEntity)
                .where(searchKeyword)
                .stream().count();

        // 엔티티 목록 → 도메인 객체 목록으로 변환
        List<Location> content = locationList
                .stream()
                .map(LocationEntity::toModel)
                .collect(Collectors.toList());

        // Page<Location> 객체로 다시 만들어서 반환
        return new PageImpl<>(
                content,
                pageable,
                totalCount
        );
    }

    private Predicate nameLike(String keyword) {
        return hasText(keyword) ? locationEntity.name.contains(keyword) : null;
    }

    private Predicate addressLike(String keyword) {
        return hasText(keyword) ? locationEntity.name.contains(keyword) : null;
    }

}
