package com.websec.websec.service;

import com.websec.websec.dto.CreatePostRequest;
import com.websec.websec.dto.PageResponse;
import com.websec.websec.dto.PostCount;
import com.websec.websec.dto.PostResponse;
import com.websec.websec.exception.ApiException;
import com.websec.websec.model.Post;
import com.websec.websec.model.User;
import com.websec.websec.repository.CommentRepository;
import com.websec.websec.repository.LikeRepository;
import com.websec.websec.repository.PostRepository;
import com.websec.websec.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

	public static final int PAGE_SIZE = 20;

	private final PostRepository postRepository;

	private final UserRepository userRepository;

	private final LikeRepository likeRepository;

	private final CommentRepository commentRepository;

	private final ImageStorageService imageStorage;

	private final UserService userService;

	public PostService(PostRepository postRepository, UserRepository userRepository, LikeRepository likeRepository,
			CommentRepository commentRepository, ImageStorageService imageStorage, UserService userService) {
		this.postRepository = postRepository;
		this.userRepository = userRepository;
		this.likeRepository = likeRepository;
		this.commentRepository = commentRepository;
		this.imageStorage = imageStorage;
		this.userService = userService;
	}

	@Transactional(readOnly = true)
	public PageResponse<PostResponse> feed(int page, Long currentUserId) {
		Page<Post> posts = postRepository.findAllByOrderByIdDesc(PageRequest.of(page, PAGE_SIZE));
		return PageResponse.of(posts, toResponses(posts.getContent(), currentUserId));
	}

	@Transactional(readOnly = true)
	public PageResponse<PostResponse> byUser(String username, int page, Long currentUserId) {
		User user = userService.findByUsername(username);
		Page<Post> posts = postRepository.findByUser_IdOrderByIdDesc(user.getId(), PageRequest.of(page, PAGE_SIZE));
		return PageResponse.of(posts, toResponses(posts.getContent(), currentUserId));
	}

	@Transactional(readOnly = true)
	public PostResponse get(long postId, Long currentUserId) {
		Post post = postRepository.findWithUserById(postId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));
		return toResponses(List.of(post), currentUserId).getFirst();
	}

	@Transactional
	public PostResponse create(CreatePostRequest request, long userId) {
		User user = userRepository.getReferenceById(userId);
		String imagePath = imageStorage.store(request.image());
		try {
			Post post = postRepository.saveAndFlush(new Post(user, imagePath, request.caption()));
			return get(post.getId(), userId);
		}
		catch (RuntimeException e) {
			imageStorage.delete(imagePath);
			throw e;
		}
	}

	@Transactional
	public void delete(long postId, long userId) {
		// only finds the post if it belongs to the user, otherwise 404 (no hint that it exists)
		Post post = postRepository.findByIdAndUser_Id(postId, userId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));
		postRepository.delete(post);
		postRepository.flush();
		imageStorage.delete(post.getImagePath());
	}

	private List<PostResponse> toResponses(List<Post> posts, Long currentUserId) {
		if (posts.isEmpty()) {
			return List.of();
		}
		List<Long> ids = posts.stream().map(Post::getId).toList();
		Map<Long, Long> likes = toMap(likeRepository.countByPostIds(ids));
		Map<Long, Long> comments = toMap(commentRepository.countByPostIds(ids));
		Set<Long> liked = currentUserId == null
				? Set.of()
				: new HashSet<>(likeRepository.findLikedPostIds(currentUserId, ids));

		return posts.stream()
			.map(post -> new PostResponse(
					post.getId(),
					post.getUser().getUsername(),
					"/" + post.getImagePath(),
					post.getCaption(),
					likes.getOrDefault(post.getId(), 0L),
					comments.getOrDefault(post.getId(), 0L),
					liked.contains(post.getId()),
					post.getCreatedAt()))
			.toList();
	}

	private Map<Long, Long> toMap(List<PostCount> counts) {
		return counts.stream().collect(Collectors.toMap(PostCount::postId, PostCount::count));
	}
}
