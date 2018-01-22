package no.nb.rethinkdb.networkdiscoveryclient.model;

/**
 * Created by oddb on 22.01.18.
 */
public class ClientItem {


    private String ipAddress;
    private long timestamp;

    public ClientItem(String ipAddress, long timestamp) {
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
