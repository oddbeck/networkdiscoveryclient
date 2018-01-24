package no.nb.rethinkdb.networkdiscoveryclient;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster;
import no.nb.rethinkdb.networkdiscoveryclient.service.ClientDiscoveryService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.*;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster.IS_CLUSTER_ALREADY_RUNNING;
import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster.YES_CLUSTER_IS_ALREADY_RUNNING;
import static no.nb.rethinkdb.networkdiscoveryclient.service.ClientDiscoveryService.BUF_SIZE;

@SpringBootApplication
public class NetworkdiscoveryServiceApplication {


    public static void main(String[] args) {

        ConfigurableApplicationContext run = SpringApplication.run(NetworkdiscoveryServiceApplication.class, args);

        // get the mainConfig
        MainConfig mainConfig = run.getBean(MainConfig.class);

        // get the configured NetworkDiscoveryService:
        ClientDiscoveryService clientDiscoveryService = run.getBean(ClientDiscoveryService.class);

        // create a broadcasterservice:
        NetworkPresenceBroadcaster broadcasterService = new NetworkPresenceBroadcaster(
            mainConfig.getBroadcastAddr(),
            mainConfig.getIpRange()
        );

        DatagramSocket socket = null;
        boolean clusterIsRunning = false;
        try {
            // set up a listening socket to figure out if there are anyone else on this network
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            socket.setSoTimeout(1500); // wait timeout = 1.5 second

            // broadcast to the network, asking if anyone's here:
            NetworkPresenceBroadcaster.informOthers(
                IS_CLUSTER_ALREADY_RUNNING, mainConfig.getBroadcastAddr()
            );

            byte[] buf = new byte[BUF_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buf, BUF_SIZE);
            try {
                // try to receive a response, waiting 1.5 seconds
                socket.receive(datagramPacket);
                String data = new String(datagramPacket.getData());
                if (data.startsWith(YES_CLUSTER_IS_ALREADY_RUNNING)) {
                    clusterIsRunning = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException ignored) {
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }


        if (!clusterIsRunning) {

        }

        Thread bcListenerThread = new Thread(clientDiscoveryService);
        Thread discService = new Thread(broadcasterService);
        discService.start();
        bcListenerThread.start();

    }
}
