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
package org.hornetq.byteman.tests;

import com.arjuna.ats.jta.exceptions.RollbackException;
import org.hornetq.api.core.HornetQInterruptedException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.core.postoffice.Binding;
import org.hornetq.core.server.Queue;
import org.hornetq.core.transaction.impl.XidImpl;
import org.hornetq.ra.HornetQResourceAdapter;
import org.hornetq.ra.inflow.HornetQActivationSpec;
import org.hornetq.tests.integration.ra.HornetQMessageHandlerXATest;
import org.hornetq.tests.integration.ra.HornetQRATestBase;
import org.hornetq.utils.UUIDGenerator;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.resource.ResourceException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         Created May 20, 2010
 */
@RunWith(BMUnitRunner.class)
public class HornetQMessageHandlerTest extends HornetQRATestBase
{

   @Override
   public boolean useSecurity()
   {
      return false;
   }

   @Test
   @BMRules
   (
      rules =
            {
               @BMRule
                     (
                           name = "interrupt",
                           targetClass = "io.netty.channel.AbstractChannel",
                           targetMethod = "writeAndFlush",
                           targetLocation = "ENTRY",
                           action = "org.hornetq.byteman.tests.HornetQMessageHandlerTest.interrupt();"
                     )
            }
   )
   public void testSimpleMessageReceivedOnQueue() throws Exception
   {
      SimpleString ssQueue = new SimpleString("jms.queue." + MDBQUEUE);
      HornetQResourceAdapter qResourceAdapter = newResourceAdapter();
      qResourceAdapter.setTransactionManagerLocatorClass(DummyTMLocator.class.getName());
      qResourceAdapter.setTransactionManagerLocatorMethod("getTM");
      MyBootstrapContext ctx = new MyBootstrapContext();
      qResourceAdapter.setConnectorClassName(NETTY_CONNECTOR_FACTORY);
      qResourceAdapter.start(ctx);
      HornetQActivationSpec spec = new HornetQActivationSpec();
      spec.setMaxSession(1);
      spec.setResourceAdapter(qResourceAdapter);
      spec.setUseJNDI(false);
      spec.setDestinationType("javax.jms.Queue");
      spec.setDestination(MDBQUEUE);
      CountDownLatch latch = new CountDownLatch(1);
      XADummyEndpoint endpoint = new XADummyEndpoint(latch);
      DummyMessageEndpointFactory endpointFactory = new DummyMessageEndpointFactory(endpoint, true);
      qResourceAdapter.endpointActivation(endpointFactory, spec);
      ClientSession session = locator.createSessionFactory().createSession();
      ClientProducer clientProducer = session.createProducer(MDBQUEUEPREFIXED);
      ClientMessage message = session.createMessage(true);
      message.getBodyBuffer().writeString("teststring");
      clientProducer.send(message);
      session.close();
      latch.await(5, TimeUnit.SECONDS);

      assertNotNull(endpoint.lastMessage);
      assertEquals(endpoint.lastMessage.getCoreMessage().getBodyBuffer().readString(), "teststring");

      qResourceAdapter.endpointDeactivation(endpointFactory, spec);

      qResourceAdapter.stop();

      Binding binding = server.getPostOffice().getBinding(ssQueue);
      assertEquals(1, ((Queue)binding.getBindable()).getMessageCount());
   }

   static volatile boolean inAfterDelivery = false;
   static volatile boolean inAfterDelivery2 = false;
   public static void interrupt()
   {
      if (inAfterDelivery)
      {
         throw new HornetQInterruptedException(new InterruptedException("interrupt from after delivery"));
      }
   }

   public class XADummyEndpoint extends DummyMessageEndpoint
   {
      private Xid xid;

      public XADummyEndpoint(CountDownLatch latch) throws SystemException
      {
         super(latch);
         xid = new XidImpl("xa1".getBytes(), 1, UUIDGenerator.getInstance().generateStringUUID().getBytes());
         Transaction transaction = DummyTMLocator.tm.getTransaction();
      }

      @Override
      public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException
      {
         super.beforeDelivery(method);
         try
         {
            xaResource.start(xid, XAResource.TMNOFLAGS);
         }
         catch (XAException e)
         {
            throw new ResourceException(e.getMessage(), e);
         }
      }

      @Override
      public void afterDelivery() throws ResourceException
      {
         if (!inAfterDelivery && !inAfterDelivery2)
         {
            inAfterDelivery = true;
            try
            {
               xaResource.end(xid, XAResource.TMSUCCESS);
            }
            catch(RuntimeException re)
            {
               //before justins fix
               super.afterDelivery();
               inAfterDelivery = false;
               inAfterDelivery2 = true;
               throw re;
            }
            catch (XAException e)
            {
               inAfterDelivery = false;
               inAfterDelivery2 = true;
               //after justins fix ( https://github.com/hornetq/hornetq/commit/3f1a162b673de6cfc273e122a9df44f5162cf09b ) the TM would catch the XA exception and call rollback.
            /*try
            {
               xaResource.rollback(xid);
            } catch (XAException e1)
            {
               //the tm would remove the connection
            }*/
               throw new HornetQInterruptedException(new InterruptedException("interrupt from after delivery"));
            }
         }
         else if (inAfterDelivery2)
         {
            throw new RollbackException("ARJUNA016102: The transaction is not active! ");
         }


         super.afterDelivery();
      }

      public void rollback() throws XAException
      {
         xaResource.rollback(xid);
      }

      public void prepare() throws XAException
      {
         xaResource.prepare(xid);
      }

      public void commit() throws XAException
      {
         xaResource.commit(xid, false);
      }
   }
}
