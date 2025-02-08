package com.longing.longing.location.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LocationJpaRepository extends JpaRepository<LocationEntity, Long> {
    Optional<LocationEntity> findById(Long id);

    @EntityGraph(attributePaths = {"locationEntities"})
    @Query("SELECT l FROM LocationEntity l " +
            "WHERE l.name LIKE %:keyword%")
    Page<LocationEntity> findAllWithLikeCountAndSearch(@Param("keyword") String keyword, Pageable pageable);
}
