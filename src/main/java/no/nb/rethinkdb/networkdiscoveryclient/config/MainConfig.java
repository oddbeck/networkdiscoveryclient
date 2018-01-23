package no.nb.rethinkdb.networkdiscoveryclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created by oddb on 22.01.18.
 */
@Configuration
public class MainConfig {

    @Value("${app.ipRange}")
    private String ipRange;
    @Value("${app.broadcastAddr}")
    private String broadcastAddr;

    public String getIpRange() {
        return ipRange;
    }


    public void setIpRange(String ipRange) {
        this.ipRange = ipRange;
    }

    public String getBroadcastAddr() {
        return broadcastAddr;
    }

    public void setBroadcastAddr(String broadcastAddr) {
        this.broadcastAddr = broadcastAddr;
    }
}
