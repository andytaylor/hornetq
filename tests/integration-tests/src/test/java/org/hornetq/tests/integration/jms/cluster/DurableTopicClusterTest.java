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
package org.hornetq.tests.integration.jms.cluster;

import org.hornetq.tests.util.JMSClusteredTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * A TopicClusterTest
 *
 * @author <mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 */
public class DurableTopicClusterTest extends JMSClusteredTestBase
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------


   @After
   public void tearDown() throws Exception
   {
      super.tearDown();
   }

   @Test
   public void testDurableSubStillReceivesOnBridgeReconnecting() throws Exception
   {
      Connection conn1 = cf1.createConnection();

      conn1.setClientID("someClient1");

      Connection conn2 = cf2.createConnection();

      conn2.setClientID("someClient2");

      conn1.start();

      conn2.start();

      try
      {

         Topic topic1 = createDurableTopic("t1");

         Topic topic2 = (Topic) context1.lookup("topic/t1");

         Session session1 = conn1.createSession(false, Session.AUTO_ACKNOWLEDGE);

         Session session2 = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);

         // topic1 and 2 should be the same.
         // Using a different instance here just to make sure it is implemented correctly
         MessageConsumer cons2 = session2.createDurableSubscriber(topic2, "sub2");
         Thread.sleep(500);
         MessageProducer prod1 = session1.createProducer(topic1);

         prod1.setDeliveryMode(DeliveryMode.PERSISTENT);

         prod1.send(session1.createTextMessage("m1"));

         TextMessage received = (TextMessage) cons2.receive(5000);

         assertNotNull(received);

         assertEquals("m1", received.getText());

         session2.close();

         cons2.close();

         jmsServer2.stop();

         System.err.println("TopicClusterTest.testDurableSubStillReceivesOnBridgeReconnecting");
         prod1.send(session1.createTextMessage("m2"));

         jmsServer2.start();
         Thread.sleep(10000);
         prod1.send(session1.createTextMessage("m3"));
         conn2 = cf2.createConnection();

         conn2.setClientID("someClient2");

         conn2.start();

         session2 = conn2.createSession(false, Session.AUTO_ACKNOWLEDGE);

         cons2 = session2.createDurableSubscriber(topic2, "sub2");

         prod1.send(session1.createTextMessage("m4"));

         received = (TextMessage) cons2.receive(5000);

         assertNotNull(received);

         assertEquals("m1", received.getText());

      }
      finally
      {
         conn1.close();
         conn2.close();
      }

      jmsServer1.destroyTopic("t1");
      jmsServer2.destroyTopic("t1");


   }

   @Before
   public void setUp() throws Exception
   {
      super.setUp();
   }

   @Override
   public boolean isPersisted()
   {
      return true;
   }

// Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
