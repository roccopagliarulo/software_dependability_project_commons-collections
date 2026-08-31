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
//benchmark per confrontare le prestazioni di TreeBidiMap (Apache Commons Collections)
//rispetto a due HashMap standard di Java mantenute manualmente sincronizzate
//per ottenere lo stesso comportamento di mappa bidirezionale.

package org.apache.commons.collections4.benchmark;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.bidimap.TreeBidiMap;
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
public class TreeBidiMapBenchmark {

    private static final int CAPACITY = 1000;
    private static final int LOOKUP_OPS = 1000;

    private TreeBidiMap<Integer, Integer> treeBidiMap;

    // Simulazione "manuale" di una bidi map con due HashMap standard,
    // una per la direzione key->value e una per value->key.
    private Map<Integer, Integer> forwardMap;
    private Map<Integer, Integer> inverseMap;

    private Random random;

    @Setup(Level.Iteration)
    public void setup() {
        treeBidiMap = new TreeBidiMap<>();
        forwardMap = new HashMap<>(CAPACITY);
        inverseMap = new HashMap<>(CAPACITY);
        random = new Random(42);
    }

    // --- Benchmark 1: throughput puro su inserimento ---

    @Benchmark
    public void testTreeBidiMapAdd() {
        for (int i = 0; i < CAPACITY; i++) {
            treeBidiMap.put(i, i * 2);
        }
    }

    @Benchmark
    public void testManualBidiMapAdd() {
        for (int i = 0; i < CAPACITY; i++) {
            final int value = i * 2;
            forwardMap.put(i, value);
            inverseMap.put(value, i);
        }
    }

    // --- Benchmark 2: operazioni miste, inserimento + lookup in entrambe le direzioni ---
    // E' qui che dovrebbe emergere il vantaggio "concettuale" della bidi map:
    // TreeBidiMap offre getKey()/get() nativamente su un'unica struttura,
    // mentre la versione manuale deve tenere sincronizzate due mappe separate.

    @Benchmark
    public void testTreeBidiMapMixedInsertAndGet() {
        for (int i = 0; i < CAPACITY; i++) {
            treeBidiMap.put(i, i * 2);
        }
        for (int i = 0; i < LOOKUP_OPS; i++) {
            final int key = random.nextInt(CAPACITY);
            treeBidiMap.get(key);
            treeBidiMap.getKey(key * 2);
        }
    }

    @Benchmark
    public void testManualBidiMapMixedInsertAndGet() {
        for (int i = 0; i < CAPACITY; i++) {
            final int value = i * 2;
            forwardMap.put(i, value);
            inverseMap.put(value, i);
        }
        for (int i = 0; i < LOOKUP_OPS; i++) {
            final int key = random.nextInt(CAPACITY);
            forwardMap.get(key);
            inverseMap.get(key * 2);
        }
    }

    // --- Benchmark 3: rimozione tramite valore (inverseBidiMap) ---
    // TreeBidiMap supporta nativamente la rimozione per valore tramite la vista
    // invertita; con due HashMap manuali servono due remove() coordinate
    // e bisogna ricordarsi il valore associato prima di rimuoverlo.

    @Benchmark
    public void testTreeBidiMapRemoveByValue() {
        for (int i = 0; i < CAPACITY; i++) {
            treeBidiMap.put(i, i * 2);
        }
        for (int i = 0; i < CAPACITY; i++) {
            treeBidiMap.removeValue(i * 2);
        }
    }

    @Benchmark
    public void testManualBidiMapRemoveByValue() {
        for (int i = 0; i < CAPACITY; i++) {
            final int value = i * 2;
            forwardMap.put(i, value);
            inverseMap.put(value, i);
        }
        for (int i = 0; i < CAPACITY; i++) {
            final int value = i * 2;
            final Integer key = inverseMap.remove(value);
            if (key != null) {
                forwardMap.remove(key);
            }
        }
    }
}
