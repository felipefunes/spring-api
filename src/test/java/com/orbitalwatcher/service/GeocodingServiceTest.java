package com.orbitalwatcher.service;

import com.orbitalwatcher.dto.LocationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeocodingServiceTest {

    @Test
    void returnsInternationalWatersWhenNoAddressIsFound() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://nominatim.openstreetmap.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(method(GET))
                .andRespond(withSuccess("{\"error\":\"Unable to geocode\"}", MediaType.APPLICATION_JSON));

        GeocodingService service = new GeocodingService(restClient);
        LocationInfo location = service.reverseGeocode(0.0, -140.0);

        assertThat(location.name()).isEqualTo("International Waters");
        assertThat(location.isOverOcean()).isTrue();

        server.verify();
    }

    @Test
    void returnsResolvedLocationWhenAddressIsFound() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://nominatim.openstreetmap.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        server.expect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "address": {
                            "city": "Paris",
                            "country": "France"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        GeocodingService service = new GeocodingService(restClient);
        LocationInfo location = service.reverseGeocode(48.8566, 2.3522);

        assertThat(location.name()).isEqualTo("Paris");
        assertThat(location.country()).isEqualTo("France");
        assertThat(location.isOverOcean()).isFalse();

        server.verify();
    }
}
