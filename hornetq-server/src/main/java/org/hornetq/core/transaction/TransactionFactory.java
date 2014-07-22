package org.hornetq.core.transaction;

import org.hornetq.core.persistence.StorageManager;

import javax.transaction.xa.Xid;

/**
 * Created by andy on 22/07/14.
 */
public interface TransactionFactory
{
   Transaction newTransaction(Xid xid, StorageManager storageManager, int timeoutSeconds);
}
