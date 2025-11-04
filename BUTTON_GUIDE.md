# 🎮 Guida ai Controlli di Esecuzione

## Panoramica dei Pulsanti

Il Flowchart Editor ha 4 pulsanti per controllare l'esecuzione del programma:

```
┌─────────────┬──────────────┬──────────┬─────────┐
│  ▶ Run All  │ ⏯ Next Step │ ⏹ Stop  │ ↻ Reset │
└─────────────┴──────────────┴──────────┴─────────┘
```

---

## 🟢 ▶ Run All - Esecuzione Automatica

**Quando usarlo:** Per eseguire tutto il programma dall'inizio alla fine.

**Comportamento:**
1. Inizia l'esecuzione dal blocco "Start"
2. Esegue automaticamente tutti i blocchi uno dopo l'altro
3. Si ferma quando raggiunge il blocco "End"
4. Durante l'esecuzione, vedrai:
   - Ogni blocco evidenziato in giallo dorato
   - Le variabili aggiornate nel pannello "Variables"
   - L'output stampato nel pannello "Output"
   - Ritardo di 500ms tra ogni blocco per seguire visivamente

**Quando è disponibile:**
- ✅ All'inizio (stato IDLE)
- ✅ Durante Step-by-Step (per continuare automaticamente)
- ❌ Durante Run (già in esecuzione)

**Cosa succede dopo:**
- I pulsanti Run e Next Step vengono disabilitati
- Solo il pulsante Stop rimane attivo
- Quando l'esecuzione finisce, torni allo stato iniziale

---

## 🔵 ⏯ Next Step - Esecuzione Passo-Passo

**Quando usarlo:** Per eseguire il programma un blocco alla volta, utile per debug e comprensione dell'algoritmo.

**Comportamento:**
1. **Primo click:** Inizia l'esecuzione dal blocco "Start"
2. **Click successivi:** Esegue il blocco successivo
3. Dopo ogni click:
   - Il blocco corrente viene evidenziato
   - Le variabili si aggiornano
   - L'output viene stampato
   - **Il programma si ferma** e aspetta il prossimo click

**Quando è disponibile:**
- ✅ All'inizio (stato IDLE)
- ✅ Durante Step-by-Step (per andare avanti)
- ❌ Durante Run (esecuzione automatica in corso)

**Workflow tipico:**
```
Click 1: Start → [evidenziato]
Click 2: Input → [chiede input]
Click 3: Process → [calcolo]
Click 4: Output → [stampa risultato]
Click 5: End → [completato]
```

**Feature speciale:**
Durante lo step-by-step, puoi cliccare **Run All** per passare all'esecuzione automatica dal punto in cui sei!

---

## 🔴 ⏹ Stop - Ferma Esecuzione

**Quando usarlo:** Per fermare l'esecuzione in qualsiasi momento.

**Comportamento:**
1. Interrompe immediatamente l'esecuzione
2. Rimuove l'evidenziazione dal blocco corrente
3. Mantiene:
   - Lo stato delle variabili
   - L'output generato fino a quel momento
4. Puoi ricominciare con Run o Next Step (ripartirà dall'inizio)

**Quando è disponibile:**
- ❌ All'inizio (niente da fermare)
- ✅ Durante Run (esecuzione automatica)
- ✅ Durante Step-by-Step

---

## ⚫ ↻ Reset - Resetta Tutto

**Quando usarlo:** Per pulire tutto e ricominciare da zero.

**Comportamento:**
1. Cancella tutte le variabili
2. Pulisce l'output
3. Rimuove l'evidenziazione
4. Torna allo stato iniziale

**Quando è disponibile:**
- ✅ Sempre (tranne durante Run)
- Durante Step-by-Step puoi fare Reset in qualsiasi momento

---

## 📊 Stati del Sistema

Il sistema ha 3 stati principali:

### 1. 🟢 IDLE (Pronto)
```
Status: "Ready"
Pulsanti disponibili: ▶ Run All, ⏯ Next Step, ↻ Reset
```
- Nessuna esecuzione in corso
- Pronto per iniziare

### 2. 🔴 RUNNING (Esecuzione Automatica)
```
Status: "Running..."
Pulsanti disponibili: ⏹ Stop
```
- Esecuzione automatica in corso
- Puoi solo fermare con Stop

### 3. 🔵 STEPPING (Passo-Passo)
```
Status: "Ready for next step - Click 'Next Step' to continue"
Pulsanti disponibili: ▶ Run All, ⏯ Next Step, ⏹ Stop, ↻ Reset
```
- In modalità step-by-step
- Massima flessibilità:
  - Click Next Step → Vai al prossimo blocco
  - Click Run All → Continua automaticamente
  - Click Stop → Ferma tutto
  - Click Reset → Ricomincia

---

## 💡 Esempi d'Uso

### Esempio 1: Esecuzione Veloce
```
1. Click "▶ Run All"
2. Guarda l'esecuzione
3. Fine!
```

### Esempio 2: Debug Passo-Passo
```
1. Click "⏯ Next Step"    → Start
2. Click "⏯ Next Step"    → Input n
3. Click "⏯ Next Step"    → Condizione n > 0
4. Click "⏯ Next Step"    → Branch True
5. Click "⏯ Next Step"    → Output
6. Click "⏯ Next Step"    → End
```

### Esempio 3: Combinazione
```
1. Click "⏯ Next Step"    → Start
2. Click "⏯ Next Step"    → Input
3. Click "⏯ Next Step"    → Prima condizione
4. Click "▶ Run All"       → Continua automaticamente da qui!
```

### Esempio 4: Fermare e Ricominciare
```
1. Click "▶ Run All"       → Esecuzione automatica
2. Click "⏹ Stop"          → Ferma a metà
3. Click "↻ Reset"         → Pulisce tutto
4. Click "⏯ Next Step"     → Ricomincia passo-passo
```

---

## 🎯 Suggerimenti

### Per l'Apprendimento
- Usa **Next Step** per capire come funziona ogni blocco
- Osserva le variabili cambiare ad ogni step
- Verifica che l'output sia quello che ti aspetti

### Per il Testing
- Usa **Next Step** per controllare valori intermedi
- Usa **Stop** se vedi qualcosa di strano
- Usa **Reset** per provare con input diversi

### Per la Dimostrazione
- Usa **Next Step** per spiegare l'algoritmo passo-passo
- Passa a **Run All** quando vuoi mostrare il risultato finale velocemente

---

## ❓ FAQ

**Q: Posso cambiare da Step a Run durante l'esecuzione?**
A: Sì! Durante lo step-by-step, clicca Run All per continuare automaticamente.

**Q: Cosa succede se clicco Stop durante lo step-by-step?**
A: L'esecuzione si ferma. Per ricominciare, clicca Next Step o Run All (ripartirà dall'inizio).

**Q: Posso modificare il flowchart durante l'esecuzione?**
A: No, ferma prima l'esecuzione con Stop, poi modifica il flowchart.

**Q: Il pulsante Next Step è disabilitato, perché?**
A: Probabilmente c'è un'esecuzione automatica (Run) in corso. Clicca Stop prima.

**Q: Come faccio a eseguire solo una parte del flowchart?**
A: Usa Next Step fino al punto che ti interessa, poi clicca Stop.

---

## 🎨 Indicatori Visivi

Durante l'esecuzione vedrai:

- **Blocco corrente**: Bordo dorato spesso
- **Status label**: Mostra lo stato corrente ("Running...", "Ready for next step", etc.)
- **Pulsanti abilitati/disabilitati**: Indicano cosa puoi fare
- **Variabili**: Si aggiornano in tempo reale
- **Output**: Appare nel pannello nero con testo verde

---

## 🚀 Inizia Subito!

1. Crea un flowchart (o usa Examples → Simple Conditional)
2. Clicca **⏯ Next Step** per provare passo-passo
3. Oppure clicca **▶ Run All** per vedere tutto in azione!

Buon divertimento con il Flowchart Editor! 🎉
