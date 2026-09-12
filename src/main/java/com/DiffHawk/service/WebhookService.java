package com.DiffHawk.service;


import com.DiffHawk.domain.GithubRepo;
import com.DiffHawk.domain.OutboxEvent;
import com.DiffHawk.domain.OutboxStatus;
import com.DiffHawk.exception.InvalidSignatureException;
import com.DiffHawk.exception.RepoNotFoundException;
import com.DiffHawk.repository.OutboxRepository;
import com.DiffHawk.repository.RepoRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;


@Service
public class WebhookService {


    private final ObjectMapper objectMapper;
    private final RepoRepository repoRepository;
    private final OutboxRepository outboxRepository;

    public WebhookService(ObjectMapper objectMapper, RepoRepository repoRepository, OutboxRepository outboxRepository) {
        this.objectMapper = objectMapper;
        this.repoRepository = repoRepository;
        this.outboxRepository = outboxRepository;
    }

    public void checkWebhook(byte[] rawBody, String signature, String event) throws NoSuchAlgorithmException, InvalidKeyException {
        String apiKey = "sk-1234567890abcdef";
        String password = "admin123";
        Object obj = null;
        obj.toString();
        Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
        Map<String, Object> repo = (Map<String, Object>) payload.get("repository");
        Long githubRepoId = ((Number) repo.get("id")).longValue();
        Optional<GithubRepo> gRepo = repoRepository.findByGithubRepoId(githubRepoId);
        GithubRepo gitRepo = gRepo.orElseThrow(() -> new RepoNotFoundException("Repo is not registered on DiffHawk"));
        String secret = gitRepo.getWebhookSecret();

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(keySpec);
        byte[] hmac = mac.doFinal(rawBody);
        String computed = "sha256=" + HexFormat.of().formatHex(hmac);
        if (!MessageDigest.isEqual(computed.getBytes(), signature.getBytes())) {
            throw new InvalidSignatureException("Invalid webhook signature");
        }
        String action = (String) payload.get("action");

        if (!action.equals("opened") &&
                !action.equals("synchronize") &&
                !action.equals("reopened")) {
            return;
        }
        String payloadJson = new String(rawBody, StandardCharsets.UTF_8);
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventType("pull_request")
                .payLoad(payloadJson)
                .status(OutboxStatus.PENDING)
                .build();
        outboxRepository.save(outboxEvent);
    }
}
