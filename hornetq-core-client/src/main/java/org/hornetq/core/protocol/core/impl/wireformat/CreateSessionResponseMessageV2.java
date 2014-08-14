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
package org.hornetq.core.protocol.core.impl.wireformat;


import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.api.core.SimpleString;

import java.util.HashSet;
import java.util.Set;

public class CreateSessionResponseMessageV2 extends CreateSessionResponseMessage
{
   private Set<SimpleString> pausedAddresses;


   public CreateSessionResponseMessageV2(final int serverVersion, Set<SimpleString> pausedAddresses)
   {
      super(serverVersion, CREATESESSION_RESP_V2);
      this.pausedAddresses = pausedAddresses;
   }

   public CreateSessionResponseMessageV2()
   {
      super(-1, CREATESESSION_RESP_V2);
   }

   public Set<SimpleString> getPausedAddresses()
   {
      return pausedAddresses;
   }

   @Override
   public void encodeRest(final HornetQBuffer buffer)
   {
      super.encodeRest(buffer);
      buffer.writeInt(pausedAddresses.size());
      for (SimpleString address : pausedAddresses)
      {
         buffer.writeSimpleString(address);
      }
   }

   @Override
   public void decodeRest(final HornetQBuffer buffer)
   {
      super.decodeRest(buffer);
      int numAddresses = buffer.readInt();
      pausedAddresses = new HashSet<>(numAddresses);
      for (int i = 0; i < numAddresses; i++)
      {
         pausedAddresses.add(buffer.readSimpleString());
      }
   }
}
