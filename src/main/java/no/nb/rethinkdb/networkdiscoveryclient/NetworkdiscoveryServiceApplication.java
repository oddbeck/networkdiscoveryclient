package no.nb.rethinkdb.networkdiscoveryclient;

import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryService;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NetworkdiscoveryServiceApplication {

    public static void main(String[] args) {

        String broadcastIp = "192.168.229.255";
        String ipRange = "192.168.229.";

        NetworkDiscoveryService service = new NetworkDiscoveryService();
        service.queryOthers();
//        Thread discService = new Thread(service);
//        discService.start();

        //SpringApplication.run(NetworkdiscoveryclientApplication.class, args);

        System.out.println("Is alone: " + service.isAlone());
    }
}
