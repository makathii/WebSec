package com.websec.websec.repository;

import com.websec.websec.dto.PostCount;
import com.websec.websec.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@EntityGraph(attributePaths = "user")
	Page<Comment> findByPost_IdOrderByIdAsc(Long postId, Pageable pageable);

	Optional<Comment> findByIdAndPost_IdAndUser_Id(Long id, Long postId, Long userId);

	@Query("select new com.websec.websec.dto.PostCount(c.post.id, count(c)) from Comment c "
			+ "where c.post.id in :postIds group by c.post.id")
	List<PostCount> countByPostIds(@Param("postIds") Collection<Long> postIds);
}
