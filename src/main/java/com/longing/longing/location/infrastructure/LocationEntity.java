package com.longing.longing.location.infrastructure;

import com.longing.longing.category.infrastructure.CategoryEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Getter;

import javax.persistence.*;

@Getter
@Entity(name = "locations")
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String mapUrl;

    @Column
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

}
