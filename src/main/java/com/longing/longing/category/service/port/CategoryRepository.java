package com.longing.longing.category.service.port;

import com.longing.longing.category.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryRepository {
    Category save(Category category);

    Page<Category> findAll(Pageable pageable);

    void deleteById(Long id);

//    Page<Category> findAllwithSearch(String keyword, Pageable pageable);
}
