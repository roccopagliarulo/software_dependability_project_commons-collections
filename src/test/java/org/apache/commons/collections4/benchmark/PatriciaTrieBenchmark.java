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
//benchmark per confrontare le prestazioni di PatriciaTrie (Apache Commons Collections)
//rispetto a TreeMap<String, V> di Java, in particolare su inserimento, lookup singolo
//e ricerca per prefisso (dove un Trie dovrebbe mostrare il suo punto di forza).

package org.apache.commons.collections4.benchmark;

import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.trie.PatriciaTrie;
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
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PatriciaTrieBenchmark {

    private static final int CAPACITY = 1000;
    private static final int LOOKUP_OPS = 1000;

    // Prefissi "realistici" usati per generare le chiavi, cosi' la ricerca
    // per prefisso ha effettivamente piu' risultati da restituire.
    private static final String[] PREFIXES = { "user_", "order_", "product_", "invoice_" };

    private Map<String, Integer> patriciaTrie;
    private Map<String, Integer> treeMap;
    private String[] keys;
    private Random random;

    @Setup(Level.Iteration)
    public void setup() {
        patriciaTrie = new PatriciaTrie<>();
        treeMap = new TreeMap<>();
        random = new Random(42);

        keys = new String[CAPACITY];
        for (int i = 0; i < CAPACITY; i++) {
            final String prefix = PREFIXES[i % PREFIXES.length];
            keys[i] = prefix + i;
        }
    }

    // --- Benchmark 1: throughput puro su inserimento ---

    @Benchmark
    public void testPatriciaTrieAdd() {
        for (int i = 0; i < CAPACITY; i++) {
            patriciaTrie.put(keys[i], i);
        }
    }

    @Benchmark
    public void testTreeMapAdd() {
        for (int i = 0; i < CAPACITY; i++) {
            treeMap.put(keys[i], i);
        }
    }

    // --- Benchmark 2: operazioni miste, inserimento + lookup singolo casuale ---

    @Benchmark
    public void testPatriciaTrieMixedInsertAndGet() {
        for (int i = 0; i < CAPACITY; i++) {
            patriciaTrie.put(keys[i], i);
        }
        for (int i = 0; i < LOOKUP_OPS; i++) {
            final String key = keys[random.nextInt(CAPACITY)];
            patriciaTrie.get(key);
        }
    }

    @Benchmark
    public void testTreeMapMixedInsertAndGet() {
        for (int i = 0; i < CAPACITY; i++) {
            treeMap.put(keys[i], i);
        }
        for (int i = 0; i < LOOKUP_OPS; i++) {
            final String key = keys[random.nextInt(CAPACITY)];
            treeMap.get(key);
        }
    }

    // --- Benchmark 3: ricerca per prefisso ---
    // Qui e' dove il Trie dovrebbe mostrare il suo vantaggio strutturale:
    // PatriciaTrie offre prefixMap() nativamente, mentre su TreeMap va
    // simulata con subMap() sfruttando l'ordinamento lessicografico delle chiavi.

    @Benchmark
    public int testPatriciaTriePrefixSearch() {
        for (int i = 0; i < CAPACITY; i++) {
            patriciaTrie.put(keys[i], i);
        }
        int total = 0;
        for (final String prefix : PREFIXES) {
            final SortedMap<String, Integer> matches = ((PatriciaTrie<Integer>) patriciaTrie).prefixMap(prefix);
            total += matches.size();
        }
        return total;
    }

    @Benchmark
    public int testTreeMapPrefixSearch() {
        for (int i = 0; i < CAPACITY; i++) {
            treeMap.put(keys[i], i);
        }
        final TreeMap<String, Integer> sortedMap = (TreeMap<String, Integer>) treeMap;
        int total = 0;
        for (final String prefix : PREFIXES) {
            // Trucco standard per simulare una ricerca per prefisso su TreeMap:
            // subMap(prefix, prefix + Character.MAX_VALUE)
            final SortedMap<String, Integer> matches =
                    sortedMap.subMap(prefix, prefix + Character.MAX_VALUE);
            total += matches.size();
        }
        return total;
    }
}
