package com.websec.websec.controller;

import com.websec.websec.dto.CommentRequest;
import com.websec.websec.dto.CommentResponse;
import com.websec.websec.dto.PageResponse;
import com.websec.websec.security.CurrentUser;
import com.websec.websec.service.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping
	public PageResponse<CommentResponse> list(@PathVariable long postId,
			@RequestParam(defaultValue = "0") @Min(0) @Max(1000) int page) {
		return commentService.list(postId, page);
	}

	@PostMapping
	public ResponseEntity<CommentResponse> add(@PathVariable long postId, @Valid @RequestBody CommentRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		CommentResponse comment = commentService.add(postId, request, CurrentUser.id(jwt));
		return ResponseEntity.created(URI.create("/api/posts/" + postId + "/comments/" + comment.id())).body(comment);
	}

	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> delete(@PathVariable long postId, @PathVariable long commentId,
			@AuthenticationPrincipal Jwt jwt) {
		commentService.delete(postId, commentId, CurrentUser.id(jwt));
		return ResponseEntity.noContent().build();
	}
}
