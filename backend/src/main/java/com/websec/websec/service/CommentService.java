package com.websec.websec.service;

import com.websec.websec.dto.CommentRequest;
import com.websec.websec.dto.CommentResponse;
import com.websec.websec.dto.PageResponse;
import com.websec.websec.exception.ApiException;
import com.websec.websec.model.Comment;
import com.websec.websec.model.Post;
import com.websec.websec.repository.CommentRepository;
import com.websec.websec.repository.PostRepository;
import com.websec.websec.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

	public static final int PAGE_SIZE = 50;

	private final CommentRepository commentRepository;

	private final PostRepository postRepository;

	private final UserRepository userRepository;

	public CommentService(CommentRepository commentRepository, PostRepository postRepository,
			UserRepository userRepository) {
		this.commentRepository = commentRepository;
		this.postRepository = postRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<CommentResponse> list(long postId, int page) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Post not found");
		}
		Page<Comment> comments = commentRepository.findByPost_IdOrderByIdAsc(postId, PageRequest.of(page, PAGE_SIZE));
		return PageResponse.of(comments, comments.map(CommentResponse::from).getContent());
	}

	@Transactional
	public CommentResponse add(long postId, CommentRequest request, long userId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));
		Comment comment = new Comment(post, userRepository.getReferenceById(userId), request.text());
		return CommentResponse.from(commentRepository.save(comment));
	}

	@Transactional
	public void delete(long postId, long commentId, long userId) {
		Comment comment = commentRepository.findByIdAndPost_IdAndUser_Id(commentId, postId, userId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
		commentRepository.delete(comment);
	}
}
