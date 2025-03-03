package com.longing.longing.common;

import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class DeletionCheckEntity {

    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean deleted = Boolean.FALSE; // 기본값을 FALSE로 설정

    private LocalDateTime deletedDate;
}