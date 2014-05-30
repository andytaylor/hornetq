package org.hornetq.byteman.tests;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;

import javax.transaction.TransactionManager;

/**
* Created by andy on 30/05/14.
*/
public class DummyTMLocator
{
   public static TransactionManagerImple tm = new TransactionManagerImple();
   public TransactionManager getTM()
   {
      return tm;
   }
}
