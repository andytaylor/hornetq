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
import org.hornetq.api.core.client.ServerEvent;
import org.hornetq.core.protocol.core.impl.PacketImpl;

public class ServerEventMessage extends PacketImpl
{
   private ServerEvent serverEvent;
   private SimpleString address;

   public ServerEventMessage(SimpleString address, ServerEvent serverEvent)
   {
      super(ADDRESS_STATE_CHANGED);
      this.address = address;
      this.serverEvent = serverEvent;
   }

   public ServerEventMessage()
   {
      super(ADDRESS_STATE_CHANGED);
   }

   @Override
   public void decodeRest(HornetQBuffer buffer)
   {
      address = buffer.readNullableSimpleString();
      serverEvent = ServerEvent.toStateType(buffer.readByte());
   }

   @Override
   public void encodeRest(HornetQBuffer buffer)
   {
      buffer.writeNullableSimpleString(address);
      buffer.writeByte(serverEvent.getType());
   }

   public SimpleString getAddress()
   {
      return address;
   }

   public ServerEvent getServerEvent()
   {
      return serverEvent;
   }
}
