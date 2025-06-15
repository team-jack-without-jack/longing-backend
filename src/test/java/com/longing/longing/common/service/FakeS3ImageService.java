package com.longing.longing.common.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class FakeS3ImageService implements S3ImageService{

    private List<MultipartFile> postImages = new ArrayList<>();
    @Override
    public String uploadLocationImage(MultipartFile file, String directoryPath, String locationImageName) {
        return null;
    }

    @Override
    public String uploadProfileImage(MultipartFile file, String directoryPath) {
        return null;
    }

    @Override
    public String uploadPostImage(MultipartFile file, String directoryPath) {
        postImages.add(file);
        return directoryPath;
    }

    @Override
    public void deleteImageFromS3(String imageAddress) {

    }
}
