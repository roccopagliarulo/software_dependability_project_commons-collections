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
//benchmark per confrontare le prestazioni di LRUMap (Apache Commons Collections)
//rispetto a LinkedHashMap in modalita' access-order (che implementa nativamente LRU) di Java.

package org.apache.commons.collections4.benchmark;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.map.LRUMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class LRUMapBenchmark {

    private static final int CAPACITY = 1000;
    // Numero di operazioni di lookup casuali nel benchmark "misto"
    private static final int LOOKUP_OPS = 1000;

    private Map<Integer, Integer> lruMap;
    private Map<Integer, Integer> linkedHashMap;
    private Random random;

    /**
     * LinkedHashMap in access-order con removeEldestEntry sovrascritto
     * per limitare la dimensione a CAPACITY, cosi' da replicare il
     * comportamento LRU offerto nativamente da LRUMap.
     */
    private static final class BoundedLinkedHashMap extends LinkedHashMap<Integer, Integer> {
        private static final long serialVersionUID = 1L;

        BoundedLinkedHashMap(final int capacity) {
            // initialCapacity, loadFactor di default, accessOrder = true
            super(capacity, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Integer, Integer> eldest) {
            return size() > CAPACITY;
        }
    }

    @Setup(Level.Iteration)
    public void setup() {
        lruMap = new LRUMap<>(CAPACITY);
        linkedHashMap = new BoundedLinkedHashMap(CAPACITY);
        random = new Random(42);
    }

    // --- Benchmark 1: throughput puro su inserimento ---

    @Benchmark
    public void testLRUMapAdd() {
        for (int i = 0; i < CAPACITY; i++) {
            lruMap.put(i, i);
        }
    }

    @Benchmark
    public void testLinkedHashMapAdd() {
        for (int i = 0; i < CAPACITY; i++) {
            linkedHashMap.put(i, i);
        }
    }

    // --- Benchmark 2: operazioni miste, inserimento + lookup casuale ---
    // Qui emergono le differenze piu' interessanti, perche' ogni get()
    // in modalita' access-order aggiorna anche l'ordine di accesso,
    // quindi comporta un costo aggiuntivo rispetto a una semplice mappa.

    @Benchmark
    public void testLRUMapMixedInsertAndGet() {
        for (int i = 0; i < CAPACITY; i++) {
            lruMap.put(i, i);
        }
        for (int i = 0; i < LOOKUP_OPS; i++) {
            final int key = random.nextInt(CAPACITY);
            lruMap.get(key);
        }
    }

    @Benchmark
    public void testLinkedHashMapMixedInsertAndGet() {
        for (int i = 0; i < CAPACITY; i++) {
            linkedHashMap.put(i, i);
        }
        for (int i = 0; i < LOOKUP_OPS; i++) {
            final int key = random.nextInt(CAPACITY);
            linkedHashMap.get(key);
        }
    }
}
