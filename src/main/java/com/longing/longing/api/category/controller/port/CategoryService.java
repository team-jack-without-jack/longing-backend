package com.longing.longing.api.category.controller.port;

import com.longing.longing.api.category.domain.Category;
import com.longing.longing.api.category.domain.CategoryCreate;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import org.springframework.data.domain.Page;

public interface CategoryService {
    Category createCategory(User user, CategoryCreate categoryCreate);

    Page<Category> getCategoryList(String keyword, int page, int size, String sortBy, String sortDirection);

    void deleteCategory(Long categoryId);
}
