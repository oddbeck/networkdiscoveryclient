package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by oddb on 22.01.18.
 */
public class NetworkDiscoveryClient implements Runnable {

    public static final String IDENTITY_STRING = "rethinkDB_identityString:";

    List<ClientItem> otherClients = new ArrayList<>();
    private DatagramSocket c;
    private String broadcastIp;
    private String ipRange;
    private long myId;

    public NetworkDiscoveryClient(String broadcastIp, String ipRange) {
        this.broadcastIp = broadcastIp;
        this.ipRange = ipRange;
        myId = new Random().nextLong();
    }

    private NetworkDiscoveryClient() {

    }


    @Override
    public void run() {
        while (true) {

            try {
                List<InetAddress> inetAddresses = NetworkInterfaceResolver.resolveAddresses();

                for (InetAddress interfaceAddress : inetAddresses) {
                    if (interfaceAddress.getAddress().toString().startsWith("/" + ipRange)) {
                        try {
                            byte[] sendData = (IDENTITY_STRING + myId).getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length);
                            c.send(sendPacket);
                            byte[] recvBuf = new byte[15000];
                            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                            c.setSoTimeout(1500);
                            try {
                                c.receive(receivePacket);
                                String hostAddress = receivePacket.getAddress().getHostAddress();
                                System.out.println("--> Broadcast response from server: " + hostAddress);
                                //Check if the message is correct
                                String message = new String(receivePacket.getData()).trim();
                                if (message.equals(IDENTITY_STRING)) {
                                    boolean addNewEntry = true;
                                    for (ClientItem client : otherClients) {
                                        if (client.getIpAddress().equalsIgnoreCase(hostAddress)) {
                                            addNewEntry = false;
                                            break;
                                        }
                                    }
                                    if (addNewEntry) {
                                        otherClients.add(new ClientItem(hostAddress, Calendar.getInstance().getTimeInMillis()));
                                        System.out.println("We've found a server.");
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Found nothing.");
                            }

                        } catch (Exception e) {
                        }

                        System.out.println("<-- Request packet sent to: " + broadcastIp + "; Interface: " + interfaceAddress.getAddress().toString());
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }

            System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response

            //We have a response

            c.close();
        }
    }
}
