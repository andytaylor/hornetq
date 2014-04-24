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


public enum HAPolicyTemplate
{
   LIVE_ONLY(new HAPolicy()),
   AUTOMATIC_SHARED_STORE(createAutomaticSharedStorePolicy());

   private final HAPolicy haPolicy;

   public HAPolicy getHaPolicy()
   {
      return haPolicy;
   }

   HAPolicyTemplate(HAPolicy haPolicy)
   {
      this.haPolicy = haPolicy;
   }

   private static HAPolicy createAutomaticSharedStorePolicy()
   {
      HAPolicy policy = new HAPolicy();
      policy.setAllowBackupRequests(true);
      policy.setBackupPortOffset(100);
      policy.setBackupRequestRetries(-1);
      policy.setBackupRequestRetryInterval(5000);
      policy.setMaxBackups(2);
      policy.setRequestBackup(true);
      return policy;
   }
}
