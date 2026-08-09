package com.hasan.gateway.controllers;

import com.hasan.gateway.dtos.NewClientResponse;
import com.hasan.gateway.dtos.RegistrationRequest;
import com.hasan.gateway.entities.Client;
import com.hasan.gateway.services.ClientService;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // 1. CREATE
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> registerClient(
            @Valid @RequestBody RegistrationRequest request,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {

        if (adminKey == null || !adminKey.equals("super-secret-admin-password-123!")) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Fatal: Only authorized backend servers can register new keys.")));
        }

        return clientService.registerClientAndGenerateKey(
                request.companyName(), request.email(), request.tierType())
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    // 2. READ
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Client>> getClient(@PathVariable UUID id) {
        return clientService.findById(id)
                .map(ResponseEntity::ok);
    }

    // 3. UPDATE
    @PutMapping("/{id}/tier")
    public Mono<ResponseEntity<String>> updateTier(@PathVariable UUID id, @RequestParam String newTier) {
        return clientService.updateTier(id, newTier)
                .map(client -> ResponseEntity.ok("Client " + id + " successfully upgraded to " + newTier + " tier."));
    }

    // 4. DELETE
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteClient(@PathVariable UUID id) {
        return clientService.deleteById(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}