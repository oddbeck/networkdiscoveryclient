package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
    private ReentrantLock lock;


    public NetworkDiscoverySVC(String broadcastIp, String ipRange) {
        lock = new ReentrantLock();

        this.broadcastIp = broadcastIp;
        this.ipRange = ipRange;
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

    private void sortServerList() {
        try {
            lock.lock();
            List<ClientItem> collect = otherClients
                .stream()
                .sorted(Comparator.comparingLong(ClientItem::getTimestamp).reversed())
                .collect(Collectors.toList());
            otherClients = collect;
        } finally {
            lock.unlock();
        }

    }

    public String getServersAsList() {

        String servers = "";

        try {
            lock.lock();
            for (ClientItem ci : otherClients) {
                servers += ci.getIpAddress() + ";";
            }
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }

        return servers;
    }

    public String getServersAsHtmlList() {

        String servers = "<h3>I am: " + myIpAddress + "</h3><br>";
        servers += "<UL>";
        try {
            lock.lock();
            for (ClientItem ci : otherClients) {
                servers += "<LI>" + ci.getIpAddress() + "</LI>";
            }
        } catch (Exception e) {

        } finally {
            if (lock.isLocked()) {
                lock.unlock();
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

        try {
            lock.lock();
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
        } catch (Exception e) {

        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        sortServerList();
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
                    try {
                        lock.lock();
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
                    } catch (Exception e) {

                    }finally {
                        if (lock.isLocked()) {
                            lock.unlock();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}