package com.websec.websec.repository;

import com.websec.websec.dto.PostCount;
import com.websec.websec.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LikeRepository extends JpaRepository<PostLike, Long> {

	boolean existsByPostIdAndUserId(Long postId, Long userId);

	@Modifying
	@Query("delete from PostLike l where l.postId = :postId and l.userId = :userId")
	void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

	@Query("select new com.websec.websec.dto.PostCount(l.postId, count(l)) from PostLike l "
			+ "where l.postId in :postIds group by l.postId")
	List<PostCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

	@Query("select l.postId from PostLike l where l.userId = :userId and l.postId in :postIds")
	List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
