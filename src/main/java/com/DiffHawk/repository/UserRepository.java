package com.DiffHawk.repository;

import com.DiffHawk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByGithubUserId(Long githubUserId);
    Optional<User> findByUsername(String username);
}
