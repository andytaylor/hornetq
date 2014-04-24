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
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.TopologyMember;
import org.hornetq.core.client.impl.ServerLocatorImpl;
import org.hornetq.core.client.impl.Topology;
import org.hornetq.core.client.impl.TopologyMemberImpl;
import org.hornetq.core.config.BackupStrategy;
import org.hornetq.core.config.ClusterConnectionConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.protocol.ServerPacketDecoder;
import org.hornetq.core.protocol.core.Channel;
import org.hornetq.core.protocol.core.CoreRemotingConnection;
import org.hornetq.core.protocol.core.impl.ChannelImpl;
import org.hornetq.core.protocol.core.impl.PacketImpl;
import org.hornetq.core.protocol.core.impl.wireformat.BackupRequestMessage;
import org.hornetq.core.protocol.core.impl.wireformat.BackupResponseMessage;
import org.hornetq.core.server.HornetQComponent;
import org.hornetq.core.server.HornetQServer;
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

/*
* An HAManager takes care of any colocated backups in a VM. These are either pre configured backups or backups requested
* by other lives. It also takes care of the quorum voting to request backups.
* */
public class HAManager implements HornetQComponent
{
   private static final SimpleString REQUEST_BACKUP_QUORUM_VOTE = new SimpleString("RequestBackupQuorumVote");

   private final HAPolicy haPolicy;

   private final HornetQSecurityManager securityManager;

   private final  HornetQServerImpl server;

   private Set<Configuration> backupServerConfigurations;

   private Map<String, HornetQServer> backupServers = new HashMap<>();

   private boolean started;

   public HAManager(HAPolicy haPolicy, HornetQSecurityManager securityManager, HornetQServerImpl hornetQServer, Set<Configuration> backupServerConfigurations)
   {
      this.haPolicy = haPolicy;
      this.securityManager = securityManager;
      server = hornetQServer;
      this.backupServerConfigurations = backupServerConfigurations;
   }

   /**
    * starts the HA manager, any pre configured backups are started and if a backup is needed a quorum vote in initiated
    */
   public void start()
   {
      if (started)
         return;
      server.getClusterManager().getQuorumManager().registerQuorumHandler(new RequestBackupQuorumVoteHandler());
      if (backupServerConfigurations != null)
      {
         for (Configuration configuration : backupServerConfigurations)
         {
            HornetQServer backup = new HornetQServerImpl(configuration, null, securityManager, server);
            backupServers.put(configuration.getName(), backup);
         }
      }
      //start the backups
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

      //vote for a backup if required
      if (haPolicy.isRequestBackup())
      {
         server.getClusterManager().getQuorumManager().vote(new RequestBackupQuorumVote());
      }
      started = true;
   }

   /**
    * stop any backups
    */
   public void stop()
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
      started = false;
   }

   @Override
   public boolean isStarted()
   {
      return started;
   }

   public synchronized void activateSharedStoreBackup(int backupSize, String journalDirectory, String bindingsDirectory, String largeMessagesDirectory, String pagingDirectory)
   {

   }

   /**
    * activate a backup server replicating from a specified node.
    *
    * @param backupSize the number of backups the requesting server thinks there are. if this is changed then we should
    * decline and the requesting server can cast a re vote
    * @param nodeID the id of the node to replicate from
    * @return true if the server was created and started
    * @throws Exception
    */
   public synchronized boolean activateReplicatedBackup(int backupSize, SimpleString nodeID) throws Exception
   {
      if (backupServers.size() >= haPolicy.getMaxBackups() || backupSize != backupServers.size())
      {
         return false;
      }
      Configuration configuration = server.getConfiguration().copy();
      HornetQServer backup = new HornetQServerImpl(configuration, null, securityManager, server);
      try
      {
         TopologyMember member = server.getClusterManager().getDefaultConnection(null).getTopology().getMember(nodeID.toString());
         int portOffset = haPolicy.getBackupPortOffset() * (backupServers.size() + 1);
         String name = "colocated_backup_" + backupServers.size() + 1;
         updateConfiguration(configuration, haPolicy.getBackupStrategy(), name, portOffset, haPolicy.getScaledownConnector(), haPolicy.getRemoteConnectors());
         configuration.setSharedStore(false);
         configuration.setBackup(true);
         backup.addActivationParam("REPLICATION_ENDPOINT", member);
         backupServers.put(configuration.getName(), backup);
         backup.start();
      }
      catch (Exception e)
      {
         backup.stop();
         //todo log a warning
         return false;
      }
      return true;
   }

   /**
    * return the current backup servers
    *
    * @return the backups
    */
   public Map<String, HornetQServer> getBackupServers()
   {
      return backupServers;
   }

   /**
    * send a request to a live server to start a backup for us
    *
    * @param connectorPair the connector for the node to request a backup from
    * @param backupSize the current size of the requested nodes backups
    * @return true if the request wa successful.
    * @throws Exception
    */
   private boolean requestBackup(Pair<TransportConfiguration, TransportConfiguration> connectorPair, int backupSize) throws Exception
   {
      try
      (
         ServerLocatorImpl locator = (ServerLocatorImpl) HornetQClient.createServerLocatorWithoutHA(connectorPair.getA())
      )
      {
         locator.setPacketDecoder(ServerPacketDecoder.INSTANCE);
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
            backupRequestMessage = new BackupRequestMessage(backupSize, server.getNodeID());
         }
         BackupResponseMessage packet = (BackupResponseMessage) channel.sendBlocking(backupRequestMessage, PacketImpl.BACKUP_REQUEST_RESPONSE);
         return packet.isBackupStarted();
      }
   }

   /**
    * update the backups configuration
    *
    * @param backupConfiguration the configuration to update
    * @param backupStrategy the strategy for the backup
    * @param name the new name of the backup
    * @param portOffset the offset for the acceptors and any connectors that need changing
    * @param scaleDownConnector the name of the scale down connector if required
    * @param remoteConnectors the connectors that don't need off setting, typically remote
    */
   private static void updateConfiguration(Configuration backupConfiguration, BackupStrategy backupStrategy, String name, int portOffset, String scaleDownConnector, List<String> remoteConnectors)
   {
      backupConfiguration.setBackupStrategy(backupStrategy);
      backupConfiguration.setName(name);

      backupConfiguration.setJournalDirectory(backupConfiguration.getJournalDirectory() + name);
      backupConfiguration.setPagingDirectory(backupConfiguration.getPagingDirectory() + name);
      backupConfiguration.setLargeMessagesDirectory(backupConfiguration.getLargeMessagesDirectory() + name);
      backupConfiguration.setBindingsDirectory(backupConfiguration.getBindingsDirectory() + name);
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
            if (!remoteConnectors.contains(entry.getValue().getName()))
            {
               updatebackupParams(name, portOffset, entry.getValue().getParams());
            }
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

   /**
    * A vote handler for incoming backup request votes
    */
   private final class RequestBackupQuorumVoteHandler implements QuorumVoteHandler
   {
      @Override
      public Vote vote(Map<String, Object> voteParams)
      {
         return new RequestBackupVote((Long) voteParams.get("ID"), backupServers.size(), server.getNodeID().toString(), backupServers.size() < haPolicy.getMaxBackups());
      }

      @Override
      public SimpleString getQuorumName()
      {
         return REQUEST_BACKUP_QUORUM_VOTE;
      }
   }

   /**
    * a quorum vote for backup requests
    */
   private final class RequestBackupQuorumVote extends QuorumVote<RequestBackupVote, Pair<String, Long>>
   {
      //the available nodes that we can request
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
      public void vote(RequestBackupVote vote)
      {
         //if the returned vote is available add it to the nodes we can request
         if (vote.backupAvailable)
         {
            nodes.add(vote.getVote());
         }
      }

      @Override
      public Pair<String, Long> getDecision()
      {
         //sort the nodes by how many backups they have and choose the first
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
      public void allVotesCast(Topology voteTopology)
      {
         //if we have any nodes that we can request then send a request
         if (nodes.size() > 0)
         {
            Pair<String, Long> decision = getDecision();
            TopologyMemberImpl member = voteTopology.getMember(decision.getA());
            try
            {
               boolean backupStarted = requestBackup(member.getConnector(), decision.getB().intValue());
               if (!backupStarted)
               {
                  nodes.clear();
                  server.getScheduledPool().schedule(new Runnable()
                  {
                     @Override
                     public void run()
                     {
                        server.getClusterManager().getQuorumManager().vote(new RequestBackupQuorumVote());
                     }
                  }, haPolicy.getBackupRequestRetryInterval(), TimeUnit.MILLISECONDS);
               }
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
      public static final String ID = "ID";
      public static final String BACKUP_SIZE = "BACKUP_SIZE";
      public static final String NODEID = "NODEID";
      public static final String BACKUP_AVAILABLE = "BACKUP_AVAILABLE";
      private long backupsSize;
      private String nodeID;
      private boolean backupAvailable;

      public RequestBackupVote(long id)
      {
         super(id);
         backupsSize = -1;
      }

      public RequestBackupVote(Long id, int backupsSize, String nodeID, boolean backupAvailable)
      {
         super(id);
         this.backupsSize = backupsSize;
         this.nodeID = nodeID;
         this.backupAvailable = backupAvailable;
      }

      public RequestBackupVote(Map<String, Object> voteMap)
      {
         super((Long) voteMap.get(ID));
         backupsSize = (Long) voteMap.get(BACKUP_SIZE);
         nodeID = (String) voteMap.get(NODEID);
         backupAvailable = (Boolean) voteMap.get(BACKUP_AVAILABLE);
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
         map.put(BACKUP_SIZE, backupsSize);
         if (nodeID != null)
         {
            map.put(NODEID, nodeID);
         }
         map.put(BACKUP_AVAILABLE, backupAvailable);
         return map;
      }
   }
}
