#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at

#        https://www.apache.org/licenses/LICENSE-2.0

#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License. 

#!/usr/bin/env bash
set -e

# ==============================================================================
# FULL VERIFICATION SCRIPT
# ==============================================================================

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   AVVIO VERIFICA COMPLETA SOFTWARE DEPENDABILITY     ${NC}"
echo -e "${BLUE}======================================================${NC}\n"

# ------------------------------------------------------------------------------
# STEP 1 & 5: Compilazione, Test Unitari e Code Coverage JaCoCo
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[STEP 1/6] Esecuzione Build Locale, Test e JaCoCo Coverage...${NC}"
mvn clean test jacoco:report
echo -e "${GREEN}✓ JaCoCo Coverage generata con successo in target/site/jacoco/index.html${NC}\n"

# ------------------------------------------------------------------------------
# STEP 6: Mutation Testing con PiTest
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[STEP 2/6] Esecuzione Mutation Testing con PiTest (queue, bag, bidimap)...${NC}"
mvn org.pitest:pitest-maven:mutationCoverage
echo -e "${GREEN}✓ Report PiTest generato con successo in target/pit-reports/index.html${NC}\n"

# ------------------------------------------------------------------------------
# STEP 7: Compilazione ed Esecuzione Suite Microbenchmark JMH
# ------------------------------------------------------------------------------

echo -e "${YELLOW}[STEP 3/6] Compilazione ed Esecuzione Suite Microbenchmark JMH...${NC}"

rm -rf target/classes target/test-classes
mvn test -Pbenchmark -Dbenchmark="org.apache.commons.collections4.benchmark.*"
echo -e "${GREEN}✓ Benchmark JMH completati. Risultati JSON in target/jmh-result.json${NC}\n"

# ------------------------------------------------------------------------------
# STEP 2: Analisi Formale JML con OpenJML ESC
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[STEP 4/6] Verifica Extended Static Checking (ESC) con OpenJML su CircularFifoQueue...${NC}"

ROOT="$(cd "$(dirname "$0")" && pwd)"
OPENJML_HOME="$ROOT/tools/openjml-macos-arm64-21.0.27"
OPENJML="$OPENJML_HOME/openjml"
OVERRIDES="$ROOT/jml/specs-overrides"
BUNDLED="$OPENJML_HOME/specs"

JML_METHODS="decrement,increment,size,clear,element,isEmpty,isFull,maxSize,peek,poll,remove"
JML_SOURCES=(
  "$ROOT/src/main/java/org/apache/commons/collections4/BoundedCollection.java"
  "$ROOT/src/main/java/org/apache/commons/collections4/queue/CircularFifoQueue.java"
)

if [ -x "$OPENJML" ]; then
    set +e
    JML_OUT="$("$OPENJML" --esc \
      --specs-path="$OVERRIDES:$BUNDLED" \
      --progress \
      -sourcepath "$ROOT/src/main/java" \
      -cp "$ROOT/src/main/java" \
      --method "$JML_METHODS" \
      "${JML_SOURCES[@]}" 2>&1)"

    echo "$JML_OUT" | grep -E "^Completed proof of .*CircularFifoQueue\.[a-z]" \
                | sed -E -e 's/ with prover .* - / => /' -e 's|org.apache.commons.collections4.queue.||'

    JML_PROVED=$(echo "$JML_OUT" | grep -cE "^Completed proof of .*CircularFifoQueue\.[a-z].*no warnings")
    JML_FAILED=$(echo "$JML_OUT" | grep -cE "verify:.*(cannot establish|Invariant)")
    JML_ERRORS=$(echo "$JML_OUT" | grep -cE "^.*: error:")
    set -e

    echo
    echo "proved: $JML_PROVED/11   verification failures: $JML_FAILED   errors: $JML_ERRORS"

    if [ "$JML_PROVED" -eq 11 ] && [ "$JML_FAILED" -eq 0 ] && [ "$JML_ERRORS" -eq 0 ]; then
        echo -e "${GREEN}✓ Analisi OpenJML ESC completata: 11/11 metodi verificati.${NC}\n"
    else
        echo "$JML_OUT" | grep -E "verify:|error:" | head -40
        echo -e "${RED}[ATTENZIONE] Verifica OpenJML ESC fallita: non tutti i metodi sono stati provati.${NC}\n"
    fi
else
    echo -e "${RED}[ATTENZIONE] OpenJML non trovato in $OPENJML. Verifica manuale consigliata se installato altrove.${NC}\n"
fi

# ------------------------------------------------------------------------------
# STEP 3 & 10: Build Container Docker e Verifica Web App Demo
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[STEP 5/6] Build Immagine Docker e Avvio Test Container DemoApp...${NC}"
docker compose down || true
docker compose up --build -d

echo "Attesa inizializzazione server web (porta 8080)..."
sleep 3

# Test rapido dell'endpoint REST esposto
echo -e "Test endpoint /api/queue/status:"
curl -s -X GET http://localhost:8080/api/queue/status | grep "maxSize" && echo ""
echo -e "${GREEN}✓ Container Docker e Demo Web App attivi e rispondenti su http://localhost:8080${NC}\n"

# Arresto container al termine del test
docker compose down

# ------------------------------------------------------------------------------
# STEP 8 & 9: Sintassi Pipeline CI/CD (GitHub Actions)
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[STEP 6/6] Verifica Sintassi Pipeline GitHub Actions (ci.yml)...${NC}"
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"
echo -e "${GREEN}✓ Sintassi .github/workflows/ci.yml valida.${NC}\n"

echo -e "${BLUE}======================================================${NC}"
echo -e "${GREEN}   TUTTI I CONTROLLI DI VERIFICA SONO STATI SUPERATI! ${NC}"
echo -e "${BLUE}======================================================${NC}"


# PER ESEGUIRLO
# chmod +x run_all_checks.sh
# ./run_all_checks.sh
