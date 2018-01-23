package no.nb.rethinkdb.networkdiscoveryclient.service;

import java.net.*;
import java.util.Random;


/**
 * Created by oddb on 22.01.18.
 */
public class NetworkDiscoveryBroadcaster implements Runnable {

    private final long masterId;
    private boolean master = false;
    private boolean alone = false;

    private String broadcastIp;
    private String ipRange;

    public static final String IDENTITY_STRING = "rethinkDB_identityString:";
    public static final String YOU_MAY_JOIN = "rethinkDB_you_may_join:";

    public static final int DISCOVERY_BROADCAST_TIME_IN_MILISECONDS = 15000;

    private NetworkDiscoveryBroadcaster() {
        Random random = new Random();
        masterId = random.nextLong();
    }

    public NetworkDiscoveryBroadcaster(String broadcastIp, String iprange) {
        this();
        this.broadcastIp = broadcastIp;
        this.ipRange = iprange;
    }

    public static long extractMasterNumberFromString(String s) {
        String[] split = s.split(":");

        if (split.length > 1) {
            try {
                long l = Long.parseLong(split[1].trim());
                return l;
            } catch (NumberFormatException e) {
                System.out.println("formatexception: " + e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    public boolean isAlone() {
        return alone;
    }

    public long getMasterId() {
        return masterId;
    }

    public boolean isMaster() {
        return master;
    }


    public static DatagramSocket informOthers(String message, String broadcastIp) throws SocketException {
        DatagramSocket querySocket = new DatagramSocket();
        querySocket.setBroadcast(true);

        byte[] sendData = message.getBytes();

        try {
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(broadcastIp), 8888);
            querySocket.send(sendPacket);
            System.out.println(">>> I'mHere-broadcast sent to: " + broadcastIp);
        } catch (Exception e) {
            return null;
        }
        return querySocket;
    }


    public DatagramSocket informOthers() throws SocketException {
        DatagramSocket querySocket = new DatagramSocket();
        querySocket.setBroadcast(true);

        byte[] sendData = (IDENTITY_STRING + masterId).getBytes();
        informOthers(new String(sendData), broadcastIp);
        return querySocket;
    }

    @Override
    public void run() {

        while (true) {

            try {
                informOthers();
                try {
                    Thread.sleep(DISCOVERY_BROADCAST_TIME_IN_MILISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }
}
