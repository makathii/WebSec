package com.websec.websec.service;

import com.websec.websec.config.AppProperties;
import com.websec.websec.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {

	@TempDir
	Path uploadDir;

	private ImageStorageService storage;

	@BeforeEach
	void setUp() throws IOException {
		String key = Base64.getEncoder().encodeToString(new byte[32]);
		storage = new ImageStorageService(
				new AppProperties(key, key, "http://localhost:5173", true, uploadDir.toString()));
	}

	@Test
	void sixteenBitPngIsRefusedBeforeDecoding() throws IOException {
		byte[] png = png(4000, 4000, 16, 6);
		assertThat(png.length).isLessThan(1024 * 1024);

		assertThatThrownBy(() -> storage.store(upload(png)))
			.isInstanceOfSatisfying(ApiException.class, e -> {
				assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(e.getMessage()).isEqualTo("Image is too large");
			});
		assertThat(Files.list(uploadDir)).isEmpty();
	}

	@Test
	void normalFullSizePngIsAccepted() throws IOException {
		String path = storage.store(upload(png(4000, 4000, 8, 2)));

		assertThat(path).matches("uploads/[0-9a-f-]{36}\\.png");
		assertThat(Files.list(uploadDir)).hasSize(1);
	}

	private static MockMultipartFile upload(byte[] data) {
		return new MockMultipartFile("image", "a.png", "image/png", data);
	}

	private static byte[] png(int width, int height, int bitDepth, int colorType) throws IOException {
		int channels = colorType == 6 ? 4 : 3;
		byte[] row = new byte[1 + width * channels * bitDepth / 8];
		ByteArrayOutputStream pixels = new ByteArrayOutputStream();
		try (DeflaterOutputStream deflate = new DeflaterOutputStream(pixels, new Deflater(9))) {
			for (int y = 0; y < height; y++) {
				deflate.write(row);
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' });
		chunk(out, "IHDR", ByteBuffer.allocate(13).putInt(width).putInt(height)
			.put((byte) bitDepth).put((byte) colorType).put((byte) 0).put((byte) 0).put((byte) 0).array());
		chunk(out, "IDAT", pixels.toByteArray());
		chunk(out, "IEND", new byte[0]);
		return out.toByteArray();
	}

	private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
		byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
		CRC32 crc = new CRC32();
		crc.update(typeBytes);
		crc.update(data);
		out.write(ByteBuffer.allocate(4).putInt(data.length).array());
		out.write(typeBytes);
		out.write(data);
		out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
	}
}
