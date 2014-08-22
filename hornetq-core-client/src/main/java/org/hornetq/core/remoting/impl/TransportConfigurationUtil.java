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

package org.hornetq.core.remoting.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.TransportConfigurationHelper;
import org.hornetq.utils.ClassloadingUtil;

/**
 * Stores static mappings of class names to ConnectorFactory instances to act as a central repo for ConnectorFactory
 * objects.
 *
 * @author <a href="mailto:mtaylor@redhat.com">Martyn Taylor</a>
 */

public class TransportConfigurationUtil
{
   private static final Map<String, Map<String, Object>> DEFAULTS = new HashMap<>();

   private static final Map<String, Object> EMPTY_HELPER =  new HashMap<>();

   public static Map<String, Object> getDefaults(String className)
   {
      if (className == null)
      {
         return EMPTY_HELPER;
      }
      if (!DEFAULTS.containsKey(className))
      {
         Object object = instantiateObject(className);
         if (object != null && object instanceof TransportConfigurationHelper)
         {
            DEFAULTS.put(className, ((TransportConfigurationHelper) object).getDefaults());
         }
         else
         {
            DEFAULTS.put(className, EMPTY_HELPER);
         }
      }
      return DEFAULTS.get(className);
   }

   private static Object instantiateObject(final String className)
   {
      return AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            try
            {
               return ClassloadingUtil.newInstanceFromClassLoader(className);
            }
            catch (IllegalStateException e)
            {
               return null;
            }
         }
      });
   }
}
