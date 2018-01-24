package no.nb.rethinkdb.networkdiscoveryclient.repo;

import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster.DISCOVERY_BROADCAST_TIME_IN_MILISECONDS;

/**
 * Created by oddb on 23.01.18.
 */

@Repository
public class BuddiesRepository {
    public static final int SERVER_MAX_LIFETIME_MILISECONDS = DISCOVERY_BROADCAST_TIME_IN_MILISECONDS * 2;
    private ReentrantLock lock;
    private List<ClientItem> otherClients = new ArrayList<>();
    private boolean killService = false;
    private boolean serverAlreadyRunning = false;

    public boolean isServerAlreadyRunning() {
        return serverAlreadyRunning;
    }

    public void setServerAlreadyRunning(boolean serverAlreadyRunning) {
        this.serverAlreadyRunning = serverAlreadyRunning;
    }

    public BuddiesRepository() {
        this.lock = new ReentrantLock();
    }

    public static long getTimeInSeconds() {

        long l = Calendar.getInstance().getTimeInMillis() / 1000;
        return l;
    }

    public static boolean shouldRemoveClient(ClientItem ci, long timestamp) {

        if ((ci.getTimestamp() + (SERVER_MAX_LIFETIME_MILISECONDS / 1000)) < timestamp) {
            return true;
        }
        return false;
    }

    public boolean isKillService() {
        return killService;
    }

    public void setKillService(boolean killService) {
        this.killService = killService;
    }

    public List<ClientItem> getOtherClients() {
        return otherClients;
    }

    public Optional<ClientItem> getMasterFromOtherClients() {
        return
            getOtherClients()
                .stream()
                .sorted(Comparator.comparingLong(ClientItem::getId).reversed())
                .findFirst();
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

    public void addOrUpdateItem(String hostaddr, long id) {
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
                ClientItem clientItem = new ClientItem(hostaddr, getTimeInSeconds(), id);
                otherClients.add(clientItem);
            }
        } catch (Exception e) {

        } finally {
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
