package no.nb.rethinkdb.networkdiscoveryclient.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by oddb on 23.01.18.
 */
@RestController
public class MasterStatusController {

    private boolean masterFound = false;


    @GetMapping("/isMasterFound")
    public String foundMaster() {

        if (!masterFound) {
            return "FAILED";
        } else {
            return "OK";
        }

    }

}
