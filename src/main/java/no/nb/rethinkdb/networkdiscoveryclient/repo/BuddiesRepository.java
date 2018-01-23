package no.nb.rethinkdb.networkdiscoveryclient.repo;

import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by oddb on 23.01.18.
 */
public class BuddiesRepository {
    private ReentrantLock lock;
    private List<ClientItem> otherClients = new ArrayList<>();
    public static final String IDENTITY_STRING = "rethinkDB_identityString:";
    private static final int SERVER_MAX_LIFETIME = 60;


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

    public void addOrUpdateItem(String hostaddr) {
        try {
            lock.lock();
            boolean foundItem = false;
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


    public void cleanupOldServersFromList() {

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

    public String getServersAsHtmlList(String myIpAddress) {

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

}
