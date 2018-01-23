package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryBroadcaster.YOU_MAY_JOIN;

/**
 * Created by oddb on 23.01.18.
 */
@Service
public class MasterServerDiscoverer implements Runnable {


    boolean masterIsDefined = false;
    List<InetAddress> inetAddresses;
    private BuddiesRepository buddiesRepository;
    private int serverlistStableCount = 0;
    private int serverListCount = -1;
    private Thread runner;
    private String myIpAddress = "undefined";
    private String masterIpAddress = "undefined";
    private String broadcastIp = "undefined";

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
        broadcastIp = mainConfig.getBroadcastAddr();
    }


    @Override
    public void run() {

        while (!masterIsDefined) {

            if (serverlistStableCount > 3) {
                Optional<ClientItem> first = buddiesRepository
                    .getOtherClients()
                    .stream()
                    .sorted(Comparator.comparingLong(ClientItem::getId).reversed())
                    .findFirst();
                if (first.isPresent()) {
                    masterIpAddress = first.get().getIpAddress();
                    if (masterIpAddress.equalsIgnoreCase(myIpAddress)) {
                        //TODO: Inform the others that I'm the boss.
                        try {
                            NetworkDiscoveryBroadcaster.informOthers(YOU_MAY_JOIN + myIpAddress, broadcastIp);
                        } catch (SocketException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                if (buddiesRepository.getOtherClients().size() == serverListCount) {
                    serverlistStableCount++;

                } else {
                    serverListCount = buddiesRepository.getOtherClients().size();
                    serverlistStableCount = 0;
                }
                System.out.println("serverlistStablecount: " + serverlistStableCount + ", srvListcount: " + serverListCount);
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
