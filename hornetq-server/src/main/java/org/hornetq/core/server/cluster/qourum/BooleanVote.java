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
package org.hornetq.core.server.cluster.qourum;


import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.api.core.SimpleString;

import java.util.Map;

/**
 * a simpel yes.no vote
 */
public final class BooleanVote extends Vote<Boolean>
{
   private boolean vote;

   public BooleanVote(boolean vote, long id)
   {
      super(id);
      this.vote = vote;
   }

   @Override
   public boolean isRequestServerVote()
   {
      return false;
   }

   public Boolean getVote()
   {
      return vote;
   }

   @Override
   public Map<String, Object> getVoteMap()
   {
      return null;
   }
}
