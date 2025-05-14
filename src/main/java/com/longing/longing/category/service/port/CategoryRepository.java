package com.longing.longing.category.service.port;

import com.longing.longing.category.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);

    Page<Category> findAll(Pageable pageable);

    void deleteById(Long id);

    Optional<Category> findById(Long categoryId);

}
