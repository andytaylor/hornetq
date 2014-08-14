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
package org.hornetq.api.core.client;

import java.util.EnumSet;
import java.util.Set;


public enum ServerEvent
{
   ADDRESS_PAUSED((byte) 0),
   ADDRESS_RESUMED((byte) 1);

   private final byte type;

   ServerEvent(byte type)
   {
      this.type = type;
   }
   private static final Set<ServerEvent> all = EnumSet.allOf(ServerEvent.class);

   public static ServerEvent toStateType(byte b)
   {
      for (ServerEvent stateType : all)
      {
         if (b == stateType.type)
         {
            return stateType;
         }
      }
      return null;
   }

   public byte getType()
   {
      return type;
   }
}
