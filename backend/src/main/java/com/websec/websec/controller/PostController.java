package com.websec.websec.controller;

import com.websec.websec.dto.CreatePostRequest;
import com.websec.websec.dto.PageResponse;
import com.websec.websec.dto.PostResponse;
import com.websec.websec.security.CurrentUser;
import com.websec.websec.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/posts")
public class PostController {

	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	@GetMapping
	public PageResponse<PostResponse> feed(@RequestParam(defaultValue = "0") @Min(0) @Max(1000) int page,
			@AuthenticationPrincipal Jwt jwt) {
		return postService.feed(page, CurrentUser.idOrNull(jwt));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PostResponse> create(@Valid @ModelAttribute CreatePostRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		PostResponse post = postService.create(request, CurrentUser.id(jwt));
		return ResponseEntity.created(URI.create("/api/posts/" + post.id())).body(post);
	}

	@GetMapping("/{postId}")
	public PostResponse get(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt) {
		return postService.get(postId, CurrentUser.idOrNull(jwt));
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<Void> delete(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt) {
		postService.delete(postId, CurrentUser.id(jwt));
		return ResponseEntity.noContent().build();
	}
}
