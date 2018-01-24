package no.nb.rethinkdb.networkdiscoveryclient;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryBroadcaster;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.*;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryBroadcaster.IS_CLUSTER_ALREADY_RUNNING;
import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryBroadcaster.YES_CLUSTER_IS_ALREADY_RUNNING;
import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryService.BUF_SIZE;

@SpringBootApplication
public class NetworkdiscoveryServiceApplication {


    public static void main(String[] args) {

        ConfigurableApplicationContext run = SpringApplication.run(NetworkdiscoveryServiceApplication.class, args);
        MainConfig mainConfig = run.getBean(MainConfig.class);


        NetworkDiscoveryService networkDiscoveryService = run.getBean(NetworkDiscoveryService.class);

        NetworkDiscoveryBroadcaster broadcasterService = new NetworkDiscoveryBroadcaster(
            mainConfig.getBroadcastAddr(),
            mainConfig.getIpRange()
        );

        DatagramSocket socket = null;
        boolean clusterIsRunning = false;
        try {
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            socket.setSoTimeout(1500);
            NetworkDiscoveryBroadcaster.informOthers(
                IS_CLUSTER_ALREADY_RUNNING, mainConfig.getBroadcastAddr()
            );
            byte[] buf = new byte[BUF_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buf, BUF_SIZE);
            try {
                socket.receive(datagramPacket);
                String data = new String(datagramPacket.getData());
                if (data.startsWith(YES_CLUSTER_IS_ALREADY_RUNNING)) {
                    clusterIsRunning = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException | UnknownHostException e) {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }


        if (!clusterIsRunning) {

        }

        Thread bcListenerThread = new Thread(networkDiscoveryService);
        Thread discService = new Thread(broadcasterService);
        discService.start();
        bcListenerThread.start();

    }

    @Bean
    public void argh(MainConfig mainConfig, NetworkDiscoveryService networkDiscoveryService) {

    }

}
