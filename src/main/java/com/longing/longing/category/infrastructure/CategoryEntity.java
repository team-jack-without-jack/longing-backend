package com.longing.longing.category.infrastructure;

import com.longing.longing.location.infrastructure.LocationEntity;
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
}
