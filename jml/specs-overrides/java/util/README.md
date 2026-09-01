# Overlay JML — Collection / Queue / AbstractCollection

Overlay di 3 file, che disattiva i riferimenti al modello `values` (gated dietro `-RAC`) solo per questi tre tipi, lasciando intatto il resto delle specifiche bundled di OpenJML. Reversibile con `--keys=SEQSPECS`.

Spiegazione completa (perché serve, cosa risolve, cosa resta fuori dalla prova): vedi `../../docs/openjml-findings.md`.