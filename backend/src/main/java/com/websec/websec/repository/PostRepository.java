package com.websec.websec.repository;

import com.websec.websec.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

	@EntityGraph(attributePaths = "user")
	Page<Post> findAllByOrderByIdDesc(Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Page<Post> findByUser_IdOrderByIdDesc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Optional<Post> findWithUserById(Long id);

	Optional<Post> findByIdAndUser_Id(Long id, Long userId);

	long countByUser_Id(Long userId);

	@Query("select p.imagePath from Post p where p.user.id = :userId")
	List<String> findImagePathsByUserId(@Param("userId") Long userId);
}
