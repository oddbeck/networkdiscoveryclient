package no.nb.rethinkdb.networkdiscoveryclient;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import no.nb.rethinkdb.networkdiscoveryclient.service.ClientDiscoveryService;
import no.nb.rethinkdb.networkdiscoveryclient.service.MasterServerDiscoverer;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.*;

import static no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository.getTimeInSeconds;
import static no.nb.rethinkdb.networkdiscoveryclient.service.ClientDiscoveryService.BUF_SIZE;
import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster.*;

@SpringBootApplication
public class NetworkdiscoveryServiceApplication {


    public static final int EXTRA_WAIT_TIME_IN_MILLISECONDS = 5000;

    public static void main(String[] args) {

        ConfigurableApplicationContext run = SpringApplication.run(NetworkdiscoveryServiceApplication.class, args);


        BuddiesRepository buddiesRepository = run.getBean(BuddiesRepository.class);
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
            socket.setSoTimeout(2500); // wait timeout = 2.5 second

            // broadcast to the network, asking if anyone's here:
            System.out.println("Are there anyone here?");
            NetworkPresenceBroadcaster.informOthers(
                IS_CLUSTER_ALREADY_RUNNING, mainConfig.getBroadcastAddr()
            );

            byte[] buf = new byte[BUF_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buf, BUF_SIZE);
            try {
                // try to receive a response. We're willing to wait a few seconds before
                // we join. We skip discovery broadcasts, and only look for join-messages.
                // if we won't get any join messages we join the election process.
                long timeInSeconds = getTimeInSeconds() + (DISCOVERY_BROADCAST_TIME_IN_MILLISECONDS/1000);
                while (timeInSeconds > getTimeInSeconds()) {
                    socket.receive(datagramPacket);
                    String data = new String(datagramPacket.getData());
                    System.out.println("Got a reply: " + data.trim());
                    if (data.startsWith(YOU_MAY_JOIN_THE_CLUSTER)) {
                        clusterIsRunning = true;
                        // we wait a while to build up a list of servers.
                        Thread.sleep(DISCOVERY_BROADCAST_TIME_IN_MILLISECONDS + EXTRA_WAIT_TIME_IN_MILLISECONDS);
                    }
                }
            } catch (IOException | InterruptedException e) {
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
            MasterServerDiscoverer masterServerDiscoverer = run.getBean(MasterServerDiscoverer.class);
            Thread thread = new Thread(masterServerDiscoverer);
            thread.start();
        } else {
            buddiesRepository.setMasterSet(true);
            System.out.println("Found others, don't need a master election.");
        }

        Thread bcListenerThread = new Thread(clientDiscoveryService);
        Thread discService = new Thread(broadcasterService);
        discService.start();
        bcListenerThread.start();

    }
}
