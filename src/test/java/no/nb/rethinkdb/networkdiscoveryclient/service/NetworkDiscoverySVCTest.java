package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * Created by oddb on 23.01.18.
 */
public class NetworkDiscoverySVCTest {


    @Test
    public void testShouldRemoveItem() {

        long timestamp = Calendar.getInstance().getTimeInMillis();
        long currentTimestamp = Calendar.getInstance().getTimeInMillis();

        ClientItem item = new ClientItem("whatever", timestamp);
        assertFalse(NetworkDiscoverySVC.shouldRemoveClient(item,currentTimestamp));

        ClientItem oldItem = new ClientItem("whatever", timestamp-60);
        assertTrue(NetworkDiscoverySVC.shouldRemoveClient(oldItem,currentTimestamp));

        ClientItem itemFromTheFuture = new ClientItem("whatever", timestamp+600);
        assertFalse(NetworkDiscoverySVC.shouldRemoveClient(itemFromTheFuture,currentTimestamp));

    }

}