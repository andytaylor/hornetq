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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.client.impl.Topology;
import org.hornetq.core.client.impl.TopologyMemberImpl;
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
      TransportConfiguration liveConnector0 = getConnectorTransportConfiguration("liveConnector0", 0);
      TransportConfiguration liveConnector1 = getConnectorTransportConfiguration("liveConnector1", 1);
      TransportConfiguration remoteConnector0 = getConnectorTransportConfiguration("remoteConnector0", 1);
      TransportConfiguration remoteConnector1 = getConnectorTransportConfiguration("remoteConnector1", 0);
      TransportConfiguration liveAcceptor0 = getAcceptorTransportConfiguration(0);
      TransportConfiguration liveAcceptor1 = getAcceptorTransportConfiguration(1);
      Configuration liveConfiguration0 = getConfiguration("server0", liveConnector0, liveAcceptor0, remoteConnector0);
      HornetQServer server0 = new HornetQServerImpl(liveConfiguration0);
      server0.setIdentity("server0");
      Configuration liveConfiguration1 = getConfiguration("server1", liveConnector1, liveAcceptor1, remoteConnector1);
      HornetQServer server1 = new HornetQServerImpl(liveConfiguration1);
      server1.setIdentity("server1");

      try
      (
         ServerLocator serverLocator = HornetQClient.createServerLocatorWithoutHA(liveConnector0)
      )
      {
         server0.start();
         server1.start();
         ClientSessionFactory sessionFactory0 = serverLocator.createSessionFactory(liveConnector0);
         waitForRemoteBackup(sessionFactory0, 10);
         ClientSessionFactory sessionFactory1 = serverLocator.createSessionFactory(liveConnector1);
         waitForRemoteBackup(sessionFactory1, 10);
         Topology topology = serverLocator.getTopology();
         Collection<TopologyMemberImpl> members = topology.getMembers();
         assertEquals(members.size(), 2);
         Map<String,HornetQServer> backupServers0 = server0.getHAManager().getBackupServers();
         assertEquals(backupServers0.size(), 1);
         Map<String,HornetQServer> backupServers1 = server1.getHAManager().getBackupServers();
         assertEquals(backupServers1.size(), 1);
         HornetQServer backupServer0 = backupServers0.values().iterator().next();
         HornetQServer backupServer1 = backupServers1.values().iterator().next();
         waitForRemoteBackupSynchronization(backupServer0);
         waitForRemoteBackupSynchronization(backupServer1);
         assertEquals(server0.getNodeID(), backupServer1.getNodeID());
         assertEquals(server1.getNodeID(), backupServer0.getNodeID());
      }
      finally
      {
         server0.stop();
         server1.stop();
      }
   }

   private Configuration getConfiguration(String identity, TransportConfiguration liveConnector, TransportConfiguration liveAcceptor, TransportConfiguration... otherLiveNodes) throws Exception
   {
      Configuration configuration = createDefaultConfig();
      configuration.getAcceptorConfigurations().clear();
      configuration.getAcceptorConfigurations().add(liveAcceptor);
      configuration.getConnectorConfigurations().put(liveConnector.getName(), liveConnector);
      configuration.setSharedStore(false);
      configuration.setJournalDirectory(configuration.getJournalDirectory() + identity);
      configuration.setBindingsDirectory(configuration.getBindingsDirectory() + identity);
      configuration.setLargeMessagesDirectory(configuration.getLargeMessagesDirectory() + identity);
      configuration.setPagingDirectory(configuration.getPagingDirectory() + identity);
      List<String> transportConfigurationList = new ArrayList<>();
      final HAPolicy haPolicy = new HAPolicy();
      for (TransportConfiguration otherLiveNode : otherLiveNodes)
      {
         configuration.getConnectorConfigurations().put(otherLiveNode.getName(), otherLiveNode);
         transportConfigurationList.add(otherLiveNode.getName());
         haPolicy.getRemoteConnectors().add(otherLiveNode.getName());
      }
      basicClusterConnectionConfig(configuration, liveConnector.getName(), transportConfigurationList);
      configuration.getQueueConfigurations().add(new CoreQueueConfiguration("jms.queue.testQueue", "jms.queue.testQueue", null, true));

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
      params.put("port", "" + (5445 + node));
      return new TransportConfiguration(NETTY_ACCEPTOR_FACTORY, params);
   }

   private TransportConfiguration getConnectorTransportConfiguration(String name, int node)
   {
      HashMap<String, Object> params = new HashMap<>();
      params.put("port", "" + (5445 + node));
      return new TransportConfiguration(NETTY_CONNECTOR_FACTORY, params, name);
   }
}
