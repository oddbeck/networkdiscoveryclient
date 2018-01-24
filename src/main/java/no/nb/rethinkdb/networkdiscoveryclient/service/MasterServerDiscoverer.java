package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Optional;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster.YOU_MAY_JOIN_THE_CLUSTER;

/**
 * Created by oddb on 23.01.18.
 */
@Service
public class MasterServerDiscoverer implements Runnable {


    List<InetAddress> inetAddresses;
    private BuddiesRepository buddiesRepository;
    private int serverlistStableCount = 0;
    private int serverListCount = -1;
    private String myIpAddress = "undefined";
    private String masterIpAddress = "undefined";
    private String broadcastIp;

    @Autowired
    public MasterServerDiscoverer(BuddiesRepository buddiesRepository, MainConfig mainConfig) {
        this.buddiesRepository = buddiesRepository;
        try {
            inetAddresses = NetworkInterfaceResolver.resolveAddresses();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        for (InetAddress addr : inetAddresses) {
            if (addr.getHostAddress().contains(mainConfig.getIpRange())) {
                myIpAddress = addr.getHostAddress();
                break;
            }
        }
        System.out.println("My ipaddress is: " + myIpAddress + ", and broadcast is: " + broadcastIp);
        broadcastIp = mainConfig.getBroadcastAddr();
    }


    @Override
    public void run() {
        System.out.println("Starting the master election");

        while (!buddiesRepository.isMasterSet()) {

            if (serverlistStableCount > 3) {
                System.out.println("Stable count looks good. Let's start figuring out who's the master.");
                Optional<ClientItem> first = buddiesRepository.getMasterFromOtherClients();

                if (first.isPresent()) {
                    masterIpAddress = first.get().getIpAddress();
                    System.out.println("Master's ip-address is: " + masterIpAddress + ", and mine is: " + myIpAddress);
                    buddiesRepository.setMasterSet(true);
                    if (masterIpAddress.equalsIgnoreCase(myIpAddress)) {
                        buddiesRepository.setServerAlreadyRunning(true);
                        startPrimaryJobAndInformOthers();
                        System.out.println("I am master...");
                    } else {
                        System.out.println("I'm not master!");
                    }
                }
            } else {
                if (buddiesRepository.getOtherClients().size() == serverListCount) {
                    serverlistStableCount++;

                } else {
                    serverListCount = buddiesRepository.getOtherClients().size();
                    serverlistStableCount = 0;
                }
                System.out.println("!serverlistStablecount: " + serverlistStableCount + ", srvListcount: " + serverListCount);
            }
            try {
                Thread.sleep(NetworkPresenceBroadcaster.DISCOVERY_BROADCAST_TIME_IN_MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done with the election!");
    }

    private void startPrimaryJobAndInformOthers() {
        try {

            Process process = Runtime.getRuntime().exec("touch /master.log".split(" "));
            if (process.isAlive()) {
                NetworkPresenceBroadcaster.informOthers(YOU_MAY_JOIN_THE_CLUSTER + myIpAddress, broadcastIp);
            } else {
                System.out.println("Process is not yet alive.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
