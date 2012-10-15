/*
 * Copyright 2009 Red Hat, Inc.
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

package org.hornetq.util;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.api.core.HornetQBuffers;
import org.hornetq.api.core.SimpleString;
import org.hornetq.tests.util.RandomUtil;
import org.hornetq.tests.CoreUnitTestCase;
import org.hornetq.utils.TypedProperties;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class TypedPropertiesTest extends TestCase
{

   private static void assertEqualsTypeProperties(final TypedProperties expected, final TypedProperties actual)
   {
      Assert.assertNotNull(expected);
      Assert.assertNotNull(actual);
      Assert.assertEquals(expected.getEncodeSize(), actual.getEncodeSize());
      Assert.assertEquals(expected.getPropertyNames(), actual.getPropertyNames());
      Iterator<SimpleString> iterator = actual.getPropertyNames().iterator();
      while (iterator.hasNext())
      {
         SimpleString key = iterator.next();
         Object expectedValue = expected.getProperty(key);
         Object actualValue = actual.getProperty(key);
         if (expectedValue instanceof byte[] && actualValue instanceof byte[])
         {
            byte[] expectedBytes = (byte[])expectedValue;
            byte[] actualBytes = (byte[])actualValue;
            CoreUnitTestCase.assertEqualsByteArrays(expectedBytes, actualBytes);
         }
         else
         {
            Assert.assertEquals(expectedValue, actualValue);
         }
      }
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   private TypedProperties props;

   private SimpleString key;

   public void testCopyContructor() throws Exception
   {
      props.putSimpleStringProperty(key, RandomUtil.randomSimpleString());

      TypedProperties copy = new TypedProperties(props);

      Assert.assertEquals(props.getEncodeSize(), copy.getEncodeSize());
      Assert.assertEquals(props.getPropertyNames(), copy.getPropertyNames());

      Assert.assertTrue(copy.containsProperty(key));
      Assert.assertEquals(props.getProperty(key), copy.getProperty(key));
   }

   public void testRemove() throws Exception
   {
      props.putSimpleStringProperty(key, RandomUtil.randomSimpleString());

      Assert.assertTrue(props.containsProperty(key));
      Assert.assertNotNull(props.getProperty(key));

      props.removeProperty(key);

      Assert.assertFalse(props.containsProperty(key));
      Assert.assertNull(props.getProperty(key));
   }

   public void testClear() throws Exception
   {
      props.putSimpleStringProperty(key, RandomUtil.randomSimpleString());

      Assert.assertTrue(props.containsProperty(key));
      Assert.assertNotNull(props.getProperty(key));

      props.clear();

      Assert.assertFalse(props.containsProperty(key));
      Assert.assertNull(props.getProperty(key));
   }

   public void testKey() throws Exception
   {
      props.putBooleanProperty(key, true);
      boolean bool = (Boolean)props.getProperty(key);
      Assert.assertEquals(true, bool);

      props.putCharProperty(key, 'a');
      char c = (Character)props.getProperty(key);
      Assert.assertEquals('a', c);
   }

   public void testGetPropertyOnEmptyProperties() throws Exception
   {
      Assert.assertFalse(props.containsProperty(key));
      Assert.assertNull(props.getProperty(key));
   }

   public void testRemovePropertyOnEmptyProperties() throws Exception
   {
      Assert.assertFalse(props.containsProperty(key));
      Assert.assertNull(props.removeProperty(key));
   }

   public void testNullProperty() throws Exception
   {
      props.putSimpleStringProperty(key, null);
      Assert.assertTrue(props.containsProperty(key));
      Assert.assertNull(props.getProperty(key));
   }

   public void testBytesPropertyWithNull() throws Exception
   {
      props.putBytesProperty(key, null);

      Assert.assertTrue(props.containsProperty(key));
      byte[] bb = (byte[])props.getProperty(key);
      Assert.assertNull(bb);
   }

   public void testTypedProperties() throws Exception
   {
      SimpleString longKey = RandomUtil.randomSimpleString();
      long longValue = RandomUtil.randomLong();
      SimpleString simpleStringKey = RandomUtil.randomSimpleString();
      SimpleString simpleStringValue = RandomUtil.randomSimpleString();
      TypedProperties otherProps = new TypedProperties();
      otherProps.putLongProperty(longKey, longValue);
      otherProps.putSimpleStringProperty(simpleStringKey, simpleStringValue);

      props.putTypedProperties(otherProps);

      long ll = props.getLongProperty(longKey);
      Assert.assertEquals(longValue, ll);
      SimpleString ss = props.getSimpleStringProperty(simpleStringKey);
      Assert.assertEquals(simpleStringValue, ss);
   }

   public void testEmptyTypedProperties() throws Exception
   {
      Assert.assertEquals(0, props.getPropertyNames().size());

      props.putTypedProperties(new TypedProperties());

      Assert.assertEquals(0, props.getPropertyNames().size());
   }

   public void testNullTypedProperties() throws Exception
   {
      Assert.assertEquals(0, props.getPropertyNames().size());

      props.putTypedProperties(null);

      Assert.assertEquals(0, props.getPropertyNames().size());
   }

   public void testEncodeDecode() throws Exception
   {
      props.putByteProperty(RandomUtil.randomSimpleString(), RandomUtil.randomByte());
      props.putBytesProperty(RandomUtil.randomSimpleString(), RandomUtil.randomBytes());
      props.putBytesProperty(RandomUtil.randomSimpleString(), null);
      props.putBooleanProperty(RandomUtil.randomSimpleString(), RandomUtil.randomBoolean());
      props.putShortProperty(RandomUtil.randomSimpleString(), RandomUtil.randomShort());
      props.putIntProperty(RandomUtil.randomSimpleString(), RandomUtil.randomInt());
      props.putLongProperty(RandomUtil.randomSimpleString(), RandomUtil.randomLong());
      props.putFloatProperty(RandomUtil.randomSimpleString(), RandomUtil.randomFloat());
      props.putDoubleProperty(RandomUtil.randomSimpleString(), RandomUtil.randomDouble());
      props.putCharProperty(RandomUtil.randomSimpleString(), RandomUtil.randomChar());
      props.putSimpleStringProperty(RandomUtil.randomSimpleString(), RandomUtil.randomSimpleString());
      props.putSimpleStringProperty(RandomUtil.randomSimpleString(), null);
      SimpleString keyToRemove = RandomUtil.randomSimpleString();
      props.putSimpleStringProperty(keyToRemove, RandomUtil.randomSimpleString());

      HornetQBuffer buffer = HornetQBuffers.dynamicBuffer(1024);
      props.encode(buffer);

      Assert.assertEquals(props.getEncodeSize(), buffer.writerIndex());

      TypedProperties decodedProps = new TypedProperties();
      decodedProps.decode(buffer);

      TypedPropertiesTest.assertEqualsTypeProperties(props, decodedProps);

      buffer.clear();

      // After removing a property, you should still be able to encode the Property
      props.removeProperty(keyToRemove);
      props.encode(buffer);

      Assert.assertEquals(props.getEncodeSize(), buffer.writerIndex());
   }

   public void testEncodeDecodeEmpty() throws Exception
   {
      TypedProperties emptyProps = new TypedProperties();

      HornetQBuffer buffer = HornetQBuffers.dynamicBuffer(1024);
      emptyProps.encode(buffer);

      Assert.assertEquals(props.getEncodeSize(), buffer.writerIndex());

      TypedProperties decodedProps = new TypedProperties();
      decodedProps.decode(buffer);

      TypedPropertiesTest.assertEqualsTypeProperties(emptyProps, decodedProps);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      props = new TypedProperties();
      key = RandomUtil.randomSimpleString();
   }

   @Override
   protected void tearDown() throws Exception
   {
      key = null;
      props = null;

      super.tearDown();
   }
}
