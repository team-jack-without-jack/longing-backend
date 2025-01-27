package com.longing.longing.category.service;

import com.longing.longing.category.controller.port.CategoryService;
import com.longing.longing.category.domain.Category;
import com.longing.longing.category.domain.CategoryCreate;
import com.longing.longing.category.service.port.CategoryRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import com.longing.longing.post.domain.Post;
import com.longing.longing.user.domain.User;
import com.longing.longing.user.service.port.UserRepository;
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
    public Category createCategory(String oauthId, CategoryCreate categoryCreate) {
        User user = userRepository.findByProviderId(oauthId)
                .orElseThrow(() -> new ResourceNotFoundException("Users", oauthId));;
        Category category = Category.from(categoryCreate);
        return categoryRepository.save(category);
    }

    @Override
    public Page<Category> getCategoryList(String keyword, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

//        if (keyword == null || keyword.trim().isEmpty()) {
//            return categoryRepository.findAll(pageable);
//        }
//        return categoryRepository.findAllwithLikeCountAndSearch(keyword, pageable);
        return categoryRepository.findAll(pageable);
    }


}
