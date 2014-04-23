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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hornetq.core.config.BackupStrategy;

public class HAPolicy implements Serializable
{
   public enum BACKUP_TYPE
   {
      SHARED_STORE((byte) 0),
      REPLICATED((byte) 1);

      private static final Set<BACKUP_TYPE> all = EnumSet.allOf(BACKUP_TYPE.class);
      private final byte type;

      BACKUP_TYPE(byte type)
      {
         this.type = type;
      }

      public byte getType()
      {
         return type;
      }

      public static BACKUP_TYPE toBackupType(byte b)
      {
         for (BACKUP_TYPE backupType : all)
         {
            if (b == backupType.getType())
            {
               return backupType;
            }
         }
         return null;
      }

   }

   private static final boolean DEFAULT_REQUEST_BACKUP = false;

   private static final int DEFAULT_BACKUP_REQUEST_RETRIES = -1;

   private static final long DEFAULT_BACKUP_REQUEST_RETRY_INTERVAL = 5000;

   private static final boolean DEFAULT_ALLOW_BACKUP_REQUESTS = false;

   private static final int DEFAULT_MAX_BACKUPS = 1;

   private static final int DEFAULT_BACKUP_PORT_OFFSET = 100;

   private BACKUP_TYPE backupType = BACKUP_TYPE.REPLICATED;

   private BackupStrategy backupStrategy = BackupStrategy.FULL;

   private String scaledownConnector;

   private boolean requestBackup = DEFAULT_REQUEST_BACKUP;

   private int backupRequestRetries = DEFAULT_BACKUP_REQUEST_RETRIES;

   private long backupRequestRetryInterval = DEFAULT_BACKUP_REQUEST_RETRY_INTERVAL;

   private boolean allowBackupRequests = DEFAULT_ALLOW_BACKUP_REQUESTS;

   private int maxBackups = DEFAULT_MAX_BACKUPS;

   private int backupPortOffset = DEFAULT_BACKUP_PORT_OFFSET;

   private List<String> remoteConnectors = new ArrayList<>();

   public BACKUP_TYPE getBackupType()
   {
      return backupType;
   }

   public void setBackupType(BACKUP_TYPE backupType)
   {
      this.backupType = backupType;
   }

   public BackupStrategy getBackupStrategy()
   {
      return backupStrategy;
   }

   public void setBackupStrategy(BackupStrategy backupStrategy)
   {
      this.backupStrategy = backupStrategy;
   }

   public String getScaledownConnector()
   {
      return scaledownConnector;
   }

   public void setScaledownConnector(String scaledownConnector)
   {
      this.scaledownConnector = scaledownConnector;
   }

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

   public List<String> getRemoteConnectors()
   {
      return remoteConnectors;
   }

   public void setRemoteConnectors(List<String> remoteConnectors)
   {
      this.remoteConnectors = remoteConnectors;
   }
}
