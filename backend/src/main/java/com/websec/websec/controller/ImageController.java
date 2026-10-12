package com.websec.websec.controller;

import com.websec.websec.service.ImageStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class ImageController {

	private final ImageStorageService imageStorage;

	public ImageController(ImageStorageService imageStorage) {
		this.imageStorage = imageStorage;
	}

	// filename is checked against the UUID pattern in ImageStorageService, so no path traversal
	@GetMapping("/uploads/{filename}")
	public ResponseEntity<Resource> get(@PathVariable String filename) {
		FileSystemResource file = new FileSystemResource(imageStorage.find(filename));
		MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
		return ResponseEntity.ok()
			.contentType(type)
			.cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
			.body(file);
	}
}
