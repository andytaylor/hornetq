package org.hornetq.core.server.impl;


import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.Pair;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.TopologyMember;
import org.hornetq.core.server.LiveNodeLocator;

public class NamedNodeIdNodeLocator extends LiveNodeLocator
{
   private final String nodeID;

   private final Pair<TransportConfiguration, TransportConfiguration> liveConfiguration;

   public NamedNodeIdNodeLocator(String nodeID, Pair<TransportConfiguration, TransportConfiguration> liveConfiguration)
   {
      this.nodeID = nodeID;
      this.liveConfiguration = liveConfiguration;
   }


   @Override
   public void locateNode(long timeout) throws HornetQException
   {
      //noop
   }

   @Override
   public void locateNode() throws HornetQException
   {
      //noop
   }

   @Override
   public Pair<TransportConfiguration, TransportConfiguration> getLiveConfiguration()
   {
      return liveConfiguration;
   }

   @Override
   public String getNodeID()
   {
      return nodeID;
   }

   @Override
   public void nodeUP(TopologyMember member, boolean last)
   {

   }

   @Override
   public void nodeDown(long eventUID, String nodeID)
   {

   }
}
