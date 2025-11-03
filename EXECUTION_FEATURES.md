# 🚀 Funzionalità di Esecuzione del Flowchart

## Panoramica

Sono state aggiunte importanti funzionalità di **esecuzione** al Flowchart Editor! Ora è possibile:

1. ✅ **Eseguire il programma** tutto di seguito
2. ✅ **Eseguire step-by-step** (un blocco alla volta)
3. ✅ **Visualizzare l'output** del programma
4. ✅ **Visualizzare le variabili** e i loro valori in tempo reale
5. ✅ **Evidenziare il blocco corrente** durante l'esecuzione

---

## 📦 Nuovi File Creati

### 1. **FlowchartInterpreter.java**
Interprete che esegue i blocchi del diagramma:
- Gestisce variabili e loro valori
- Esegue assegnamenti (`x = 10`, `result = x + 5`)
- Valuta condizioni (`x > 0`, `y <= 10`)
- Gestisce input/output
- Supporta loop
- Notifica l'UI ad ogni step

### 2. **ExecutionControlPanel.java**
Pannello con i controlli di esecuzione:
- **▶ Run**: Esegue tutto il programma di seguito
- **⏯ Step**: Esegue un blocco alla volta
- **⏹ Stop**: Ferma l'esecuzione
- **↻ Reset**: Resetta lo stato dell'esecuzione
- Mostra lo stato corrente dell'esecuzione

### 3. **OutputPanel.java**
Pannello per visualizzare l'output:
- Area di testo in stile terminale (nero con testo verde)
- Mostra l'output dei blocchi I/O
- Auto-scroll verso il basso
- Pulsante "Clear Output"

### 4. **VariablesPanel.java**
Pannello per visualizzare le variabili:
- Tabella con tre colonne: Variable, Value, Type
- Aggiornamento in tempo reale durante l'esecuzione
- Mostra il tipo di ogni variabile (Integer, Double, String, etc.)

---

## 🎨 Modifiche ai File Esistenti

### FlowchartPanel.java
Aggiunte funzionalità per l'evidenziazione:
- `highlightCell(Object cell)`: Evidenzia un blocco con bordo dorato
- `clearHighlight()`: Rimuove l'evidenziazione
- Getter per `graph`, `startCell`, `endCell`

### FlowchartEditorApp.java
Integrazione completa delle nuove funzionalità:
- Layout con split pane per visualizzare flowchart, output e variabili
- Connessione tra interprete e UI
- Gestione eventi dei pulsanti di esecuzione
- Dialog per richiedere input all'utente

---

## 💡 Come Funziona l'Interprete

### Blocchi Supportati

#### 1. **Process (Rettangolo Blu)**
Esegue assegnamenti e operazioni:
```
x = 10
y = x + 5
result = x * y
```

#### 2. **I/O (Cilindro Verde)**
Gestisce input e output:
```
Input: x, y       → Chiede i valori all'utente
Output: result    → Stampa il valore nell'output panel
```

#### 3. **Conditional (Diamante Giallo)**
Valuta condizioni e sceglie il branch corretto:
```
x > 0?
y <= 10?
result == 5?
```

#### 4. **Loop (Esagono Arancione)**
Ripete il corpo del loop finché la condizione è vera:
```
i < n?
count > 0?
```

### Valutazione Espressioni

L'interprete supporta:
- **Numeri**: `10`, `3.14`, `-5`
- **Variabili**: `x`, `result`, `count`
- **Operatori aritmetici**: `+`, `-`, `*`, `/`
- **Operatori di confronto**: `>`, `<`, `>=`, `<=`, `==`, `!=`
- **Espressioni composte**: `x + y * 2`, `(a + b) / 2`

### Tipi di Dato

L'interprete riconosce automaticamente:
- **Integer**: numeri interi (`10`, `-5`)
- **Double**: numeri decimali (`3.14`, `-2.5`)
- **String**: testo (tutto il resto)

---

## 🎮 Come Usare le Nuove Funzionalità

### 1. Esecuzione Completa

1. Crea un flowchart con blocchi Process, I/O, Conditional, Loop
2. Clicca il pulsante **▶ Run**
3. L'esecuzione parte automaticamente
4. Ogni blocco viene evidenziato con un bordo dorato
5. Le variabili appaiono nel pannello "Variables"
6. L'output appare nel pannello "Output"

### 2. Esecuzione Step-by-Step

1. Clicca il pulsante **⏯ Step**
2. Il primo blocco (Start) viene evidenziato
3. Clicca **Step** di nuovo per passare al blocco successivo
4. Continua a cliccare **Step** per procedere un blocco alla volta
5. Puoi vedere l'evoluzione delle variabili e dell'output ad ogni step

### 3. Stop e Reset

- **Stop**: Ferma l'esecuzione in qualsiasi momento
- **Reset**: Pulisce variabili, output ed evidenziazione

---

## 📝 Esempio di Flowchart Eseguibile

### Esempio 1: Calcolo Semplice
```
Start
  ↓
Input: n                    (chiede valore per n)
  ↓
n > 0?
  ↓ Sì                      ↓ No
  result = n * 2            result = 0
  ↓                         ↓
  ╰─────────────────────────╯
  ↓
Output: result              (stampa il risultato)
  ↓
End
```

### Esempio 2: Loop
```
Start
  ↓
Input: n, i = 0
  ↓
i < n?
  ↓ Sì            ↓ No
  Print i         Output: Done
  i = i + 1       ↓
  ↓               End
  ╰──┘ (loop back)
```

---

## 🔧 Dettagli Tecnici

### Architettura

```
FlowchartEditorApp (Main Window)
  ├── ExecutionControlPanel (North)
  │     ├── Run Button
  │     ├── Step Button
  │     ├── Stop Button
  │     └── Reset Button
  │
  └── Main Split Pane (Center)
        ├── FlowchartPanel (Left)
        │     └── Graph visualization with highlighting
        │
        └── Right Split Pane (Right)
              ├── OutputPanel (Top)
              │     └── Terminal-style output
              │
              └── VariablesPanel (Bottom)
                    └── Variables table
```

### Threading

- L'esecuzione avviene in un **thread separato** per non bloccare l'UI
- L'aggiornamento dell'UI usa `SwingUtilities.invokeLater()`
- Delay di 500ms tra ogni step per visualizzare l'esecuzione

### Listener Pattern

L'interprete notifica l'UI tramite callback:
- `onExecutionStep()`: Chiamato ad ogni step
- `onExecutionComplete()`: Chiamato quando l'esecuzione finisce
- `onExecutionError()`: Chiamato in caso di errore
- `onInputRequired()`: Chiamato quando serve input dall'utente

---

## 🚀 Build e Esecuzione

### Prerequisiti
- Java 8 o superiore
- JGraphX library (jgraphx.jar)

### Build
```bash
./build.sh
```

Lo script:
1. Scarica automaticamente JGraphX da Maven Central
2. Compila tutti i file Java
3. Crea la directory `build/` con i class file

### Run
```bash
./run.sh
```

Lo script:
1. Verifica che il progetto sia compilato
2. Avvia l'applicazione con il classpath corretto

---

## 📖 Prossimi Miglioramenti Possibili

- [ ] Salvataggio/caricamento di flowchart
- [ ] Breakpoints per debug
- [ ] Velocità di esecuzione regolabile
- [ ] History/timeline dell'esecuzione
- [ ] Supporto per array e strutture dati complesse
- [ ] Export dell'output in file
- [ ] Modalità dark/light per l'editor

---

## 🎉 Conclusione

Con queste nuove funzionalità, il Flowchart Editor diventa uno strumento completo per:
- **Creare** diagrammi a blocchi visivamente
- **Eseguire** i diagrammi come programmi
- **Debuggare** step-by-step
- **Visualizzare** variabili e output in tempo reale

Perfetto per l'insegnamento della programmazione e per la prototipazione di algoritmi!
