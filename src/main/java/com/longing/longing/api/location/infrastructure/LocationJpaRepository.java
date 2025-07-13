package com.longing.longing.api.location.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LocationJpaRepository extends JpaRepository<LocationEntity, Long> {
//    Optional<LocationEntity> findById(Long id);
    /**
     * (추가) 단일 LocationEntity 조회 시에도 연관된 LocationImageEntity를 함께 로딩
     */
    @Query(
            "SELECT l " +
                    "FROM LocationEntity l " +
                    "LEFT JOIN FETCH l.locationImageEntities img " +
                    "WHERE l.id = :id"
    )
    Optional<LocationEntity> findByIdWithImages(@Param("id") Long id);


    /**
     * 키워드가 없을 때는 JpaRepository.findAll(Pageable)이 호출되므로 별도 메서드가 필요 없습니다.
     * 키워드가 있는 경우, 아래 메서드를 호출하면 JPQL의 FETCH JOIN으로
     * locationImageEntities 컬렉션까지 한 번에 로딩하면서
     * Page<LocationEntity> 형태로 페이징 처리됩니다.
     */
    @Query(
            value =
                    "SELECT DISTINCT l " +
                            "FROM LocationEntity l " +
                            "LEFT JOIN FETCH l.locationImageEntities img " +
                            "WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                            "   OR LOWER(l.address) LIKE LOWER(CONCAT('%', :keyword, '%'))",
            countQuery =
                    "SELECT COUNT(l) " +
                            "FROM LocationEntity l " +
                            "WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                            "   OR LOWER(l.address) LIKE LOWER(CONCAT('%', :keyword, '%'))"
    )
    Page<LocationEntity> findAllWithSearch(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
