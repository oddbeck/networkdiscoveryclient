package no.nb.rethinkdb.networkdiscoveryclient.controller;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryBroadcaster;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoverySVC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by oddb on 22.01.18.
 */
@RestController
public class MainController {

    private NetworkDiscoveryBroadcaster broadcasterService;
    private NetworkDiscoverySVC broadcastListenerService;
    private Thread bcListenerThread;
    private Thread discService;
    private BuddiesRepository buddiesRepository;



    private MainConfig mainConfig;

    @Autowired
    public MainController(MainConfig mainConfig, NetworkDiscoverySVC networkDiscoverySVC, BuddiesRepository buddiesRepository) {
        this.mainConfig = mainConfig;
        this.buddiesRepository = buddiesRepository;
        this.broadcasterService = new NetworkDiscoveryBroadcaster(mainConfig.getBroadcastAddr(), mainConfig.getIpRange());
        this.broadcastListenerService = networkDiscoverySVC;

        this.bcListenerThread = new Thread(broadcastListenerService);
        this.discService = new Thread(broadcasterService);
        this.discService.start();
        this.bcListenerThread.start();
    }

    @GetMapping("/")
    public String showServers() {
        String servers = buddiesRepository.getServersAsList();

        return servers;
    }

    @GetMapping("/rendered2")
    public String showServersRendered(HttpServletRequest req) {
        String servers = "<html><head></head><body>";
        servers += buddiesRepository.getServersAsHtmlList(req.getLocalAddr());
        servers += "</body></html>";
        return servers;
    }

    @GetMapping("/rendered")
    @ResponseBody
    public List<ClientItem> getAsJson() {
        return buddiesRepository.getOtherClients();
    }
}
