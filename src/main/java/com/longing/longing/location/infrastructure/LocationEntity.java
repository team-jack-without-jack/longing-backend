package com.longing.longing.location.infrastructure;

import com.longing.longing.category.domain.Category;
import com.longing.longing.category.infrastructure.CategoryEntity;
import com.longing.longing.location.domain.Location;
import com.longing.longing.location.domain.LocationUpdate;
import com.longing.longing.post.domain.PostUpdate;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.function.Consumer;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "locations")
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

    @Builder
    public LocationEntity(Long id, String name, String mapUrl, String phoneNumber, UserEntity user, CategoryEntity category) {
        this.id = id;
        this.name = name;
        this.mapUrl = mapUrl;
        this.phoneNumber = phoneNumber;
        this.user = user;
        this.category = category;
    }

    public static LocationEntity fromModel(Location location) {
        return LocationEntity.builder()
                .id(location.getId())
                .name(location.getName())
                .mapUrl(location.getMapUrl())
                .phoneNumber(location.getPhoneNumber())
                .user(UserEntity.fromModel(location.getUser()))
                .category(CategoryEntity.fromModel(location.getCategory()))
                .build();
    }

    public Location toModel() {
        return Location.builder()
                .id(id)
                .name(name)
                .mapUrl(mapUrl)
                .category(category.toModel())
                .phoneNumber(phoneNumber)
                .user(user.toModel())
                .build();
    }

//    public LocationEntity update(LocationUpdate locationUpdate) {
//        if (locationUpdate.getTitle() != null) {
//            this.title = postUpdate.getTitle();
//        }
//        if (locationUpdate.getContent() != null) {
//            this.content = postUpdate.getContent();
//        }
//        return this;
//    }

    public LocationEntity update(CategoryEntity categoryEntity, LocationUpdate locationUpdate) {
        updateField(locationUpdate.getName(), value -> this.name = value);
        updateField(locationUpdate.getMapUrl(), value -> this.mapUrl = value);
        updateField(locationUpdate.getPhoneNumber(), value -> this.phoneNumber = value);
        updateField(locationUpdate.getCategoryId(), value -> this.category = categoryEntity);
        return this;
    }

    private <T> void updateField(T value, Consumer<T> updater) {
        if (value != null) {
            updater.accept(value);
        }
    }


}
