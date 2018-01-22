package no.nb.rethinkdb.networkdiscoveryclient.service;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by oddb on 22.01.18.
 */
public class NetworkInterfaceResolver {

    public static List<InetAddress> resolveAddresses() throws SocketException {

        List<InetAddress> addresses = new ArrayList<>();

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue; // Don't want to broadcast to the loopback interface
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();
                if (broadcast == null) {
                    continue;
                }

                addresses.add(interfaceAddress.getAddress());
            }
        }

        return addresses;
    }
}
