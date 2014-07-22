package org.hornetq.core.protocol.openwire;

import org.hornetq.core.persistence.StorageManager;
import org.hornetq.core.server.Queue;
import org.hornetq.core.server.impl.RefsOperation;
import org.hornetq.core.transaction.Transaction;
import org.hornetq.core.transaction.impl.TransactionImpl;

import javax.transaction.xa.Xid;

public class AMQTransaction extends TransactionImpl
{

   public AMQTransaction(StorageManager storageManager, int timeoutSeconds)
   {
      super(storageManager, timeoutSeconds);
   }

   public AMQTransaction(StorageManager storageManager)
   {
      super(storageManager);
   }

   public AMQTransaction(Xid xid, StorageManager storageManager, int timeoutSeconds)
   {
      super(xid, storageManager, timeoutSeconds);
   }

   public AMQTransaction(long id, Xid xid, StorageManager storageManager)
   {
      super(id, xid, storageManager);
   }

   @Override
   public RefsOperation createRefsOperation(Queue queue)
   {
      return new AMQrefsOperation(queue, storageManager);
   }

   public class AMQrefsOperation extends RefsOperation
   {
      public AMQrefsOperation(Queue queue, StorageManager storageManager)
      {
         super(queue, storageManager);
      }

      @Override
      public void afterRollback(Transaction tx)
      {
         //do nothing for AMQ
      }
   }
}