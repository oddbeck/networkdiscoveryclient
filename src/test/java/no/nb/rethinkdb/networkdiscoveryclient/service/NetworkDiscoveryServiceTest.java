package no.nb.rethinkdb.networkdiscoveryclient.service;

import no.nb.rethinkdb.networkdiscoveryclient.model.ClientItem;
import no.nb.rethinkdb.networkdiscoveryclient.repo.BuddiesRepository;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * Created by oddb on 23.01.18.
 */
public class NetworkDiscoveryServiceTest {


    @Test
    public void testShouldRemoveItem() {

        long timestamp = Calendar.getInstance().getTimeInMillis();
        long currentTimestamp = Calendar.getInstance().getTimeInMillis();

        ClientItem item = new ClientItem("whatever", timestamp, 10);
        assertFalse(BuddiesRepository.shouldRemoveClient(item,currentTimestamp));

        ClientItem oldItem = new ClientItem("whatever", timestamp-60,10);
        assertTrue(BuddiesRepository.shouldRemoveClient(oldItem,currentTimestamp));

        ClientItem itemFromTheFuture = new ClientItem("whatever", timestamp+600,10);
        assertFalse(BuddiesRepository.shouldRemoveClient(itemFromTheFuture,currentTimestamp));

    }

}