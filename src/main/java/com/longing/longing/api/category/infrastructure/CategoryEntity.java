package com.longing.longing.api.category.infrastructure;

import com.longing.longing.api.category.domain.Category;
import com.longing.longing.api.location.infrastructure.LocationEntity;
import com.longing.longing.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.List;

@Getter
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE categories SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
@Entity
public class CategoryEntity extends BaseTimeEntity {

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
                .id(id)
                .name(name)
                .description(description)
                .build();
    }
}
