package com.hasan.gateway.services;

import com.hasan.gateway.dtos.NewClientResponse;
import com.hasan.gateway.entities.ApiKey;
import com.hasan.gateway.entities.Client;
import com.hasan.gateway.repos.ApiKeyRepo;
import com.hasan.gateway.repos.ClientRepo;
import com.hasan.gateway.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepo clientRepo;

    @Mock
    private ApiKeyRepo apiKeyRepo;

    @InjectMocks
    private ClientService clientService;

    @Test
    void registerClient_ProTier_Assigns3000LimitAndHashesKey() {
        // --- 1. ARRANGE ---
        String companyName = "Google";
        String email = "admin@google.com";
        String tier = "PRO";

        UUID mockClientId = UUID.randomUUID();

        when(clientRepo.save(any(Client.class))).thenAnswer(invocation -> {
            Client c = invocation.getArgument(0);
            c.setId(mockClientId);
            return reactor.core.publisher.Mono.just(c);
        });

        when(apiKeyRepo.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey k = invocation.getArgument(0);
            k.setId(UUID.randomUUID());
            return reactor.core.publisher.Mono.just(k);
        });

        // --- 2. ACT & VERIFY ---
        StepVerifier.create(clientService.registerClientAndGenerateKey(companyName, email, tier))
                .assertNext(response -> {
                    assertNotNull(response.apiKey());
                    assertTrue(response.apiKey().startsWith("sk_live_"));
                    assertEquals(mockClientId, response.clientId());
                })
                .verifyComplete();

        // --- 3. ASSERT REPOSITORY INTERACTIONS ---
        verify(clientRepo, times(1)).save(any(Client.class));

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepo, times(1)).save(apiKeyCaptor.capture());
        
        ApiKey savedApiKey = apiKeyCaptor.getValue();

        assertEquals(3000, savedApiKey.getRequestLimit());
        assertEquals("PRO", savedApiKey.getTier());

        String rawKeyExtracted = apiKeyCaptor.getValue().getKeyHash();
        assertNotNull(rawKeyExtracted);
    }

    @Test
    void registerClient_FreeTier_Assigns20Limit() {
        // --- 1. ARRANGE ---
        UUID mockClientId = UUID.randomUUID();

        when(clientRepo.save(any(Client.class))).thenAnswer(invocation -> {
            Client c = invocation.getArgument(0);
            c.setId(mockClientId);
            return reactor.core.publisher.Mono.just(c);
        });

        when(apiKeyRepo.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey k = invocation.getArgument(0);
            k.setId(UUID.randomUUID());
            return reactor.core.publisher.Mono.just(k);
        });

        // --- 2. ACT & VERIFY ---
        StepVerifier.create(clientService.registerClientAndGenerateKey("Startup", "test@startup.com", "FREE"))
                .expectNextCount(1)
                .verifyComplete();

        // --- 3. ASSERT ---
        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepo, times(1)).save(apiKeyCaptor.capture());
        
        assertEquals(20, apiKeyCaptor.getValue().getRequestLimit());
        assertEquals("FREE", apiKeyCaptor.getValue().getTier());
    }
}