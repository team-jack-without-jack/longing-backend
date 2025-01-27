package com.longing.longing.category.infrastructure;

import com.longing.longing.category.domain.Category;
import com.longing.longing.category.domain.CategoryCreate;
import com.longing.longing.category.service.port.CategoryRepository;
import com.longing.longing.common.domain.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id).map(CategoryEntity::toModel);
    };

    @Override
    public Category save(Category category) {
        return categoryJpaRepository.save(CategoryEntity.fromModel(category)).toModel();
    }

    @Override
    public Page<Category> findAll(Pageable pageable) {
        List<Category> categories = categoryJpaRepository.findAll(pageable).stream()
                .map(CategoryEntity::toModel)
                .collect(Collectors.toList());

        return new PageImpl<>(categories, pageable, categories.size());
    }

    @Override
    public void deleteById(Long id) {
        categoryJpaRepository.deleteById(id);
    }

//    @Override
//    public Page<Category> findAllwithSearch(String keyword, Pageable pageable) {
//        List<Category> postEntities = categoryJpaRepository.findAllwithSearch(keyword, pageable)
//                .stream()
//                .map(CategoryEntity::toModel)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(postEntities, pageable, postEntities.size());
//    }
}
