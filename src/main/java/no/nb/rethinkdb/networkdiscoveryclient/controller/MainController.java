package no.nb.rethinkdb.networkdiscoveryclient.controller;

import no.nb.rethinkdb.networkdiscoveryclient.config.MainConfig;
import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryBroadcaster;
import no.nb.rethinkdb.networkdiscoveryclient.service.NetworkDiscoveryService;
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

    private BuddiesRepository buddiesRepository;


    @Autowired
    public MainController(BuddiesRepository buddiesRepository) {
        this.buddiesRepository = buddiesRepository;
    }

    @GetMapping("/")
    public String showServers() {
        List<ClientItem> otherClients = buddiesRepository.getOtherClients();
        StringBuffer sb = new StringBuffer();
        for (ClientItem ci : otherClients) {
            sb.append(" --join " + ci.getIpAddress());
        }
        return sb.toString();
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
