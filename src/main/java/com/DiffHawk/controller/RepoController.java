package com.DiffHawk.controller;


import com.DiffHawk.dto.RepoRequestDto;
import com.DiffHawk.dto.RepoResponseDto;
import com.DiffHawk.service.RepoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repos")
public class RepoController {

    private final RepoService repoService;
    public RepoController(RepoService repoService) {
        this.repoService = repoService;
    }

    @PostMapping("/register")
    public ResponseEntity<RepoResponseDto> registerRepo(@Valid @RequestBody RepoRequestDto request){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RepoResponseDto response = repoService.registerRepo(request,userId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/mine")
    public ResponseEntity<List<RepoResponseDto>> getMyRepos(){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<RepoResponseDto> repos = repoService.getMyRepos(userId);
        return ResponseEntity.ok(repos);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepo(@PathVariable Long id){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        repoService.deleteRepo(id,userId);
        return ResponseEntity.noContent().build();
    }
}
