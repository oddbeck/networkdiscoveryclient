package no.nb.rethinkdb.networkdiscoveryclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class NetworkdiscoveryServiceApplication {


    public static void main(String[] args) {

        ConfigurableApplicationContext run = SpringApplication.run(NetworkdiscoveryServiceApplication.class, args);

    }
}
