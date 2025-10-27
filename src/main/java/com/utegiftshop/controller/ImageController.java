package com.utegiftshop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/api/images") // Base path cho images
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Value("${app.upload.dir}") // Đọc đường dẫn từ properties
    private String uploadDir;

    @GetMapping("/{filename:.+}") // Lấy cả đuôi file
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                logger.debug("Serving image: {}, Type: {}", filename, contentType); // Đổi thành debug
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                logger.warn("Image not found/readable: {}", imagePath.toString());
                return ResponseEntity.notFound().build(); // Trả 404
            }
        } catch (MalformedURLException e) {
            logger.error("Malformed URL for image: {}", filename, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
             logger.error("Error reading image file: {}", filename, e);
             return ResponseEntity.status(500).build();
        }
    }
}