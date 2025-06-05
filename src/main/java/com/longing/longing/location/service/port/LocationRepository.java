package com.longing.longing.location.service.port;

import com.longing.longing.location.domain.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LocationRepository {
    Optional<Location> findById(Long id);

    Location save(Location location);

    Optional<Location> findByIdWithImages(Long id);

    Page<Location> findAll(Pageable pageable);

    void deleteById(Long locationId);

    /**
     * 키워드를 포함하여 Location을 페이징 조회하면서
     * 연관된 LocationImage를 한 번에 fetch join으로 로딩한다.
     *
     * @param keyword  검색어 (Location의 특정 필드(예: name)에 포함 여부로 필터링)
     * @param pageable 페이징/정렬 정보
     * @return 검색된 Location + LocationImage들이 미리 로딩된 Page 객체
     */
    Page<Location> findAllWithSearch(String keyword, Pageable pageable);
}
