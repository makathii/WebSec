package com.websec.websec.service;

import com.websec.websec.exception.ApiException;
import com.websec.websec.repository.CommentRepository;
import com.websec.websec.repository.LikeRepository;
import com.websec.websec.repository.PostRepository;
import com.websec.websec.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceTest {

	private final PostRepository postRepository = mock(PostRepository.class);

	private final ImageStorageService imageStorage = mock(ImageStorageService.class);

	private final PostService postService = new PostService(postRepository, mock(UserRepository.class),
			mock(LikeRepository.class), mock(CommentRepository.class), imageStorage, mock(UserService.class));

	@Test
	void userCannotDeleteSomeoneElsesPost() {
		when(postRepository.findByIdAndUser_Id(5L, 2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> postService.delete(5L, 2L))
			.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
		verify(postRepository, never()).delete(any());
		verify(imageStorage, never()).delete(anyString());
	}
}
