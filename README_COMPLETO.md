# 🎨 Flowchart Editor con Esecuzione

**Un editor di diagrammi a blocchi completo con esecuzione interattiva!**

## ⚠️ IMPORTANTE

Questo è un **programma Java con interfaccia grafica (GUI)** che deve essere eseguito sul tuo computer.

👉 **Non vedrai i pulsanti qui su GitHub - devi eseguirlo localmente!**

---

## 🎯 Cosa Fa Questo Programma

Un editor visuale per creare e **eseguire** diagrammi a blocchi (flowchart):

### ✨ Funzionalità

1. **Crea diagrammi visualmente**
   - Click sulle frecce per inserire blocchi
   - Tipi: Process, I/O, Conditional (IF), Loop
   - Layout automatico gerarchico

2. **Esegui i tuoi algoritmi!** ⭐ **NOVITÀ!**
   - 🟢 **Run All**: Esecuzione automatica completa
   - 🔵 **Next Step**: Passo-passo (un click = un blocco)
   - 🔴 **Stop**: Ferma in qualsiasi momento
   - ⚫ **Reset**: Ricomincia da capo

3. **Visualizza in tempo reale**
   - 💛 Blocco corrente evidenziato
   - 📊 Pannello Variables (nome, valore, tipo)
   - 🖥️ Pannello Output (stile terminale)

---

## 🚀 Quick Start

### Passo 1: Scarica il progetto

```bash
git clone https://github.com/pizzomadone/algoschool.git
cd algoschool
```

### Passo 2: Scarica JGraphX

Scarica la libreria JGraphX in `lib/jgraphx.jar`:
- Da [Maven Central](https://repo1.maven.org/maven2/org/jgrapht/jgraphx-demo/1.0.0.1/)
- O da [GitHub Releases](https://github.com/jgraph/jgraphx/releases)

### Passo 3: Compila ed Esegui

**Con Eclipse:**
1. Import → Existing Projects
2. Add External JARs → `lib/jgraphx.jar`
3. Run `FlowchartEditorApp.java`

**Da terminale:**
```bash
javac -d build -cp lib/jgraphx.jar src/*.java
java -cp "build:lib/jgraphx.jar" FlowchartEditorApp
```

👉 **Guida dettagliata:** Leggi `COME_ESEGUIRE.md`

---

## 🎮 I 4 Pulsanti di Esecuzione

### 🟢 ▶ Run All
Esegue tutto il programma automaticamente dall'inizio alla fine.
- Ogni blocco si evidenzia in giallo
- Velocità: 500ms per blocco
- Vedi output e variabili aggiornarsi

### 🔵 ⏯ Next Step
**Ogni click esegue UN SOLO BLOCCO!**
- Click 1 → Start
- Click 2 → Primo blocco
- Click 3 → Secondo blocco
- ... continua ...
- Perfetto per debug e apprendimento

### 🔴 ⏹ Stop
Ferma l'esecuzione (automatica o step-by-step).
- Mantiene stato, variabili e output
- Per ricominciare, usa Run o Next Step

### ⚫ ↻ Reset
Pulisce tutto per ricominciare da zero.
- Cancella variabili
- Pulisce output
- Rimuove evidenziazione

**💡 BONUS:** Durante step-by-step puoi cliccare Run All per continuare automaticamente!

---

## 📸 Come Appare

```
╔════════════════════════════════════════════════════════════╗
║  🎮 EXECUTION CONTROLS                                     ║
║  ╔═══════════╗ ╔═════════════╗ ╔════════╗ ╔═══════╗      ║
║  ║  ▶ Run    ║ ║ ⏯ Next Step ║ ║ ⏹ Stop ║ ║ ↻ Reset║      ║
║  ║   All     ║ ║             ║ ║        ║ ║       ║      ║
║  ╚═══════════╝ ╚═════════════╝ ╚════════╝ ╚═══════╝      ║
║     [VERDE]       [BLU]          [ROSSO]    [GRIGIO]      ║
╠════════════════════════════════════╤═══════════════════════╣
║        FLOWCHART                   │   OUTPUT              ║
║                                    │   ┌─────────────────┐ ║
║      ╔═══════╗                     │   │ 10              │ ║
║      ║ Start ║ ← [Evidenziato!]   │   │ Result OK       │ ║
║      ╚═══╤═══╝                     │   └─────────────────┘ ║
║          │                         │                       ║
║      ┌───▼───┐                     │   VARIABLES           ║
║      │Input:n│                     │   ┌─────┬─────┬─────┐║
║      └───┬───┘                     │   │ n   │  5  │ Int │║
║          │                         │   │resul│ 10  │ Int │║
║       ◆──▼──◆                      │   └─────┴─────┴─────┘║
║      ╱ n>0? ╲                      │                       ║
║     ◆────────◆                     │                       ║
║    ╱          ╲                    │                       ║
║  ...          ...                  │                       ║
╚════════════════════════════════════╧═══════════════════════╝
```

👉 **Vedi `INTERFACE_PREVIEW.txt` per un'anteprima completa!**

---

## 📖 Esempio: Calcolo Semplice

1. **Menu Examples → Simple Conditional**

2. Vedrai questo flowchart:
```
Start → Input: n → [n > 0?] → result = n*2 / result = 0 → Output: result → End
```

3. **Click "Next Step"** ripetutamente:
   - Step 1: Start (evidenziato)
   - Step 2: Input → Inserisci `5`
   - Step 3: Condizione → Valuta `5 > 0` = True
   - Step 4: Branch Sì → `result = 10`
   - Step 5: Output → Vedi `10` nel pannello
   - Step 6: End → Completato!

4. **Pannelli aggiornati:**
   - Variables: `n=5`, `result=10`
   - Output: `10`

---

## 📚 Documentazione

- **`COME_ESEGUIRE.md`** → Guida completa all'installazione ed esecuzione
- **`BUTTON_GUIDE.md`** → Guida dettagliata ai 4 pulsanti
- **`INTERFACE_PREVIEW.txt`** → Anteprima ASCII dell'interfaccia
- **`EXECUTION_FEATURES.md`** → Dettagli tecnici sulle funzionalità
- **`src/README.md`** → Guida all'uso dell'editor

---

## 🏗️ Architettura

### File Principali

```
src/
├── FlowchartEditorApp.java         # Finestra principale
├── FlowchartPanel.java              # Area del flowchart
├── FlowchartInterpreter.java       # ⭐ Interprete per esecuzione
├── ExecutionControlPanel.java      # ⭐ Pannello pulsanti
├── OutputPanel.java                 # ⭐ Pannello output
└── VariablesPanel.java              # ⭐ Pannello variabili

(⭐ = Nuovi file per l'esecuzione)
```

### Come Funziona

```
User clicks "Next Step"
        ↓
ExecutionControlPanel → onStep()
        ↓
FlowchartEditorApp → interpreter.step()
        ↓
FlowchartInterpreter:
  - Legge il blocco corrente
  - Esegue l'operazione (assegnamento, I/O, condizione)
  - Aggiorna variabili
  - Notifica listener
        ↓
FlowchartPanel → highlightCell() [bordo giallo]
VariablesPanel → updateVariables()
OutputPanel → appendOutput()
```

---

## 🎓 Perfetto Per

- ✅ **Insegnamento** - Mostra visivamente come funziona un algoritmo
- ✅ **Apprendimento** - Comprendi passo-passo ogni operazione
- ✅ **Debug** - Vedi dove il tuo algoritmo sbaglia
- ✅ **Prototipazione** - Testa algoritmi velocemente
- ✅ **Presentazioni** - Mostra il funzionamento in modo chiaro

---

## 🔧 Requisiti

- **Java JDK 8+** (testato con JDK 8, 11, 17)
- **JGraphX library** (per la visualizzazione dei grafi)
- **Ambiente grafico** (non funziona su server headless)

---

## ❓ FAQ

**Q: Dove sono i pulsanti?**
A: Devi eseguire il programma Java sul tuo computer. Non sono visibili qui su GitHub.

**Q: Posso usarlo online?**
A: No, è un'applicazione desktop Java Swing. Serve Java installato.

**Q: Funziona su Windows/Mac/Linux?**
A: Sì! Java è cross-platform.

**Q: Come faccio a vedere solo parte dell'esecuzione?**
A: Usa "Next Step" e clicca fino al punto che ti interessa, poi "Stop".

**Q: Posso passare da step-by-step a esecuzione automatica?**
A: Sì! Durante step-by-step, clicca "Run All" per continuare automaticamente.

**Q: Supporta loop e condizioni?**
A: Sì! Supporta Process, I/O, Conditional (IF), e Loop (WHILE).

---

## 🎉 Features

### Creazione Diagrammi

- ✅ Click su frecce per inserire blocchi
- ✅ Tipi: Start, End, Process, I/O, Conditional, Loop
- ✅ Layout automatico gerarchico
- ✅ Editing inline (F2 o double-click)
- ✅ Delete per rimuovere blocchi
- ✅ Esempi predefiniti

### Esecuzione

- ✅ Modalità automatica (Run All)
- ✅ Modalità passo-passo (Next Step)
- ✅ Stop in qualsiasi momento
- ✅ Reset per ricominciare
- ✅ Evidenziazione blocco corrente
- ✅ Visualizzazione variabili in tempo reale
- ✅ Output stile terminale
- ✅ Input dialog per richiedere valori
- ✅ Delay configurabile tra step

### Interprete

- ✅ Assegnamenti: `x = 10`, `result = x + y`
- ✅ Operazioni: `+`, `-`, `*`, `/`
- ✅ Confronti: `>`, `<`, `>=`, `<=`, `==`, `!=`
- ✅ Input/Output: `Input: x`, `Output: result`
- ✅ Condizioni: `x > 0?`, `y != 5?`
- ✅ Loop: ripete finché condizione è vera
- ✅ Tipi: Integer, Double, String (auto-detect)

---

## 🚧 Possibili Miglioramenti Futuri

- [ ] Salvataggio/caricamento flowchart (JSON/XML)
- [ ] Breakpoints per debug
- [ ] Velocità esecuzione regolabile
- [ ] History/timeline dell'esecuzione
- [ ] Array e strutture dati complesse
- [ ] Export output in file
- [ ] Dark/Light theme
- [ ] Export flowchart come immagine PNG/SVG

---

## 👨‍💻 Sviluppo

Progetto sviluppato con:
- **Java Swing** per l'interfaccia grafica
- **JGraphX** per la visualizzazione dei grafi
- **Threading** per esecuzione non bloccante
- **Listener pattern** per comunicazione tra componenti

---

## 📄 Licenza

Progetto educativo open source.

---

## 🎊 Inizia Subito!

1. Clona il repo
2. Scarica JGraphX
3. Compila
4. Esegui `FlowchartEditorApp`
5. Menu Examples → Simple Conditional
6. Click "⏯ Next Step" e guarda la magia! ✨

**Buon divertimento con il Flowchart Editor!** 🚀

---

## 📞 Supporto

Problemi? Leggi la documentazione:
- `COME_ESEGUIRE.md` per problemi di esecuzione
- `BUTTON_GUIDE.md` per capire i pulsanti
- `INTERFACE_PREVIEW.txt` per vedere l'interfaccia

---

**Made with ❤️ for learning and teaching programming!**
