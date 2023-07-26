package bootiful.clients;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import javax.sql.DataSource;
import java.net.URL;
import java.time.Instant;
import java.util.List;

@SpringBootApplication
public class ClientsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientsApplication.class, args);
    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    ApplicationRunner runner(ManualStarWarsClient manual, AutoStarWarsClient auto, JdbcClient jdbcClient) {
        return args -> {
            RowMapper<Planet> planetRowMapper = (rs, rowNum) -> new Planet(rs.getString("name"), 0, 0, null, null, null, null, null, List.of(),
                    new String[0],  rs.getTimestamp("created").toInstant() , null, null);
//            var kh = new GeneratedKeyHolder(List.of(Map.of("id", Integer.class)));
            var planet = manual.planets(4);
            var updated = jdbcClient
                    .sql("insert into planet (name, created ) values(?, ?)")
                    .params(List.of(planet.name()))
                    .update( );
            Assert.state(updated > 0, "there should have been one or more records updated");
            jdbcClient.sql("select * from planet" )
                    .query(planetRowMapper)
                    .list()
                    .forEach(System.out::println);
        };
    }

    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    ManualStarWarsClient manual(RestClient client) {
        return new ManualStarWarsClient(client);
    }

    @Bean
    AutoStarWarsClient auto(RestClient client) {
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(AutoStarWarsClient.class);
    }
}

record Planet(String name, int rotationPeriod, int orbitalPeriod, String climate, String gravity,
              String terrain, String surfaceWater, String population, List<Object> residents, String[] films,
              Instant created, Instant edited, URL url) {
}

class ManualStarWarsClient {

    private final RestClient restClient;

    ManualStarWarsClient(RestClient restClient) {
        this.restClient = restClient;
    }

    Planet planets(int id) {
        ResponseEntity<Planet> re = this.restClient
                .get()
                .uri("https://swapi.dev/api/planets/{planetId}/?format=json", id)
                .retrieve()
                .toEntity(Planet.class);
        Assert.state(re.getStatusCode().is2xxSuccessful(), "the request was not successful");
        return re.getBody();
    }
}

interface AutoStarWarsClient {

    @GetExchange("https://swapi.dev/api/planets/{planetId}/?format=json")
    Planet planets(@PathVariable("planetId") int id);
}