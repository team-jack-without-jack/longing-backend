package com.longing.longing.category.infrastructure;

import com.longing.longing.category.domain.Category;
import com.longing.longing.location.infrastructure.LocationEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;
import java.util.List;

@Getter
@Entity(name = "categories")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @OneToMany(mappedBy = "category")
    private List<LocationEntity> locationEntities;


    @Builder
    public CategoryEntity(Long id, String name, String description, List<LocationEntity> locationEntities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.locationEntities = locationEntities;
    }

    public static CategoryEntity fromModel(Category category) {
        return CategoryEntity.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    public Category toModel() {
        return Category.builder()

                .build();
    }
}
