package com.DiffHawk.service;

import com.DiffHawk.domain.GithubRepo;
import com.DiffHawk.domain.User;
import com.DiffHawk.dto.RepoRequestDto;
import com.DiffHawk.dto.RepoResponseDto;
import com.DiffHawk.exception.RepoAlreadyRegisteredException;
import com.DiffHawk.exception.RepoNotFoundException;
import com.DiffHawk.exception.UnauthorizedException;
import com.DiffHawk.exception.UserNotFoundException;
import com.DiffHawk.repository.RepoRepository;
import com.DiffHawk.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class RepoService {

    @Value("${app.webhook-url}")
    private String webhookUrl;
    private final UserRepository userRepository;
    private final RepoRepository repoRepository;

    public RepoService(UserRepository userRepository, RepoRepository repoRepository) {
        this.userRepository = userRepository;
        this.repoRepository = repoRepository;
    }

    @Transactional
    public RepoResponseDto registerRepo(RepoRequestDto request, Long userId){
        Optional<GithubRepo> repo = repoRepository.findByOwnerAndName(request.owner(),request.name());
        if(repo.isPresent()){
            throw new RepoAlreadyRegisteredException("Repo already registered");
        }
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            throw new UserNotFoundException("User not found");
        }
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = HexFormat.of().formatHex(bytes);

        GithubRepo githubRepo = GithubRepo.builder()
                .owner(request.owner())
                .name(request.name())
                .githubRepoId(request.githubRepoId())
                .webhookSecret(secret)
                .user(user.get())
                .build();
        GithubRepo savedRepo = repoRepository.save(githubRepo);

        return new RepoResponseDto(
                savedRepo.getId(),
                savedRepo.getGithubRepoId(),
                savedRepo.getOwner(),
                savedRepo.getName(),
                webhookUrl,
                savedRepo.getWebhookSecret(),
                savedRepo.getCreatedAt()
        );
    }
    public List<RepoResponseDto> getMyRepos(Long userId){
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()){
            throw new UserNotFoundException("User not found");
        }
        List<GithubRepo> lst = repoRepository.findByUser(user.get());

        return lst.stream()
                .map(repo -> new RepoResponseDto(
                        repo.getId(),
                        repo.getGithubRepoId(),
                        repo.getOwner(),
                        repo.getName(),
                        webhookUrl,
                        repo.getWebhookSecret(),
                        repo.getCreatedAt()

                ))
                .toList();
    }
    @Transactional
    public void deleteRepo(Long repoId, Long userId){
        Optional<GithubRepo> repo = repoRepository.findById(repoId);
        if(repo.isEmpty()){
            throw new RepoNotFoundException("Repo not found");
        }
        if(!repo.get().getUser().getId().equals(userId)){
            throw new UnauthorizedException("This repo does not belong to you");
        }
        repoRepository.delete(repo.get());
    }
}
