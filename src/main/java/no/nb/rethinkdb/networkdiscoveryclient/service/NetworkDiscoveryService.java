package no.nb.rethinkdb.networkdiscoveryclient.service;

import java.io.IOException;
import java.net.*;
import java.util.Random;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryClient.IDENTITY_STRING;

/**
 * Created by oddb on 22.01.18.
 */
public class NetworkDiscoveryService implements Runnable {

    private final long masterId;
    private boolean master = false;
    private boolean alone = false;

    private String broadcastIp;

    public NetworkDiscoveryService() {
        Random random = new Random();
        masterId = random.nextLong();
    }

    public static long extractMasterNumberFromString(String s) {
        String[] split = s.split(":");
        if (split.length > 1) {
            try {
                long l = Long.parseLong(split[1]);
                return l;
            } catch (NumberFormatException e) {
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

    public void queryOthers() {
        try {

            // look for buddies
            DatagramSocket querySocket = new DatagramSocket();
            querySocket.setBroadcast(true);
            querySocket.setSoTimeout(1500);

            byte[] sendData = (IDENTITY_STRING + masterId).getBytes();

            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(broadcastIp), 8888);
                querySocket.send(sendPacket);
                System.out.println(">>> Request packet sent to: " + broadcastIp);
            } catch (Exception e) {
                return;
            }

            // prepare handling a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            try {
                // receive it
                querySocket.receive(receivePacket);

                // if here, it means we found a response
                String message = new String(receivePacket.getData()).trim();

                if (message.startsWith(IDENTITY_STRING)) {
                    String[] split = message.split(":");
                    if (split.length > 1) {
                        try {
                            long l = extractMasterNumberFromString(message);
                            if (l > masterId) {
                                master = false;
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                alone = true;
                System.out.println("We're alone.");
            }
        } catch (Exception e) {
            master = true;
        }
    }

    @Override
    public void run() {

        try {
            DatagramSocket listenerSocket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            listenerSocket.setBroadcast(true);

            while (true) {
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                listenerSocket.setSoTimeout(1500);
                try {
                    listenerSocket.receive(packet);
                } catch (IOException e) {
                    master = true;
                    continue;
                }

                //Packet received
                System.out.println(">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                System.out.println(">>>Packet received; data: " + new String(packet.getData()));

                //See if the packet holds the right command (message)
                String message = new String(packet.getData()).trim();
                if (message.startsWith(IDENTITY_STRING)) {
                    byte[] sendData = (IDENTITY_STRING + getMasterId()).getBytes();

                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    listenerSocket.send(sendPacket);

                    System.out.println(getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
