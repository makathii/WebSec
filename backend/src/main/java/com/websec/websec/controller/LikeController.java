package com.websec.websec.controller;

import com.websec.websec.security.CurrentUser;
import com.websec.websec.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /like is the like of the logged-in user on this post
@RestController
@RequestMapping("/api/posts/{postId}/like")
public class LikeController {

	private final LikeService likeService;

	public LikeController(LikeService likeService) {
		this.likeService = likeService;
	}

	@PutMapping
	public ResponseEntity<Void> like(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt) {
		likeService.like(postId, CurrentUser.id(jwt));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	public ResponseEntity<Void> unlike(@PathVariable long postId, @AuthenticationPrincipal Jwt jwt) {
		likeService.unlike(postId, CurrentUser.id(jwt));
		return ResponseEntity.noContent().build();
	}
}
