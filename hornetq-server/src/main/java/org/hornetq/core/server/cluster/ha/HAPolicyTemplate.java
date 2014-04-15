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
