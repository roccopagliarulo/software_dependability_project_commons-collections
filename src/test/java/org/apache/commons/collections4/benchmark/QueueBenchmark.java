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
//benchmark per confrontare le prestazioni di una struttura dati Apache Commons
//(ad esempio CircularFifoQueue e TreeBag o TreeBidiMap) rispetto alle collezioni standard di Java.

package org.apache.commons.collections4.benchmark;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.queue.CircularFifoQueue;
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
public class QueueBenchmark {

    private static final int CAPACITY = 1000;
    private Queue<Integer> circularFifoQueue;
    private Queue<Integer> standardQueue;

    @Setup(Level.Iteration)
    public void setup() {
        circularFifoQueue = new CircularFifoQueue<>(CAPACITY);
        standardQueue = new ArrayDeque<>(CAPACITY);
    }

    @Benchmark
    public void testCircularFifoQueueAddAndPoll() {
        for (int i = 0; i < CAPACITY; i++) {
            circularFifoQueue.offer(i);
        }
        while (!circularFifoQueue.isEmpty()) {
            circularFifoQueue.poll();
        }
    }

    @Benchmark
    public void testStandardArrayDequeAddAndPoll() {
        for (int i = 0; i < CAPACITY; i++) {
            standardQueue.offer(i);
        }
        while (!standardQueue.isEmpty()) {
            standardQueue.poll();
        }
    }
}
