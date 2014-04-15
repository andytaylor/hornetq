package org.hornetq.core.server.cluster.qourum;

import org.hornetq.api.core.SimpleString;

import java.util.Map;

public interface QuorumVoteHandler
{
   Vote vote(Map<String, Object> voteParams);

   SimpleString getQuorumName();
}
