package no.nb.rethinkdb.networkdiscoveryclient.service;

import org.junit.Test;

import static no.nb.rethinkdb.networkdiscoveryclient.service.NetworkPresenceBroadcaster.IDENTITY_STRING;
import static org.junit.Assert.*;

/**
 * Created by oddb on 22.01.18.
 */
public class NetworkPresenceBroadcasterTest {

    @Test
    public void extractMasterNumberFromString() {


        long number = 999999;
        String message = IDENTITY_STRING + number;

        assertEquals(number, NetworkPresenceBroadcaster.extractMasterNumberFromString(message));


        long l = Long.parseLong("8673563963366590599");
        assertTrue(l>0);
    }
}