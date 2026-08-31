#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#        https://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
# ---------------------------------------------------------------------------
# Verifica JML di CircularFifoQueue con OpenJML (modalità ESC).
#
# Controlla gli 11 metodi specificati. Se tutto va bene: 11/11 provati,
# 0 fallimenti.
#
# --specs-path mette jml/specs-overrides per primo, così Collection, Queue
# e AbstractCollection usano le mie versioni modificate; tutto il resto
# usa le spec originali di OpenJML.
# ---------------------------------------------------------------------------

#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")" && pwd)"
OPENJML_HOME="$ROOT/tools/openjml-macos-arm64-21.0.27"
OPENJML="$OPENJML_HOME/openjml"
OVERRIDES="$ROOT/jml/specs-overrides"
BUNDLED="$OPENJML_HOME/specs"

METHODS="decrement,increment,size,clear,element,isEmpty,isFull,maxSize,peek,poll,remove"
SOURCES=(
  "$ROOT/src/main/java/org/apache/commons/collections4/BoundedCollection.java"
  "$ROOT/src/main/java/org/apache/commons/collections4/queue/CircularFifoQueue.java"
)

if [ ! -x "$OPENJML" ]; then
  echo "ERROR: OpenJML not found at $OPENJML" >&2
  exit 1
fi

echo "== OpenJML ESC: CircularFifoQueue (11 methods) =="
OUT="$("$OPENJML" --esc \
  --specs-path="$OVERRIDES:$BUNDLED" \
  --progress \
  -sourcepath "$ROOT/src/main/java" \
  -cp "$ROOT/src/main/java" \
  --method "$METHODS" \
  "${SOURCES[@]}" 2>&1)"
STATUS=$?

echo "$OUT" | grep -E "^Completed proof of .*CircularFifoQueue\.[a-z]" \
            | sed -E -e 's/ with prover .* - / => /' -e 's|org.apache.commons.collections4.queue.||'

PROVED=$(echo "$OUT" | grep -cE "^Completed proof of .*CircularFifoQueue\.[a-z].*no warnings")
FAILED=$(echo "$OUT" | grep -cE "verify:.*(cannot establish|Invariant)")
ERRORS=$(echo "$OUT" | grep -cE "^.*: error:")

echo
echo "proved: $PROVED/11   verification failures: $FAILED   errors: $ERRORS"

if [ "$PROVED" -eq 11 ] && [ "$FAILED" -eq 0 ] && [ "$ERRORS" -eq 0 ]; then
  echo "RESULT: PASS - all 11 methods verified by OpenJML ESC"
  exit 0
fi

echo "RESULT: FAIL"
echo "$OUT" | grep -E "verify:|error:" | head -40
exit "${STATUS:-1}"
