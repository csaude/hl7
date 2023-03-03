package mz.org.fgh.hl7.service;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.Location;
import mz.org.fgh.hl7.LocationSearch;

@Service
public class LocationServiceImpl implements LocationService {

    private static final String PROVINCE_TAG = "Provincia";

    private static final String REPRESENTATION = "custom:(uuid,name,childLocations:(uuid,name,childLocations:(uuid,name)))";

    private final WebClient webClient;

    public LocationServiceImpl(
            WebClient.Builder webClientBuilder,
            @Value("${openmrs.url}") String baseUrl,
            @Value("${openmrs.username}") String username,
            @Value("${openmrs.password}") String password) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("ws", "rest", "v1");

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .uriBuilderFactory(new DefaultUriBuilderFactory(builder))
                .filter(basicAuthentication(username, password))
                .build();
    }

    @Cacheable("allProvinces")
    public List<Location> findAllProvinces() {
        try {

            return webClient.get()
                    .uri("/location?tag={tag}&v={representation}", PROVINCE_TAG, REPRESENTATION)
                    .retrieve()
                    .bodyToMono(LocationSearch.class)
                    .map(LocationSearch::getResults)
                    .block();

        } catch (WebClientException e) {

            Throwable cause = e.getMostSpecificCause();

            if (cause instanceof SocketTimeoutException) {
                throw new AppException("hl7.fetch.location.error.timeout", cause);
            }
            if (cause instanceof ConnectException) {
                throw new AppException("hl7.fetch.location.error.connect", cause);
            }
            if (cause instanceof IOException) {
                throw new AppException("hl7.fetch.location.error", cause);
            }

            throw e;
        }
    }

    @Cacheable("provinceByUuid")
    public Location findByUuid(String uuid) {
        try {

            return webClient.get()
                    .uri("/location/{uuid}?v={reprensentation}", uuid, REPRESENTATION)
                    .retrieve()
                    .bodyToMono(Location.class)
                    .block();

        } catch (WebClientException e) {

            Throwable cause = e.getMostSpecificCause();

            if (cause instanceof SocketTimeoutException) {
                throw new AppException("hl7.fetch.location.error.timeout", cause);
            }
            if (cause instanceof ConnectException) {
                throw new AppException("hl7.fetch.location.error.connect", cause);
            }
            if (cause instanceof IOException) {
                throw new AppException("hl7.fetch.location.error", cause);
            }

            throw e;
        }
    }
}
