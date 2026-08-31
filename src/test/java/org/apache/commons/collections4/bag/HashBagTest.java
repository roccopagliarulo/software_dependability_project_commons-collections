/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.bag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InvalidObjectException;

import org.apache.commons.collections4.Bag;
import org.junit.jupiter.api.Test;

/**
 * Extension of {@link AbstractBagTest} for exercising the {@link HashBag}
 * implementation.
 */
public class HashBagTest<T> extends AbstractBagTest<T> {

    @Override
    public String getCompatibilityVersion() {
        return "4";
    }

    @Override
    protected int getIterationBehaviour() {
        return UNORDERED;
    }

    @Override
    public Bag<T> makeObject() {
        return new HashBag<>();
    }

    @Test
    void testAddReturnsTrueForNewElementAndIncrementsByOne() {
        final HashBag<String> bag = new HashBag<>();
        final int before = bag.getCount("A");

        assertTrue(bag.add("A"));
        assertEquals(before + 1, bag.getCount("A"));
    }

    @Test
    void testAddReturnsFalseForExistingElementAndIncrementsByOne() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        final int before = bag.getCount("A");

        assertFalse(bag.add("A"));
        assertEquals(before + 1, bag.getCount("A"));
    }

    @Test
    void testAddAllReturnsTrueWhenBagChanges() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");

        final boolean changed = bag.addAll(java.util.Arrays.asList("B", "C", "A"));

        assertTrue(changed);
        assertEquals(2, bag.getCount("A"));
        assertEquals(1, bag.getCount("B"));
        assertEquals(1, bag.getCount("C"));
    }

    @Test
    void testAddAllReturnsFalseForEmptyOrDuplicateOnlyCollection() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");

        assertFalse(bag.addAll(java.util.Collections.emptyList()));
        assertFalse(bag.addAll(java.util.Arrays.asList("A", "A")));
        assertEquals(3, bag.getCount("A"));
    }

    @Test
    void testRemoveDecrementsCountByOneWhenGreaterThanOne() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("A");

        assertTrue(bag.remove("A", 1));
        assertEquals(1, bag.getCount("A"));
        assertTrue(bag.contains("A"));
    }

    @Test
    void testRemoveRemovesElementWhenCountReachesZero() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");

        assertTrue(bag.remove("A", 1));
        assertEquals(0, bag.getCount("A"));
        assertFalse(bag.contains("A"));
        assertFalse(bag.uniqueSet().contains("A"));
    }

    @Test
    void testRemoveReturnsFalseForAbsentElement() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");

        assertFalse(bag.remove("B", 1));
        assertEquals(1, bag.getCount("A"));
    }

    @Test
    void testRemoveAllReturnsTrueWhenElementsAreRemoved() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("A");
        bag.add("B");
        bag.add("C");

        final boolean changed = bag.removeAll(java.util.Arrays.asList("A", "A", "B", "C"));

        assertTrue(changed);
        assertEquals(0, bag.getCount("A"));
        assertEquals(0, bag.getCount("B"));
        assertEquals(0, bag.getCount("C"));
    }

    @Test
    void testRemoveAllReturnsFalseWhenCollectionHasNoIntersection() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("B");

        final boolean changed = bag.removeAll(java.util.Arrays.asList("C", "D"));

        assertFalse(changed);
        assertEquals(1, bag.getCount("A"));
        assertEquals(1, bag.getCount("B"));
    }

    @Test
    void testRetainAllWithExactSubsetRemovesOnlyExcessElements() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("A");
        bag.add("B");
        bag.add("C");

        final boolean changed = bag.retainAll(java.util.Arrays.asList("A", "B"));

        assertTrue(changed);
        assertEquals(1, bag.getCount("A"));
        assertEquals(1, bag.getCount("B"));
        assertEquals(0, bag.getCount("C"));
        assertEquals(2, bag.size());
    }

    @Test
    void testRetainAllWithDisjointCollectionClearsBag() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("B");

        final boolean changed = bag.retainAll(java.util.Arrays.asList("C", "D"));

        assertTrue(changed);
        assertEquals(0, bag.size());
        assertEquals(0, bag.getCount("A"));
        assertEquals(0, bag.getCount("B"));
    }

    @Test
    void testRetainAllWithFullBagCollectionReturnsFalse() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("A");
        bag.add("B");

        final boolean changed = bag.retainAll(java.util.Arrays.asList("A", "B"));

        assertTrue(changed);
        assertEquals(1, bag.getCount("A"));
        assertEquals(1, bag.getCount("B"));
        assertEquals(2, bag.size());
    }

    @Test
    void testRetainAllWithEmptyCollectionEmptiesBag() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("B");

        final boolean changed = bag.retainAll(java.util.Collections.emptyList());

        assertTrue(changed);
        assertEquals(0, bag.size());
        assertEquals(0, bag.getCount("A"));
        assertEquals(0, bag.getCount("B"));
    }

    @Test
    void testToStringOnEmptyBag() {
        final HashBag<String> bag = new HashBag<>();
        assertEquals("[]", bag.toString());
    }

    @Test
    void testToStringShowsCountsForEachElement() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("A");
        bag.add("A");
        bag.add("B");
        bag.add("C");
        bag.add("C");
        bag.add("C");

        final String toString = bag.toString();
        assertTrue(toString.contains("2:A") || toString.contains("A:2"));
        assertTrue(toString.contains("1:B") || toString.contains("B:1"));
        assertTrue(toString.contains("3:C") || toString.contains("C:3"));
        assertTrue(toString.startsWith("["));
        assertTrue(toString.endsWith("]"));
    }

    @Test
    void testAddClampsCountAndSizeToIntegerMaxValue() {
        final HashBag<String> bag = new HashBag<>();
        bag.add("X", Integer.MAX_VALUE - 1);
        bag.add("Y", Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE, bag.size());
        bag.add("X", 10);
        assertEquals(Integer.MAX_VALUE, bag.getCount("X"));
        assertEquals(Integer.MAX_VALUE, bag.size());
        assertEquals(2, bag.uniqueSet().size());
        // true size is 2 * Integer.MAX_VALUE - 11 after this, so size() must stay saturated
        bag.remove("X", 10);
        assertEquals(Integer.MAX_VALUE - 10, bag.getCount("X"));
        assertEquals(Integer.MAX_VALUE, bag.size());
        // size() only drops below Integer.MAX_VALUE once the true size does
        bag.remove("Y");
        assertEquals(Integer.MAX_VALUE - 10, bag.size());
    }

    @Test
    void testDeserializeRejectsNonPositiveCount() throws Exception {
        final int marker = 0x11223344;
        final HashBag<String> bag = new HashBag<>();
        bag.add("X", marker);
        final byte[] byteArray = serialize(bag);
        for (final int count : new int[] { 0, -7 }) {
            final byte[] bytes = byteArray.clone();
            replaceInt(bytes, marker, count);
            assertThrows(InvalidObjectException.class, () -> deserialize(bytes));
        }
    }

//    void testCreate() throws Exception {
//        Bag<T> bag = makeObject();
//        writeExternalFormToDisk((java.io.Serializable) bag, "src/test/resources/data/test/HashBag.emptyCollection.version4.obj");
//        bag = makeFullCollection();
//        writeExternalFormToDisk((java.io.Serializable) bag, "src/test/resources/data/test/HashBag.fullCollection.version4.obj");
//    }
}
