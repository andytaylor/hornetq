/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.tests.integration.cluster.failover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.cluster.ha.HAPolicy;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.hornetq.core.server.impl.InVMNodeManager;
import org.hornetq.tests.util.ServiceTestBase;
import org.junit.Test;

public class AutomaticColocatedQuorumVoteTest extends ServiceTestBase
{
   @Test
   public void testSimpleDistributionOfBackups() throws Exception
   {
      TransportConfiguration liveConnector0 = getConnectorTransportConfiguration(0);
      TransportConfiguration liveConnector1 = getConnectorTransportConfiguration(1);
      TransportConfiguration liveAcceptor0 = getAcceptorTransportConfiguration(0);
      TransportConfiguration liveAcceptor1 = getAcceptorTransportConfiguration(1);
      Configuration liveConfiguration0 = getConfiguration(liveConnector0, liveAcceptor0, liveConnector1);
      HornetQServer server0 = new HornetQServerImpl(liveConfiguration0);
      server0.start();
      Configuration liveConfiguration1 = getConfiguration(liveConnector1, liveAcceptor1, liveConnector0);
      HornetQServer server1 = new HornetQServerImpl(liveConfiguration1);
      server1.start();
      Thread.sleep(30000);
   }

   private Configuration getConfiguration(TransportConfiguration liveConnector, TransportConfiguration liveAcceptor, TransportConfiguration... otherLiveNodes) throws Exception
   {
      String liveNode = (String) liveConnector.getParams().get("server-id");
      Configuration configuration = createDefaultConfig();
      configuration.getAcceptorConfigurations().clear();
      configuration.getAcceptorConfigurations().add(liveAcceptor);
      configuration.getConnectorConfigurations().put(liveConnector.getName(), liveConnector);
      configuration.setSharedStore(false);
      configuration.setJournalDirectory(configuration.getJournalDirectory() + liveNode);
      configuration.setBindingsDirectory(configuration.getBindingsDirectory() + liveNode);
      configuration.setLargeMessagesDirectory(configuration.getLargeMessagesDirectory() + liveNode);
      configuration.setPagingDirectory(configuration.getPagingDirectory() + liveNode);
      List<String> transportConfigurationList = new ArrayList<>();
      for (TransportConfiguration otherLiveNode : otherLiveNodes)
      {
         configuration.getConnectorConfigurations().put(otherLiveNode.getName(), otherLiveNode);
         transportConfigurationList.add(otherLiveNode.getName());
      }
      basicClusterConnectionConfig(configuration, liveConnector.getName(), transportConfigurationList);
      configuration.getQueueConfigurations().add(new CoreQueueConfiguration("jms.queue.testQueue", "jms.queue.testQueue", null, true));

      final HAPolicy haPolicy = new HAPolicy();
      haPolicy.setAllowBackupRequests(true);
      haPolicy.setBackupPortOffset(100);
      haPolicy.setBackupRequestRetries(-1);
      haPolicy.setBackupRequestRetryInterval(500);
      haPolicy.setMaxBackups(1);
      haPolicy.setRequestBackup(true);
      haPolicy.setAllowBackupRequests(true);
      configuration.setHAPolicy(haPolicy);

      return configuration;
   }

   private TransportConfiguration getAcceptorTransportConfiguration(int node)
   {
      HashMap<String, Object> params = new HashMap<>();
      params.put("port", "" + 5445 + node);
      return new TransportConfiguration(NETTY_ACCEPTOR_FACTORY, params);
   }

   private TransportConfiguration getConnectorTransportConfiguration(int node)
   {
      HashMap<String, Object> params = new HashMap<>();
      params.put("port", "" + 5445 + node);
      return new TransportConfiguration(NETTY_CONNECTOR_FACTORY, params);
   }
}
