package com.websec.websec.service;

import com.websec.websec.config.AppProperties;
import com.websec.websec.exception.ApiException;
import com.websec.websec.validation.Patterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

@Service
public class ImageStorageService {

	private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

	private static final String PATH_PREFIX = "uploads/";

	private static final int MAX_SIZE = 4000;

	private final Path uploadDir;

	public ImageStorageService(AppProperties properties) throws IOException {
		this.uploadDir = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
		Files.createDirectories(uploadDir);
		ImageIO.setUseCache(false);
	}

	/**
	 * Saves the image and returns the path for the database.
	 * We don't trust the filename or content type from the client. ImageIO looks at the file itself
	 * to find out the format, and we save a freshly encoded copy, so metadata (EXIF) is not kept.
	 */
	public String store(MultipartFile file) {
		BufferedImage image;
		String format;
		try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG and PNG images are allowed");
			}
			ImageReader reader = readers.next();
			try {
				format = reader.getFormatName().toLowerCase();
				if (!format.equals("jpeg") && !format.equals("png")) {
					throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG and PNG images are allowed");
				}
				reader.setInput(input);
				// check the size first, a small file can still be a huge image
				if (reader.getWidth(0) > MAX_SIZE || reader.getHeight(0) > MAX_SIZE) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "Image must be at most 4000x4000 pixels");
				}
				image = reader.read(0);
			}
			finally {
				reader.dispose();
			}
		}
		catch (ApiException e) {
			throw e;
		}
		catch (IOException | RuntimeException e) {
			// broken files can make the decoder throw all kinds of exceptions
			throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid image");
		}

		String filename = UUID.randomUUID() + (format.equals("jpeg") ? ".jpg" : ".png");
		Path target = resolve(filename);
		try {
			if (!ImageIO.write(image, format, target.toFile())) {
				throw new IOException("No writer for " + format);
			}
		}
		catch (IOException e) {
			deleteQuietly(target);
			throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid image");
		}
		return PATH_PREFIX + filename;
	}

	public Path find(String filename) {
		Path file = resolve(filename);
		if (!Files.isRegularFile(file)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
		}
		return file;
	}

	public void delete(String imagePath) {
		if (imagePath.startsWith(PATH_PREFIX)) {
			deleteQuietly(resolve(imagePath.substring(PATH_PREFIX.length())));
		}
	}

	private Path resolve(String filename) {
		if (filename == null || !filename.matches(Patterns.IMAGE_FILENAME)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
		}
		Path file = uploadDir.resolve(filename).normalize();
		if (!file.startsWith(uploadDir)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
		}
		return file;
	}

	private void deleteQuietly(Path file) {
		try {
			Files.deleteIfExists(file);
		}
		catch (IOException e) {
			log.warn("Could not delete {}", file.getFileName());
		}
	}
}
