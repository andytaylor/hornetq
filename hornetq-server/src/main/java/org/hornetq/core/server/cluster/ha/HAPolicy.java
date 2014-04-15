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

public class HAPolicy
{
   private static final boolean DEFAULT_REQUEST_BACKUP = false;

   private static final int DEFAULT_BACKUP_REQUEST_RETRIES = -1;

   private static final long DEFAULT_BACKUP_REQUEST_RETRY_INTERVAL = 5000;

   private static final boolean DEFAULT_ALLOW_BACKUP_REQUESTS = false;

   private static final int DEFAULT_MAX_BACKUPS = 1;

   private static final int DEFAULT_BACKUP_PORT_OFFSET = 100;

   private boolean requestBackup = DEFAULT_REQUEST_BACKUP;

   private int backupRequestRetries = DEFAULT_BACKUP_REQUEST_RETRIES;

   private long backupRequestRetryInterval = DEFAULT_BACKUP_REQUEST_RETRY_INTERVAL;

   private boolean allowBackupRequests = DEFAULT_ALLOW_BACKUP_REQUESTS;

   private int maxBackups = DEFAULT_MAX_BACKUPS;

   private int backupPortOffset = DEFAULT_BACKUP_PORT_OFFSET;

   public boolean isRequestBackup()
   {
      return requestBackup;
   }

   public void setRequestBackup(boolean requestBackup)
   {
      this.requestBackup = requestBackup;
   }

   public int getBackupRequestRetries()
   {
      return backupRequestRetries;
   }

   public void setBackupRequestRetries(int backupRequestRetries)
   {
      this.backupRequestRetries = backupRequestRetries;
   }

   public long getBackupRequestRetryInterval()
   {
      return backupRequestRetryInterval;
   }

   public void setBackupRequestRetryInterval(long backupRequestRetryInterval)
   {
      this.backupRequestRetryInterval = backupRequestRetryInterval;
   }

   public boolean isAllowBackupRequests()
   {
      return allowBackupRequests;
   }

   public void setAllowBackupRequests(boolean allowBackupRequests)
   {
      this.allowBackupRequests = allowBackupRequests;
   }

   public int getMaxBackups()
   {
      return maxBackups;
   }

   public void setMaxBackups(int maxBackups)
   {
      this.maxBackups = maxBackups;
   }

   public int getBackupPortOffset()
   {
      return backupPortOffset;
   }

   public void setBackupPortOffset(int backupPortOffset)
   {
      this.backupPortOffset = backupPortOffset;
   }
}
