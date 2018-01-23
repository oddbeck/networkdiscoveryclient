package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Created by oddb on 22.01.18.
 */
@Service
public class NetworkDiscoverySVC implements Runnable, AutoCloseable {

    public static final String IDENTITY_STRING = "rethinkDB_identityString:";
    private final int BUF_SIZE = 1500;
    private String ipRange;
    private long myId;
    private String myIpAddress = "undefined";
    private List<InetAddress> inetAddresses;
    private BuddiesRepository buddiesRepository;
    private boolean runForever = true;

    @Autowired
    public NetworkDiscoverySVC(MainConfig mainConfig , BuddiesRepository repository) {

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

    private NetworkDiscoverySVC() {

    }

    /*
    Når antallet pods/servere er stabilt så blir de enige om hvem som er master vha høyest ID, og så
    starter han rethinkdb-tjenesten sin først og venter på at den er oppe, så får de andre beskjed
    om å starte sine ved å bruke 'join'-parameteren.
     */

    private boolean requestIsMe(String address) {
        for (InetAddress adr : inetAddresses) {
            if (adr.getHostAddress().equalsIgnoreCase(address)) {
                return true;
            }
        }

        return false;
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
            try {
                socket.receive(datagramPacket);
                String data = new String(datagramPacket.getData());
                if (data.startsWith(IDENTITY_STRING)) {
                    String hostaddr = datagramPacket.getAddress().getHostAddress();
                    if (requestIsMe(hostaddr)) {
                        continue;
                    }
                    long id = NetworkDiscoveryBroadcaster.extractMasterNumberFromString(data);
                    buddiesRepository.addOrUpdateItem(hostaddr,id);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        runForever = false;
    }
}