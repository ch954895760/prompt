package com.prompt.controller;

import com.prompt.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    private Path absoluteUploadPath;

    @PostConstruct
    public void init() {
        // 使用绝对路径，确保文件保存到项目目录下
        String userDir = System.getProperty("user.dir");
        absoluteUploadPath = Paths.get(userDir, uploadPath).toAbsolutePath().normalize();
    }

    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            return Result.error("仅支持 jpg、jpeg、png、gif、webp 格式的图片");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过 5MB");
        }

        try {
            Path uploadDir = absoluteUploadPath.resolve("avatars");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String newFilename = UUID.randomUUID().toString().replace("-", "") + suffix;
            Path filePath = uploadDir.resolve(newFilename);
            
            // 使用 File 对象保存文件
            File destFile = filePath.toFile();
            file.transferTo(destFile);

            String fileUrl = "/uploads/avatars/" + newFilename;
            return Result.success(fileUrl);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
