package com.longing.longing.category.controller.port;

import com.longing.longing.category.domain.Category;
import com.longing.longing.category.domain.CategoryCreate;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import org.springframework.data.domain.Page;

public interface CategoryService {
    Category createCategory(CustomUserDetails userDetails, CategoryCreate categoryCreate);

    Page<Category> getCategoryList(String keyword, int page, int size, String sortBy, String sortDirection);

    void deleteCategory(Long categoryId);
}
