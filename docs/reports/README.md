# Report di JaCoCo e PiTest

Questa cartella contiene snapshot permanenti dei report di coverage (JaCoCo) e mutation testing (PiTest), committati esplicitamente perché gli artifact caricati da GitHub Actions scadono dopo un periodo limitato. Servono come riferimento stabile per il report del progetto.

## baseline/

Snapshot dei report generati con la sola test suite originale del progetto.

**Risultati complessivi del progetto** (PiTest, pacchetti `queue`, `bag`, `bidimap`):
- Mutation score: 71% (804/1128), test strength 80%, line coverage 89% (classi mutate)
- `queue`: ~84% mutation coverage
- `bag`: ~70%
- `bidimap`: ~69%

**Classe `AbstractMapBag`** (target dell'esperimento Copilot):
- Mutation score: 69.3% (79/114 mutanti uccisi, 31 sopravvissuti, 4 non coperti)
- Line coverage: 98.8% (161/163 righe)

## post-copilot/

Snapshot dopo aver usato GitHub Copilot Chat per generare test JUnit mirati in `HashBagTest.java`, basati sull'analisi dei mutanti sopravvissuti di PiTest (riga, mutatore, metodo).

**Classe `AbstractMapBag`** — confronto con la baseline:

| Metodo | Mutation score baseline | Mutation score post-Copilot |
|---|---|---|
| `add` | 58.3% | 91.7% |
| `addAll` | 0.0% | 100.0% |
| `remove` | 76.5% | 76.5% *(invariato)* |
| `removeAll` | 20.0% | 80.0% |
| `retainAll` | 50.0% | 64.3% |
| `toString` | 0.0% | 80.0% |
| **Totale classe** | **69.3%** | **84.2%** |

Line coverage invariata al 98.8% (l'intervento ha migliorato la precisione degli assert, non la copertura delle righe, che era già quasi completa).

## Come rigenerare i report

```bash
# JaCoCo
mvn test jacoco:report

# PiTest (intero progetto)
mvn org.pitest:pitest-maven:mutationCoverage

# PiTest (solo una classe/test, più veloce)
mvn org.pitest:pitest-maven:mutationCoverage \
  -DtargetClasses=org.apache.commons.collections4.bag.AbstractMapBag \
  -DtargetTests=org.apache.commons.collections4.bag.HashBagTest
```