package com.DiffHawk.repository;

import com.DiffHawk.domain.GithubRepo;
import com.DiffHawk.domain.Review;
import com.DiffHawk.domain.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review,Long> {
    Optional<Review> findByRepoAndPrNumberAndHeadSha(GithubRepo repo, Long prNumber, String headSha);
    List<Review> findByStatus(ReviewStatus status);
}
