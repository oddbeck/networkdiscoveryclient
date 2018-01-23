package no.nb.rethinkdb.networkdiscoveryclient.model;

/**
 * Created by oddb on 22.01.18.
 */
public class ClientItem {


    private String ipAddress;
    private long timestamp;
    private long id;

    public ClientItem(String ipAddress, long timestamp, long id) {
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
