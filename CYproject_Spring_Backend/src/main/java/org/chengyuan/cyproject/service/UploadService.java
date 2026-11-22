package org.chengyuan.cyproject.service;

import org.chengyuan.cyproject.dto.response.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    UploadImageResponse uploadImage(MultipartFile file);
}
