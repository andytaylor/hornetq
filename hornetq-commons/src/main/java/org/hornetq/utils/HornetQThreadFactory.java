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
package org.hornetq.utils;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * A HornetQThreadFactory
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public final class HornetQThreadFactory implements ThreadFactory
{
   private final ThreadGroup group;

   private final AtomicInteger threadCount = new AtomicInteger(0);

   private final int threadPriority;

   private final boolean daemon;

   private final ClassLoader tccl;

   private final AccessControlContext acc;

   public HornetQThreadFactory(final String groupName, final boolean daemon, final ClassLoader tccl)
   {
      group = new ThreadGroup(groupName + "-" + System.identityHashCode(this));

      this.threadPriority = Thread.NORM_PRIORITY;

      this.tccl = tccl;

      this.daemon = daemon;

      this.acc = (System.getSecurityManager() == null) ? null : AccessController.getContext();
   }

   public Thread newThread(final Runnable command)
   {
      // always create a thread in a privileged block if running with Security Manager
      if (acc != null && System.getSecurityManager() != null)
      {
         return AccessController.doPrivileged(new ThreadCreateAction(command), acc);
      }
      else
      {
         return createThread(command);
      }
   }

   private final class ThreadCreateAction implements PrivilegedAction<Thread>
   {
      private final Runnable target;

      private ThreadCreateAction(final Runnable target)
      {
         this.target = target;
      }

      public Thread run()
      {
         return createThread(target);
      }
   }

   private Thread createThread(final Runnable command)
   {
      final Thread t = new Thread(group, command, "Thread-" + threadCount.getAndIncrement() + " (" + group.getName() + ")");
      t.setDaemon(daemon);
      t.setPriority(threadPriority);
      t.setContextClassLoader(tccl);

      return t;
   }
}
