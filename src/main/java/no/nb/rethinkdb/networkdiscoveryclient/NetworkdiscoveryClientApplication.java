package no.nb.rethinkdb.networkdiscoveryclient;

import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryService;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NetworkdiscoveryClientApplication {

	public static void main(String[] args) {

		String broadcastIp = "192.168.229.255";
		String ipRange = "192.168.229.";

		NetworkDiscoveryService service = new NetworkDiscoveryService();
		service.queryOthers();

		if (service.isAlone()) {
			System.out.println("I'm alone");
		} else {
			System.out.println("I've found buddies.");
		}


		System.out.println("client is done");

		//SpringApplication.run(NetworkdiscoveryclientApplication.class, args);
	}
}
