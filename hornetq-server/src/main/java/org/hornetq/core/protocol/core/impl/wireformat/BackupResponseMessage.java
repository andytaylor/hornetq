package org.hornetq.core.protocol.core.impl.wireformat;


import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.core.protocol.core.impl.PacketImpl;

public class BackupResponseMessage extends PacketImpl
{
   boolean backupStarted;

   public BackupResponseMessage()
   {
      super(PacketImpl.BACKUP_REQUEST_RESPONSE);
   }

   public BackupResponseMessage(boolean backupStarted)
   {
      super(PacketImpl.BACKUP_REQUEST_RESPONSE);
      this.backupStarted = backupStarted;
   }

   @Override
   public void encodeRest(HornetQBuffer buffer)
   {
      super.encodeRest(buffer);
      buffer.writeBoolean(backupStarted);
   }

   @Override
   public void decodeRest(HornetQBuffer buffer)
   {
      super.decodeRest(buffer);
      backupStarted = buffer.readBoolean();
   }

   @Override
   public boolean isResponse()
   {
      return true;
   }

   public boolean isBackupStarted()
   {
      return backupStarted;
   }

   public void setBackupStarted(boolean backupStarted)
   {
      this.backupStarted = backupStarted;
   }
}
