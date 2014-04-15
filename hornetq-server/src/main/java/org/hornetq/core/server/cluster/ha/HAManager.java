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

import org.hornetq.api.core.client.ClusterTopologyListener;
import org.hornetq.api.core.client.TopologyMember;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.cluster.ClusterConnection;
import org.hornetq.core.server.cluster.qourum.QuorumVote;
import org.hornetq.core.server.cluster.qourum.Vote;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.hornetq.spi.core.security.HornetQSecurityManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HAManager implements ClusterTopologyListener
{
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

   private final class RequestBackupQuorumVote implements QuorumVote
   {
      @Override
      public Vote connected()
      {
         return new RequestBackupVote();
      }

      @Override
      public Vote notConnected()
      {
         return new RequestBackupVote();
      }

      @Override
      public void vote(Vote vote)
      {
         System.out.println("org.hornetq.core.server.cluster.ha.HAManager.RequestBackupQuorumVote.vote");
      }

      @Override
      public Object getDecision()
      {
         return null;
      }

      @Override
      public void allVotesCast()
      {
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

   class RequestBackupVote implements Vote
   {
      @Override
      public boolean isRequestServerVote()
      {
         return true;
      }

      @Override
      public Object getVote()
      {
         return null;
      }
   }
}
