package no.nb.rethinkdb.networkdiscoveryclient.service;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by oddb on 22.01.18.
 */
public class NetworkDiscoveryBroadcasterTest {

    @Test
    public void extractMasterNumberFromString() {


        long number = 999999;
        String message = NetworkDiscoverySVC.IDENTITY_STRING + number;

        assertEquals(number, NetworkDiscoveryBroadcaster.extractMasterNumberFromString(message));
    }
}