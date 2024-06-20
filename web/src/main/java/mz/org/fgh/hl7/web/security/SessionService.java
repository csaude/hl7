package mz.org.fgh.hl7.web.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import mz.org.fgh.hl7.web.AppException;

@Component
public class SessionService {

    private final WebClient webClient;

    public SessionService(
            WebClient.Builder webClientBuilder,
            @Value("${openmrs.url}") String baseUrl) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("ws", "rest", "v1");

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .uriBuilderFactory(new DefaultUriBuilderFactory(builder))
                .build();
    }

    public OpenmrsSession getSession(String username, String password) {
        return webClient
                .get()
                .uri("/session")
                .headers(h -> h.setBasicAuth(username, password))
                .retrieve()
                .bodyToMono(OpenmrsSession.class)
                .onErrorMap(IOException.class,
                        e -> new AppException("hl7.openmrs.login.error", e))
                .block();
    }

}
