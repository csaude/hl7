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
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import mz.org.fgh.hl7.AppException;
import mz.org.fgh.hl7.model.Location;
import mz.org.fgh.hl7.model.LocationSearch;

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

        List<Location> locationList = webClient.get()
                .uri("/location?tag={tag}&v={representation}", PROVINCE_TAG, REPRESENTATION)
                .retrieve()
                .bodyToMono(LocationSearch.class)
                .map(LocationSearch::getResults)
                .onErrorMap(SocketTimeoutException.class,
                        e -> new AppException("hl7.fetch.location.error.timeout", e))
                .onErrorMap(ConnectException.class,
                        e -> new AppException("hl7.fetch.location.error.connect", e))
                .onErrorMap(IOException.class,
                        e -> new AppException("hl7.fetch.location.error", e))
                .block();

        if (locationList.isEmpty()) {
            throw new AppException("hl7.fetch.province.error.empty");
        }

        return locationList;
    }

    @Cacheable("provinceByUuid")
    public Location findByUuid(String uuid) {

        return webClient.get()
                .uri("/location/{uuid}?v={reprensentation}", uuid, REPRESENTATION)
                .retrieve()
                .bodyToMono(Location.class)
                .onErrorMap(SocketTimeoutException.class,
                        e -> new AppException("hl7.fetch.location.error.timeout", e))
                .onErrorMap(ConnectException.class,
                        e -> new AppException("hl7.fetch.location.error.connect", e))
                .onErrorMap(IOException.class,
                        e -> new AppException("hl7.fetch.location.error", e))
                .block();

    }
}
