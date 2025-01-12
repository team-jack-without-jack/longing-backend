package com.longing.longing.category.infrastructure;

import com.longing.longing.category.domain.Category;
import com.longing.longing.common.domain.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl {

    private final CategoryJpaRepository categoryJpaRepository;

    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id).map(CategoryEntity::toModel);
    };
}
