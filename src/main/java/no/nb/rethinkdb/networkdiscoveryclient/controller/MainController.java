package no.nb.rethinkdb.networkdiscoveryclient.controller;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryBroadcaster;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoverySVC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by oddb on 22.01.18.
 */
@RestController
public class MainController {

    private NetworkDiscoveryBroadcaster broadcasterService;
    private NetworkDiscoverySVC broadcastListenerService;
    private Thread bcListenerThread;
    private Thread discService;

    private MainConfig mainConfig;

    @Autowired
    public MainController(MainConfig mainConfig) {
        this.mainConfig = mainConfig;
        broadcasterService = new NetworkDiscoveryBroadcaster(mainConfig.getBroadcastAddr(), mainConfig.getIpRange());
        broadcastListenerService = new NetworkDiscoverySVC(mainConfig.getBroadcastAddr(), mainConfig.getIpRange());

        bcListenerThread = new Thread(broadcastListenerService);
        discService = new Thread(broadcasterService);
        discService.start();
        bcListenerThread.start();
    }

    @GetMapping("/")
    public String showServers() {
        String servers = broadcastListenerService.getServersAsList();

        return servers;
    }

    @GetMapping("/rendered")
    public String showServersRendered() {
        String servers = "<html><head></head><body>";
        servers += broadcastListenerService.getServersAsHtmlList();
        servers += "</body></html>";
        return servers;
    }
}
