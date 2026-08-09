package com.hasan.gateway.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hasan.gateway.dtos.NewClientResponse;
import com.hasan.gateway.entities.ApiKey;
import com.hasan.gateway.entities.Client;
import com.hasan.gateway.repos.ApiKeyRepo;
import com.hasan.gateway.repos.ClientRepo;
import com.hasan.gateway.security.SecurityUtil;

import reactor.core.publisher.Mono;

@Service
public class ClientService {

    private final ClientRepo clientRepo;
    private final ApiKeyRepo apiKeyRepo;

    public ClientService(ClientRepo clientRepo, ApiKeyRepo apiKeyRepo) {
        this.clientRepo = clientRepo;
        this.apiKeyRepo = apiKeyRepo;
    }

    public Mono<NewClientResponse> registerClientAndGenerateKey(String companyName, String email, String tierType) {
        Client client = new Client();
        client.setCompanyName(companyName);
        client.setEmail(email);
        client.setTierType(tierType);

        // 1. Save Client non-blockingly, then chain the API key creation
        return clientRepo.save(client)
                .flatMap(savedClient -> {
                    // 2. Generate a secure, raw API key
                    String rawApiKey = "sk_live_" + UUID.randomUUID().toString().replace("-", "");

                    // 3. Hash the key securely
                    String hashedKey = SecurityUtil.hashKey(rawApiKey);

                    // 4. Create and configure the ApiKey entity (including denormalized tier)
                    ApiKey apiKey = new ApiKey();
                    apiKey.setClientId(savedClient.getId());
                    apiKey.setKeyHash(hashedKey);
                    apiKey.setTier(tierType); 
                    apiKey.setRequestLimit("PRO".equalsIgnoreCase(tierType) ? 3000 : 20);

                    return apiKeyRepo.save(apiKey)
                            .thenReturn(new NewClientResponse(savedClient.getId(), rawApiKey));
                });
    }

    public Mono<Client> findById(UUID id) {
        return clientRepo.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Client not found with id: " + id)));
    }

    public Mono<Client> updateTier(UUID id, String newTier) {
        return findById(id)
                .flatMap(client -> {
                    client.setTierType(newTier);
                    return clientRepo.save(client);
                });
    }

    public Mono<Void> deleteById(UUID id) {
        return clientRepo.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RuntimeException("Cannot delete: Client not found with id: " + id));
                    }
                    return clientRepo.deleteById(id);
                });
    }
}