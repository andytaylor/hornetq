package org.hornetq.tests.integration.cluster.ha;

import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.cluster.ha.HAPolicy;
import org.hornetq.core.server.cluster.ha.HAPolicyTemplate;
import org.hornetq.tests.integration.cluster.distribution.ClusterTestBase;
import org.junit.Before;
import org.junit.Test;


public class HAAutomaticBackupSharedStore extends ClusterTestBase
{
   @Before
   public void setup() throws Exception
   {
      super.setUp();

      setupServers();

      setUpHAPolicy(0);
      setUpHAPolicy(1);
      setUpHAPolicy(2);

      setupClusterConnection("cluster0", "queues", false, 1, isNetty(), 0, 1, 2);

      setupClusterConnection("cluster1", "queues", false, 1, isNetty(), 1, 0, 2);

      setupClusterConnection("cluster2", "queues", false, 1, isNetty(), 2, 0, 1);
   }

   @Test
   public void basicDiscovery() throws Exception
   {
      startServers(0, 1, 2, 3, 4, 5);

      createQueue(3, "queues.testaddress", "queue0", null, false);
      createQueue(4, "queues.testaddress", "queue0", null, false);
      createQueue(5, "queues.testaddress", "queue0", null, false);

   }

   protected void setupServers() throws Exception
   {
      // The lives
      setupLiveServer(0, isFileStorage(), true, isNetty());
      setupLiveServer(1, isFileStorage(), true, isNetty());
      setupLiveServer(2, isFileStorage(), true, isNetty());

   }

   private void setUpHAPolicy(int node)
   {
      HornetQServer server = getServer(node);
      server.getConfiguration().setHAPolicy(HAPolicyTemplate.AUTOMATIC_SHARED_STORE.getHaPolicy());
   }

   public boolean isNetty()
   {
      return true;
   }
}
