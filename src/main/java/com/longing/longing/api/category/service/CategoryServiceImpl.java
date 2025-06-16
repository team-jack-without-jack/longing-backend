package com.longing.longing.api.category.service;

import com.longing.longing.api.category.controller.port.CategoryService;
import com.longing.longing.api.category.domain.CategoryCreate;
import com.longing.longing.api.category.service.port.CategoryRepository;
import com.longing.longing.api.category.domain.Category;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.api.user.Provider;
import com.longing.longing.api.user.domain.User;
import com.longing.longing.api.user.service.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public Category createCategory(User user, CategoryCreate categoryCreate) {
        Category category = Category.from(categoryCreate);
        return categoryRepository.save(category);
    }

    @Override
    public Page<Category> getCategoryList(String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return categoryRepository.findAll(pageable);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }


}
