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
package org.hornetq.core.server.cluster.ha;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.ClusterTopologyListener;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.api.core.client.TopologyMember;
import org.hornetq.core.client.impl.TopologyMemberImpl;
import org.hornetq.core.config.BackupStrategy;
import org.hornetq.core.config.ClusterConnectionConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.protocol.core.Channel;
import org.hornetq.core.protocol.core.CoreRemotingConnection;
import org.hornetq.core.protocol.core.Packet;
import org.hornetq.core.protocol.core.impl.ChannelImpl;
import org.hornetq.core.protocol.core.impl.PacketImpl;
import org.hornetq.core.protocol.core.impl.wireformat.BackupRequestMessage;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.cluster.ClusterConnection;
import org.hornetq.core.server.cluster.qourum.QuorumVote;
import org.hornetq.core.server.cluster.qourum.QuorumVoteHandler;
import org.hornetq.core.server.cluster.qourum.Vote;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.hornetq.spi.core.security.HornetQSecurityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HAManager implements ClusterTopologyListener
{
   public static final SimpleString REQUEST_BACKUP_QUORUM_VOTE = new SimpleString("RequestBackupQuorumVote");

   private final HAPolicy haPolicy;

   private final HornetQSecurityManager securityManager;

   private final  HornetQServerImpl server;

   private Set<Configuration> backupServerConfigurations;

   private Map<String, HornetQServer> backupServers = new HashMap<>();

   public HAManager(HAPolicy haPolicy, HornetQSecurityManager securityManager, HornetQServerImpl hornetQServer, Set<Configuration> backupServerConfigurations)
   {
      this.haPolicy = haPolicy;
      this.securityManager = securityManager;
      server = hornetQServer;
      this.backupServerConfigurations = backupServerConfigurations;
   }

   public void start()
   {
      server.getClusterManager().getQuorumManager().registerQuorumHandler(new RequestBackupQuorumVoteHandler());
      if (backupServerConfigurations != null)
      {
         for (Configuration configuration : backupServerConfigurations)
         {
            HornetQServer backup = new HornetQServerImpl(configuration, null, securityManager, server);
            backupServers.put(configuration.getName(), backup);
         }
      }

      for (HornetQServer hornetQServer : backupServers.values())
      {
         try
         {
            hornetQServer.start();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }

      if (haPolicy.isRequestBackup())
      {
         server.getClusterManager().getQuorumManager().vote(new RequestBackupQuorumVote());
      }
   }

   public void stopAllBackups()
   {
      for (HornetQServer hornetQServer : backupServers.values())
      {
         try
         {
            hornetQServer.stop();
         }
         catch (Exception e)
         {
            e.printStackTrace();
            //todo
         }
      }
      backupServers.clear();
   }

   private void requestBackup(Pair<TransportConfiguration, TransportConfiguration> connectorPair, int backupSize) throws Exception
   {
      ServerLocator locator = HornetQClient.createServerLocatorWithoutHA(connectorPair.getA());
      ClientSessionFactory sessionFactory = locator.createSessionFactory();
      CoreRemotingConnection connection = (CoreRemotingConnection) sessionFactory.getConnection();
      Channel channel = connection.getChannel(ChannelImpl.CHANNEL_ID.PING.id, -1);
      BackupRequestMessage backupRequestMessage;
      if (haPolicy.getBackupType() == HAPolicy.BACKUP_TYPE.SHARED_STORE)
      {
         backupRequestMessage = new BackupRequestMessage(backupSize,
                                                         server.getConfiguration().getJournalDirectory(),
                                                         server.getConfiguration().getBindingsDirectory(),
                                                         server.getConfiguration().getLargeMessagesDirectory(),
                                                         server.getConfiguration().getPagingDirectory());
      }
      else
      {
         ClusterConnection defaultConnection = server.getClusterManager().getDefaultConnection(null);
         backupRequestMessage = new BackupRequestMessage(backupSize, server.getNodeID());
      }
      Packet packet = channel.sendBlocking(backupRequestMessage, PacketImpl.BACKUP_REQUEST_RESPONSE);
   }

   @Override
   public void nodeUP(TopologyMember member, boolean last)
   {
      System.out.println("org.hornetq.core.server.cluster.ha.HAManager.nodeUP");
   }

   @Override
   public void nodeDown(long eventUID, String nodeID)
   {
      System.out.println("org.hornetq.core.server.cluster.ha.HAManager.nodeDown");
   }

   public synchronized void activateSharedStoreBackup(int backupSize, String journalDirectory, String bindingsDirectory, String largeMessagesDirectory, String pagingDirectory)
   {

   }

   public synchronized void activateReplicatedBackup(int backupSize, SimpleString nodeID) throws Exception
   {
      TopologyMemberImpl member = server.getClusterManager().getDefaultConnection(null).getTopology().getMember(nodeID.toString());
      Configuration configuration = server.getConfiguration().copy();
      int portOffset = haPolicy.getBackupPortOffset() * (backupServers.size() + 1);
      String name = "colocated_backup_" + backupServers.size() + 1;
      updateConfiguration(configuration, haPolicy.getBackupStrategy(), name, portOffset, haPolicy.getScaledownConnector());
      configuration.setSharedStore(false);
      configuration.setBackup(true);
      HornetQServer backup = new HornetQServerImpl(configuration, null, securityManager, server);
      backupServers.put(configuration.getName(), backup);
      backup.start();
   }

   public static void updateConfiguration(Configuration backupConfiguration, BackupStrategy backupStrategy, String name, int portOffset, String scaleDownConnector)
   {
      backupConfiguration.setBackupStrategy(backupStrategy);
      backupConfiguration.setName(name);
      //we only do this if we are a full server, if recover then our connectors will be the same as the parent
      if (backupConfiguration.getBackupStrategy() == BackupStrategy.FULL)
      {
         Set<TransportConfiguration> acceptors = backupConfiguration.getAcceptorConfigurations();
         for (TransportConfiguration acceptor : acceptors)
         {
            updatebackupParams(name, portOffset, acceptor.getParams());
         }
         Map<String, TransportConfiguration> connectorConfigurations = backupConfiguration.getConnectorConfigurations();
         for (Map.Entry<String, TransportConfiguration> entry : connectorConfigurations.entrySet())
         {
            updatebackupParams(name, portOffset, entry.getValue().getParams());
         }
      }
      else
      {
         //use the scale down cluster if set, this gets used when scaling down, typically invm if colocated
         if (scaleDownConnector != null)
         {
            List<ClusterConnectionConfiguration> clusterConfigurations = backupConfiguration.getClusterConfigurations();
            for (ClusterConnectionConfiguration clusterConfiguration : clusterConfigurations)
            {
               clusterConfiguration.setScaleDownConnector(scaleDownConnector);
            }
         }
      }
   }

   private static void updatebackupParams(String name, int portOffset, Map<String, Object> params)
   {
      if (params != null)
      {
         Object port = params.get("port");
         if (port != null)
         {
            Integer integer = Integer.valueOf(port.toString());
            integer += portOffset;
            params.put("port", integer.toString());
         }
         Object serverId = params.get("server-id");
         if (serverId != null)
         {
            params.put("server-id", serverId.toString() + "(" + name + ")");
         }
      }
   }

   private final class RequestBackupQuorumVoteHandler implements QuorumVoteHandler
   {
      @Override
      public Vote vote(Map<String, Object> voteParams)
      {
         return new RequestBackupVote((Long) voteParams.get("ID"), backupServers.size(), server.getNodeID().toString());
      }

      @Override
      public SimpleString getQuorumName()
      {
         return REQUEST_BACKUP_QUORUM_VOTE;
      }
   }

   private final class RequestBackupQuorumVote extends QuorumVote<Pair<String, Long>, Pair<String, Long>>
   {
      private final List<Pair<String, Long>> nodes = new ArrayList<>();

      public RequestBackupQuorumVote()
      {
         super(server.getStorageManager().generateUniqueID(), REQUEST_BACKUP_QUORUM_VOTE);
      }

      @Override
      public Vote connected()
      {
         return new RequestBackupVote(getVoteID());
      }

      @Override
      public Vote notConnected()
      {
         return new RequestBackupVote(getVoteID());
      }

      @Override
      public void vote(Vote<Pair<String, Long>> vote)
      {
         nodes.add(vote.getVote());
      }

      @Override
      public Pair<String, Long> getDecision()
      {
         Collections.sort(nodes, new Comparator<Pair<String, Long>>()
         {
            @Override
            public int compare(Pair<String, Long> o1, Pair<String, Long> o2)
            {
               return o1.getB().compareTo(o2.getB());
            }
         });
         return nodes.get(0);
      }

      @Override
      public void allVotesCast()
      {
         if (nodes.size() > 0)
         {
            Pair<String, Long> decision = getDecision();
            TopologyMemberImpl member = server.getClusterManager().getDefaultConnection(null).getTopology().getMember(decision.getA());
            try
            {
               requestBackup(member.getConnector(), decision.getB().intValue());
            }
            catch (Exception e)
            {
               e.printStackTrace();
               //todo
            }
         }
         else
         {
            nodes.clear();
            server.getScheduledPool().schedule(new Runnable()
            {
               @Override
               public void run()
               {
                  server.getClusterManager().getQuorumManager().vote(RequestBackupQuorumVote.this);
               }
            }, haPolicy.getBackupRequestRetryInterval(), TimeUnit.MILLISECONDS);
         }
      }

      @Override
      public SimpleString getName()
      {
         return REQUEST_BACKUP_QUORUM_VOTE;
      }

      @Override
      public Vote createVote(Map voteMap)
      {
         return new RequestBackupVote(voteMap);
      }

   }

   class RequestBackupVote extends Vote<Pair<String, Long>>
   {
      private long backupsSize;
      private String nodeID;

      public RequestBackupVote(long id)
      {
         super(id);
         backupsSize = -1;
      }

      public RequestBackupVote(Long id, int backupsSize, String nodeID)
      {
         super(id);
         this.backupsSize = backupsSize;
         this.nodeID = nodeID;
      }

      public RequestBackupVote(Map<String, Object> voteMap)
      {
         super((Long) voteMap.get("ID"));
         backupsSize = (Long) voteMap.get("BACKUP_SIZE");
         nodeID = (String) voteMap.get("NODEID");
      }

      @Override
      public boolean isRequestServerVote()
      {
         return true;
      }

      @Override
      public Pair<String, Long> getVote()
      {
         return new Pair<>(nodeID, backupsSize);
      }

      @Override
      public Map<String, Object> getVoteMap()
      {
         Map<String, Object> map = super.getVoteMap();
         map.put("BACKUP_SIZE", backupsSize);
         if (nodeID != null)
         {
            map.put("NODEID", nodeID);
         }
         return map;
      }
   }
}
