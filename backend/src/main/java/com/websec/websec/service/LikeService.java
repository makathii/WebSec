package com.websec.websec.service;

import com.websec.websec.exception.ApiException;
import com.websec.websec.model.PostLike;
import com.websec.websec.repository.LikeRepository;
import com.websec.websec.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

	private final LikeRepository likeRepository;

	private final PostRepository postRepository;

	public LikeService(LikeRepository likeRepository, PostRepository postRepository) {
		this.likeRepository = likeRepository;
		this.postRepository = postRepository;
	}

	@Transactional
	public void like(long postId, long userId) {
		requirePost(postId);
		if (!likeRepository.existsByPostIdAndUserId(postId, userId)) {
			likeRepository.save(new PostLike(postId, userId));
		}
	}

	@Transactional
	public void unlike(long postId, long userId) {
		requirePost(postId);
		likeRepository.deleteByPostIdAndUserId(postId, userId);
	}

	private void requirePost(long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Post not found");
		}
	}
}
