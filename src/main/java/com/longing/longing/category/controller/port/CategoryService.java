package com.longing.longing.category.controller.port;

import com.longing.longing.category.domain.Category;
import com.longing.longing.category.domain.CategoryCreate;
import org.springframework.data.domain.Page;

public interface CategoryService {
    Category createCategory(String oauthId, CategoryCreate categoryCreate);

    Page<Category> getCategoryList(String keyword, int page, int size, String sortBy, String sortDirection);
}
