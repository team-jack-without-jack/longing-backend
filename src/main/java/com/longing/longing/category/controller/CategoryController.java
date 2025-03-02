package com.longing.longing.category.controller;

import com.longing.longing.category.controller.port.CategoryService;
import com.longing.longing.category.domain.Category;
import com.longing.longing.category.domain.CategoryCreate;
import com.longing.longing.common.response.ApiResponse;
import com.longing.longing.config.auth.dto.CustomUserDetails;
import com.longing.longing.user.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Category 생성
     * @param categoryCreate
     * @param authentication
     * @return
     */
    @PostMapping()
    public ApiResponse<Category> createCategory(
            @RequestBody CategoryCreate categoryCreate,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        Category category = categoryService.createCategory(userDetails, categoryCreate);
        return ApiResponse.created(category);
    }

    @GetMapping()
    public ApiResponse<Page<Category>> getCategoryList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Page<Category> categoryList = categoryService.getCategoryList(keyword, page, size, sortBy, sortDirection);
        return ApiResponse.ok(categoryList);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeCategory(
            @PathVariable("id") Long categoryId
    ) {
        categoryService.deleteCategory(categoryId);
        return ApiResponse.ok(null);
    }
}
