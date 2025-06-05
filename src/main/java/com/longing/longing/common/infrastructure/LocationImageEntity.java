package com.longing.longing.common.infrastructure;

import com.longing.longing.common.BaseTimeEntity;
import com.longing.longing.common.domain.LocationImage;
import com.longing.longing.common.domain.PostImage;
import com.longing.longing.location.infrastructure.LocationEntity;
import com.longing.longing.post.infrastructure.PostEntity;
import com.longing.longing.user.infrastructure.UserEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "location_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE location_images SET deleted = true, deleted_date = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class LocationImageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Boolean isThumbnail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Builder
    public LocationImageEntity(
            Long id,
            String address,
            LocationEntity location,
            UserEntity user,
            Boolean isThumbnail
    ) {
        this.id = id;
        this.address = address;
        this.isThumbnail = isThumbnail;
        this.location = location;
        this.user = user;
    }

    public static LocationImageEntity fromModel(LocationImage locationImage) {
        return LocationImageEntity.builder()
                .id(locationImage.getId())
                .address(locationImage.getAddress())
                .location(LocationEntity.fromModel(locationImage.getLocation()))
                .user(UserEntity.fromModel(locationImage.getUser()))
                .build();
    }

    public LocationImage toModel() {
        return LocationImage.builder()
                .id(id)
                .address(address)
                .isThumbnail(isThumbnail)
                .build();
    }
}
