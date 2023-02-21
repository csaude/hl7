package mz.org.fgh.hl7.service;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

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

    public List<Location> findAllProvinces() {
        return webClient.get()
                .uri("/location?tag={tag}&v={representation}", PROVINCE_TAG, REPRESENTATION)
                .retrieve()
                .bodyToMono(LocationSearch.class)
                .map(LocationSearch::getResults)
                .block();
    }

    public Location findByUuid(String uuid) {
        return webClient.get()
                .uri("/location/{uuid}?v={reprensentation}", uuid, REPRESENTATION)
                .retrieve()
                .bodyToMono(Location.class)
                .block();
    }
}
