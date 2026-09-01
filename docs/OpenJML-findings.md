<!-- 
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. -->

# Specifica JML e verifica OpenJML — Findings

Questo documento registra il processo di specifica JML e verifica OpenJML (Extended Static Checking) per `CircularFifoQueue`, incluso un tentativo abbandonato e la sua causa profonda. Serve come riferimento stabile per il report del progetto.

**Tool:** OpenJML `openjml-macos-arm64-21.0.27`
**Riproduzione:** `./verify_jml.sh` (termina con codice non zero se anche un solo metodo non viene provato)

## Risultato finale

11 metodi principali specificati in JML, tutti verificati senza errori né avvisi:

| Metodo | Risultato |
|---|---|
| `decrement` | ✅ Verificato |
| `increment` | ✅ Verificato |
| `element` | ✅ Verificato |
| `isEmpty` | ✅ Verificato |
| `isFull` | ✅ Verificato |
| `maxSize` | ✅ Verificato |
| `size` | ✅ Verificato *(inizialmente fallito — vedi Risoluzione)* |
| `clear` | ✅ Verificato *(idem)* |
| `peek` | ✅ Verificato *(idem)* |
| `poll` | ✅ Verificato *(idem)* |
| `remove` | ✅ Verificato *(idem)* |

**11/11 — 0 fallimenti, 0 errori.**

## Il tentativo abbandonato: modello astratto

Il primo approccio usava un campo modello `values` (sequenza logica del contenuto) collegato alla rappresentazione concreta tramite `represents`:

```java
/*@ represents values \such_that
  @   (\forall int i; 0 <= i && i < values.length;
  @        values[i] == elements[(start + i) % maxElements]);
  @*/
```

- **Tentativo A** (`represents` diretto): crash interno di OpenJML in fase di type-checking.
- **Tentativo B** (indirection tramite metodo modello puro `_at(i)`): supera il type-checking ma fallisce più avanti, nella fase di prova SMT, con un errore analogo (*sort mismatch*).

Stesso errore in due fasi diverse con due formulazioni diverse → il limite non è nella specifica, ma nel backend di OpenJML (`BasicBlocker2`), che non riesce a tradurre un accesso a un array generico `E[]` a un indice non costante dipendente da una variabile quantificata con aritmetica modulare (buffer circolare). Limite riproducibile, non aggirabile lato specifica.

**Conseguenza:** senza `represents`, i 5 metodi ereditati da `java.util.Collection`/`Queue` (`size`, `clear`, `peek`, `poll`, `remove`) portano con sé specifiche predefinite di OpenJML che referenziano proprio il modello `values` abbandonato → diventano non dimostrabili.

## La risoluzione — tre cause distinte

1. **Le clausole ereditate sono opt-out, non obbligatorie.** Nei file di specifica bundled di OpenJML, ogni riferimento al modello `values` è racchiuso in un blocco gated dalla chiave `-RAC` (attiva di default). Invece del flag globale `--keys=RAC` (che rompe le specifiche bundled di altri tipi, es. `String.jml`), è stato creato un overlay mirato via `--specs-path`: 3 file in `jml/specs-overrides/` (`Collection.jml`, `Queue.jml`, `AbstractCollection.jml`), **11 righe modificate in totale**, che ri-gatano solo i contratti (non le dichiarazioni, per non rompere altri tipi del JDK che le referenziano) da `-RAC` a `+SEQSPECS`. Reversibile con `--keys=SEQSPECS`.
2. **Il modificatore `helper` su `size()`.** Anche dopo l'overlay, `size()` falliva ancora: `helper` sopprime l'assunzione automatica dell'invariante di classe, impedendo al risolutore di escludere l'overflow aritmetico su `maxElements - start + end`. Rimosso nell'override → eliminati tutti e quattro i fallimenti residui.
3. **Un difetto reale nella specifica.** L'ultimo fallimento non era un limite dello strumento: mancava la clausola `full ==> start == end`. Senza di essa, uno stato apparentemente valido (`full=true, start=3, end=5, maxElements=10`) soddisfaceva tutte le altre condizioni ma faceva calcolare a `size()` un valore errato (2 invece di 10). Aggiunta la clausola, il metodo verifica.

## Controllo di non-vacuità

11/11 non esclude da solo che le specifiche siano tautologiche. Tre metodi già verificati sono stati mutati deliberatamente, mantenendo la specifica invariata:

| Mutazione | Rilevata da OpenJML |
|---|---|
| `size()`: `end - start` → `end - start + 1` | ✅ sì |
| `peek()`: `elements[start]` → `elements[end]` | ✅ sì |
| `isFull()`: risultato invertito | ✅ sì |

Tutte e tre le mutazioni sono state rifiutate correttamente → le specifiche discriminano comportamento corretto da scorretto.

## Cosa NON è dimostrato

L'overlay rende dimostrabili i contratti funzionali concreti, l'invariante di classe e gli obblighi impliciti di JML (null-dereference, indici, overflow) per tutti e 11 i metodi. **Non** rende dimostrabile la conformità alla semantica astratta di sequenza di `java.util.Queue` tramite `represents`: quel collegamento resta bloccato dal limite di `BasicBlocker2` (sopra), non aggirato ma reso ininfluente disattivando le clausole che ne dipendevano. La sottotipizzazione comportamentale resta comunque in vigore per le clausole non disattivate, tramite `also`.

## File nel repository

- `jml/specs-overrides/java/util/{Collection,Queue,AbstractCollection}.jml` — overlay a 3 file (11 righe), escluso da Apache RAT