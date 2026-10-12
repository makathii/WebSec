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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

@Service
public class ImageStorageService {

	private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

	private static final String PATH_PREFIX = "uploads/";

	private static final int MAX_DIMENSION = 4000;

	private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

	private static final byte[] PNG_MAGIC = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

	private final Path uploadDir;

	public ImageStorageService(AppProperties properties) throws IOException {
		this.uploadDir = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
		Files.createDirectories(uploadDir);
		ImageIO.setUseCache(false);
	}

	/**
	 * Stores the image and returns the relative path that goes into the database.
	 * The client filename and content type are ignored. The type comes from the magic bytes and
	 * the image is decoded and encoded again, which drops EXIF data and anything hidden in the file.
	 */
	public String store(MultipartFile file) {
		byte[] bytes;
		try {
			bytes = file.getBytes();
		}
		catch (IOException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read file");
		}

		String extension;
		if (startsWith(bytes, JPEG_MAGIC)) {
			extension = "jpg";
		}
		else if (startsWith(bytes, PNG_MAGIC)) {
			extension = "png";
		}
		else {
			throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG and PNG images are allowed");
		}

		BufferedImage image = clean(decode(bytes, extension), extension);
		String filename = UUID.randomUUID() + "." + extension;
		Path target = resolve(filename);
		try {
			if (!ImageIO.write(image, extension.equals("jpg") ? "jpeg" : "png", target.toFile())) {
				throw new IOException("No image writer");
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

	private BufferedImage decode(byte[] bytes, String extension) {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(extension.equals("jpg") ? "jpeg" : "png");
		if (!readers.hasNext()) {
			throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid image");
		}
		ImageReader reader = readers.next();
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			reader.setInput(input, true, true);
			// check the size from the header before decoding, so huge images can't eat all memory
			int width = reader.getWidth(0);
			int height = reader.getHeight(0);
			if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Image must be at most 4000x4000 pixels");
			}
			return reader.read(0);
		}
		catch (ApiException e) {
			throw e;
		}
		catch (IOException | RuntimeException e) {
			throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid image");
		}
		finally {
			reader.dispose();
		}
	}

	private BufferedImage clean(BufferedImage source, String extension) {
		int type = extension.equals("jpg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), type);
		Graphics2D graphics = copy.createGraphics();
		graphics.drawImage(source, 0, 0, null);
		graphics.dispose();
		return copy;
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

	private boolean startsWith(byte[] bytes, byte[] prefix) {
		if (bytes.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (bytes[i] != prefix[i]) {
				return false;
			}
		}
		return true;
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
