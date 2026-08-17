package com.distributed.job_validator.bamboohr;

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
public class BambooHRValidatorService {

    private final WebClient webClient;

    public BambooHRValidatorService() {
        ConnectionProvider provider = ConnectionProvider.builder("bamboohr-pool")
                .maxConnections(300)
                .pendingAcquireMaxCount(1000)
                .pendingAcquireTimeout(Duration.ofSeconds(3))
                .maxIdleTime(Duration.ofSeconds(10))
                .maxLifeTime(Duration.ofMinutes(5))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .responseTimeout(Duration.ofSeconds(3))
                .secure(sslSpec -> {
                    try {
                        sslSpec.sslContext(io.netty.handler.ssl.SslContextBuilder.forClient().build())
                               .handshakeTimeout(Duration.ofSeconds(3));
                    } catch (javax.net.ssl.SSLException e) {
                        throw new RuntimeException("Failed to build SSL Context", e);
                    }
                })
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS))
                )
                .followRedirect(false); // Important: Don't follow 301/302 redirects to generic login pages

        // CRITICAL: No baseUrl is set here because BambooHR uses dynamic subdomains
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept-Encoding", "gzip, deflate")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public Mono<Boolean> validateSlug(String slug) {
        String testUrl = String.format("https://%s.bamboohr.com/careers/list", slug.trim());
        
        return webClient.get()
                .uri(testUrl)
                .exchangeToMono(response -> 
                    response.releaseBody()
                            // If a company exists, this endpoint returns 200 OK. 
                            // If it doesn't, it usually throws a DNS error or returns a 404/302.
                            .thenReturn(response.statusCode().is2xxSuccessful())
                )
                .onErrorReturn(false);
    }
}