package org.chengyuan.cyproject.service.impl;

import org.chengyuan.cyproject.dto.response.UploadImageResponse;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.error.errors.BizException;
import org.chengyuan.cyproject.service.UploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final long MAX_SIZE = 10 * 1024 * 1024L; // 10 MB

    /** 上传文件对外访问的 URL 前缀，与 WebConfig 中 /uploads/** 的静态映射保持一致 */
    private static final String URL_PREFIX = "uploads";

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${url}")
    private String url;

    @Override
    public UploadImageResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }

        String extension = extensionOf(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID().toString() + extension;
        Path path = dir.resolve(filename);

        try {
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }

        BufferedImage image = readImage(path);
        if (image == null) {
            // 文件不是可解析的图片，清理并拒绝
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // 清理失败不影响主流程
            }
            throw new BizException(ErrorCode.BAD_REQUEST);
        }

        UploadImageResponse response = new UploadImageResponse();
        response.setUrl(url + URL_PREFIX + "/" + filename);
        response.setSize(file.getSize());
        response.setWidth(image.getWidth());
        response.setHeight(image.getHeight());
        response.setMime(contentType);
        return response;
    }

    private BufferedImage readImage(Path path) {
        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    private String extensionOf(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dot);
                // 只允许简单的扩展名，避免路径注入
                if (ext.matches("\\.[A-Za-z0-9]{1,10}")) {
                    return ext.toLowerCase();
                }
            }
        }
        // 根据 MIME 兜底
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".img";
        };
    }
}
