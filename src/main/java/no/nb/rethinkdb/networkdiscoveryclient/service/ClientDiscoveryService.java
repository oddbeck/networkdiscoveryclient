package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster.*;

/**
 * Created by oddb on 22.01.18.
 */
@Service
public class ClientDiscoveryService implements Runnable, AutoCloseable {

    public final static int BUF_SIZE = 1500;
    private String ipRange;
    private long myId;
    private String myIpAddress = "undefined";
    private List<InetAddress> inetAddresses;
    private BuddiesRepository buddiesRepository;
    private boolean runForever = true;
    private MainConfig mainConfig;
    private boolean joinDirectly = false;

    @Autowired
    public ClientDiscoveryService(MainConfig mainConfig, BuddiesRepository repository) {

        this.mainConfig = mainConfig;
        buddiesRepository = repository;
        this.ipRange = mainConfig.getIpRange();
        myId = new Random().nextLong();
        try {
            List<InetAddress> inetAddresses = NetworkInterfaceResolver.resolveAddresses();
            for (InetAddress adr : inetAddresses) {
                if (adr.getHostAddress().contains(ipRange)) {
                    myIpAddress = adr.getHostAddress();
                    break;
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("I am: " + myIpAddress);
    }

    private ClientDiscoveryService() {

    }

    public boolean isJoinDirectly() {
        return joinDirectly;
    }

    public void setJoinDirectly(boolean joinDirectly) {
        this.joinDirectly = joinDirectly;
    }

    @Override
    public void run() {

        boolean foundMatchingInterface = false;
        try {
            inetAddresses = NetworkInterfaceResolver.resolveAddresses();
            for (InetAddress inetAddress : inetAddresses) {
                if (inetAddress.getHostAddress().startsWith(ipRange)) {
                    foundMatchingInterface = true;
                    break;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        if (!foundMatchingInterface) {
            throw new RuntimeException("Unable to find a matching interface for iprange: " + ipRange);
        }

        DatagramSocket socket;
        try {
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }

        while (runForever) {

            buddiesRepository.cleanupOldServersFromList();

            byte[] buf = new byte[BUF_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buf, BUF_SIZE);

            // figure out what message is sent on the network:
            try {
                socket.receive(datagramPacket);
                String data = new String(datagramPacket.getData());

                // is this a 'newcomter' query?
                if (data.startsWith(IS_CLUSTER_ALREADY_RUNNING)) {
                    System.out.println("Telling the newcomer that we're here.");
                    NetworkPresenceBroadcaster.informOthers(YES_CLUSTER_IS_ALREADY_RUNNING, mainConfig.getBroadcastAddr());
                    continue;
                }
                // is this a regular identity broadcast?
                if (data.startsWith(IDENTITY_STRING)) {
                    String hostaddr = datagramPacket.getAddress().getHostAddress();
                    long id = NetworkPresenceBroadcaster.extractMasterNumberFromString(data);
                    buddiesRepository.addOrUpdateItem(hostaddr, id);
                    if (joinDirectly) {
                        joinDirectly = false;
                        System.out.println("Joining directly, not waiting for a join command.");
                        startSlaveJobsAndSetAlreadyRunning();
                    }
                    continue;
                }
                // is this a message from the 'master' telling us to join him?
                if (!buddiesRepository.isServerAlreadyRunning() && data.startsWith(YOU_MAY_JOIN)) {
                    if (buddiesRepository.getMasterFromOtherClients().isPresent()) {
                        ClientItem masterClient = buddiesRepository.getMasterFromOtherClients().get();
                        if (!masterClient.getIpAddress().equalsIgnoreCase(myIpAddress)) {
                            System.out.println("I may join...my id is: " + myId + ", and master is : " + masterClient.getId());
                            startSlaveJobsAndSetAlreadyRunning();
                        }
                    } else {
                        System.out.println("I am master, so I don't care to join. I've started my own process.");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void startSlaveJobsAndSetAlreadyRunning() throws IOException {
        startSlaveJobs();
        buddiesRepository.setServerAlreadyRunning(true);
    }

    private void startSlaveJobs() throws IOException {
        Process exec = Runtime.getRuntime().exec("touch /slave.log".split(" "));
        if (exec.isAlive()) {
            System.out.println("I've joined");
        } else {
            System.out.println("I failed to join...");
        }
    }

    @Override
    public void close() {
        runForever = false;
    }
}