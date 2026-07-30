package com.distributed.job_validator.greenhouse;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class GreenhouseValidatorService {

    private final WebClient webClient;

    public GreenhouseValidatorService() {
        ConnectionProvider provider = ConnectionProvider.builder("greenhouse-pool")
                .maxConnections(300) 
                .pendingAcquireMaxCount(1000)
                .pendingAcquireTimeout(Duration.ofSeconds(3)) // Dropped to 3s
                .maxIdleTime(Duration.ofSeconds(60)) 
                .maxLifeTime(Duration.ofMinutes(5))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000) // Dropped to 3s
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true) 
                .responseTimeout(Duration.ofSeconds(3)) // Dropped to 3s
                .secure(sslSpec -> {
                    try {
                        sslSpec.sslContext(io.netty.handler.ssl.SslContextBuilder.forClient().build())
                               .handshakeTimeout(Duration.ofSeconds(3)); // Dropped to 3s
                    } catch (javax.net.ssl.SSLException e) {
                        throw new RuntimeException("Failed to build SSL Context", e);
                    }
                })
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS)) // Dropped to 3s
                        .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS)) // Dropped to 3s
                );

        this.webClient = WebClient.builder()
                .baseUrl("https://boards-api.greenhouse.io/v1/boards/")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept-Encoding", "gzip, deflate")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public Mono<Boolean> validateSlug(String slug) {
        return webClient.get()
                .uri(slug.trim())
                .exchangeToMono(response -> 
                    // Safely dump the response body to prevent leaks without throwing exceptions
                    response.releaseBody()
                            .thenReturn(response.statusCode().is2xxSuccessful())
                )
                .onErrorReturn(false);
    }
}