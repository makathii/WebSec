package com.websec.websec.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "image_path", nullable = false)
	private String imagePath;

	private String caption;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Post() {
	}

	public Post(User user, String imagePath, String caption) {
		this.user = user;
		this.imagePath = imagePath;
		this.caption = caption;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getImagePath() {
		return imagePath;
	}

	public String getCaption() {
		return caption;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
