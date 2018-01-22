package no.nb.rethinkdb.networkdiscoveryclient.service;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by oddb on 22.01.18.
 */
public class NetworkDiscoveryServiceTest {

    @Test
    public void extractMasterNumberFromString() {


        long number = 999999;
        String message = NetworkDiscoveryClient.IDENTITY_STRING + number;

        assertEquals(number,NetworkDiscoveryService.extractMasterNumberFromString(message));
    }
}