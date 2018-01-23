package no.nb.rethinkdb.networkdiscoveryclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class NetworkdiscoveryServiceApplication {


    public static void main(String[] args) {
//        String broadcastIp = "192.168.229.255";
//        String ipRange = "192.168.229.";
//
//        if (args.length == 2) {
//            broadcastIp = args[0];
//            ipRange = args[1];
//        }

        ConfigurableApplicationContext run = SpringApplication.run(NetworkdiscoveryServiceApplication.class, args);

    }
}
