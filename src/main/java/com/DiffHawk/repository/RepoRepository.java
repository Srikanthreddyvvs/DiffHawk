package com.DiffHawk.repository;

import com.DiffHawk.domain.GithubRepo;
import com.DiffHawk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface RepoRepository extends JpaRepository<GithubRepo,Long> {
    List<GithubRepo> findByUser(User user);
    Optional<GithubRepo> findByOwnerAndName(String owner, String name);
    Optional<GithubRepo> findByGithubRepoId(Long githubRepoId);
}
