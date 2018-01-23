package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by oddb on 22.01.18.
 */
public class NetworkDiscoverySVC implements Runnable {

    public static final String IDENTITY_STRING = "rethinkDB_identityString:";
    private static final int SERVER_MAX_LIFETIME = 60;
    private final int BUF_SIZE = 1500;
    private List<ClientItem> otherClients = new ArrayList<>();
    private DatagramSocket c;
    private String broadcastIp;
    private String ipRange;
    private long myId;
    private String myIpAddress = "undefined";
    private DatagramSocket socket = null;
    private List<InetAddress> inetAddresses;


    public NetworkDiscoverySVC(String broadcastIp, String ipRange) {
        this.broadcastIp = broadcastIp;
        this.ipRange = ipRange;
        myId = new Random().nextLong();
        try {
            List<InetAddress> inetAddresses = NetworkInterfaceResolver.resolveAddresses();
            for (InetAddress adr: inetAddresses) {
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

    public static long getTimeInSeconds() {

        long l = Calendar.getInstance().getTimeInMillis() / 1000;
        return l;
    }

    public static boolean shouldRemoveClient(ClientItem ci, long timestamp) {

        if ((ci.getTimestamp() + SERVER_MAX_LIFETIME) < timestamp) {
            return true;
        }
        return false;
    }

    public String getServersAsList() {

        String servers = "";
        synchronized (otherClients) {
            for (ClientItem ci : otherClients) {
                servers += ci.getIpAddress() + ";";
            }
        }
        return servers;
    }

    public String getServersAsHtmlList() {

        String servers = "<h3>I am: " + myIpAddress + "</h3><br>";
        servers += "<UL>";
        synchronized (otherClients) {
            for (ClientItem ci : otherClients) {
                servers += "<LI>" + ci.getIpAddress() + "</LI>";
            }
        }
        servers += "</UL>";
        return servers;
    }

    private boolean requestIsMe(String address) {
        for (InetAddress adr : inetAddresses) {
            if (adr.getHostAddress().equalsIgnoreCase(address)) {
                return true;
            }
        }

        return false;
    }

    private void cleanupOldServersFromList() {

        synchronized (otherClients) {
            long timeInMillis = getTimeInSeconds();
            List<ClientItem> removeList = new ArrayList<>();
            for (ClientItem ci : otherClients) {
                if (shouldRemoveClient(ci, timeInMillis)) {
                    removeList.add(ci);
                }
            }

            for (ClientItem ci : removeList) {
                System.out.println("Removing old dead item: " + ci.getIpAddress());
                otherClients.remove(ci);
            }

        }
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

        try {
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            cleanupOldServersFromList();
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
                    boolean foundItem = false;
                    synchronized (otherClients) {
                        for (ClientItem item : otherClients) {
                            if (item.getIpAddress().equalsIgnoreCase(hostaddr)) {
                                item.setTimestamp(getTimeInSeconds());
                                foundItem = true;
                                break;
                            }
                        }
                        if (!foundItem) {
                            ClientItem clientItem = new ClientItem(hostaddr, getTimeInSeconds());
                            otherClients.add(clientItem);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}