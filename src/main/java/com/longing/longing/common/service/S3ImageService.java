package com.longing.longing.common.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3ImageService {
    String uploadLocationImage(MultipartFile file, String directoryPath, String locationImageName);

    String uploadProfileImage(MultipartFile file, String directoryPath);

    String uploadPostImage(MultipartFile file, String directoryPath);

    void deleteImageFromS3(String imageAddress);
}
