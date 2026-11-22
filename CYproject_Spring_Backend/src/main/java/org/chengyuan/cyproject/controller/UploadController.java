package org.chengyuan.cyproject.controller;

import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.dto.response.UploadImageResponse;
import org.chengyuan.cyproject.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/images")
    public Response<UploadImageResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        return Response.ok(uploadService.uploadImage(file));
    }
}
