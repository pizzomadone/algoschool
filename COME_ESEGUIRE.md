# 🚀 Come Eseguire il Flowchart Editor

## ⚠️ IMPORTANTE: Questo è un programma Java con GUI

Il Flowchart Editor è un'applicazione Java Swing con interfaccia grafica.
**Non può essere eseguito in questo ambiente server - va eseguito sul tuo computer.**

---

## 📋 Prerequisiti

- **Java Development Kit (JDK)** 8 o superiore
- **Eclipse IDE** (consigliato) OPPURE terminale con Java

---

## 🎯 Metodo 1: Usando Eclipse (CONSIGLIATO)

### Passo 1: Scaricare la libreria JGraphX

1. Vai su: https://github.com/jgraph/jgraphx/releases
2. Scarica `jgraphx.jar` (oppure usa Maven Central)
3. Salva il file nella cartella `lib/` del progetto

**Alternativa con Maven Central:**
```
https://repo1.maven.org/maven2/org/jgrapht/jgraphx-demo/1.0.0.1/jgraphx-demo-1.0.0.1.jar
```

### Passo 2: Importare il progetto in Eclipse

1. Apri Eclipse
2. File → Import → General → Existing Projects into Workspace
3. Seleziona la cartella `algoschool`
4. Click Finish

### Passo 3: Configurare JGraphX

1. Click destro sul progetto → **Build Path** → **Configure Build Path**
2. Tab **Libraries** → Click **Add External JARs...**
3. Seleziona `lib/jgraphx.jar`
4. Click **Apply and Close**

### Passo 4: Eseguire il programma

1. Apri `src/FlowchartEditorApp.java`
2. Click destro → **Run As** → **Java Application**
3. 🎉 L'applicazione si apre!

---

## 🖥️ Metodo 2: Da Terminale

### Passo 1: Scaricare JGraphX manualmente

```bash
# Opzione A: Scaricare con wget
cd lib/
wget https://repo1.maven.org/maven2/org/jgrapht/jgraphx-demo/1.0.0.1/jgraphx-demo-1.0.0.1.jar -O jgraphx.jar

# Opzione B: Scaricare con curl
curl -L -o jgraphx.jar "https://repo1.maven.org/maven2/org/jgrapht/jgraphx-demo/1.0.0.1/jgraphx-demo-1.0.0.1.jar"
```

### Passo 2: Compilare

```bash
cd algoschool/
javac -d build -cp lib/jgraphx.jar src/*.java
```

### Passo 3: Eseguire

```bash
java -cp "build:lib/jgraphx.jar" FlowchartEditorApp
```

**Su Windows:**
```bash
java -cp "build;lib\jgraphx.jar" FlowchartEditorApp
```

---

## 🎨 Come Apparirà l'Interfaccia

Quando esegui il programma, vedrai una finestra divisa in 3 sezioni:

```
┌───────────────────────────────────────────────────────────────────┐
│  Flowchart Editor                                        [_][□][X] │
├───────────────────────────────────────────────────────────────────┤
│  File  Examples  Edit  View  Help                                 │
├───────────────────────────────────────────────────────────────────┤
│  [New]  ✦ Click on EDGES (arrows) to insert blocks...            │
├───────────────────────────────────────────────────────────────────┤
│  🎮 EXECUTION CONTROLS:                                           │
│  ┌─────────────┬──────────────┬──────────┬─────────┐             │
│  │  ▶ Run All  │ ⏯ Next Step │ ⏹ Stop  │ ↻ Reset │   Ready     │
│  └─────────────┴──────────────┴──────────┴─────────┘             │
├─────────────────────────────────────┬─────────────────────────────┤
│                                     │  OUTPUT                     │
│          FLOWCHART                  │  ┌─────────────────────────┐│
│                                     │  │                         ││
│         ╔═══════╗                   │  │  [output appears here]  ││
│         ║ Start ║                   │  │                         ││
│         ╚═══╤═══╝                   │  │                         ││
│             │                       │  └─────────────────────────┘│
│         ╔═══▼═══╗                   │                             │
│         ║  End  ║                   │  VARIABLES                  │
│         ╚═══════╝                   │  ┌─────────────────────────┐│
│                                     │  │ Variable │ Value │ Type ││
│                                     │  ├──────────┼───────┼──────┤│
│                                     │  │          │       │      ││
│                                     │  └─────────────────────────┘│
└─────────────────────────────────────┴─────────────────────────────┘
│  Ready - JGraphX Version (Swing only)                            │
└───────────────────────────────────────────────────────────────────┘
```

### I 4 Pulsanti che Vedrai:

1. **🟢 ▶ Run All** (Verde)
   - Esegue tutto il programma automaticamente
   - Vedrai ogni blocco evidenziarsi in giallo uno dopo l'altro

2. **🔵 ⏯ Next Step** (Blu)
   - Click per eseguire UN BLOCCO alla volta
   - Perfetto per vedere passo-passo cosa succede

3. **🔴 ⏹ Stop** (Rosso)
   - Ferma l'esecuzione

4. **⚫ ↻ Reset** (Grigio)
   - Pulisce tutto per ricominciare

---

## 📖 Come Usare il Programma

### 1. Creare un Flowchart

Due modi:

**A) Usa un esempio pronto:**
- Menu **Examples** → Seleziona "Simple Conditional" o "Loop Example"

**B) Crea il tuo:**
1. Inizia con Start → End (già presente)
2. **Click su una freccia** (edge) tra i blocchi
3. Scegli il tipo di blocco (Process, I/O, Conditional, Loop)
4. Inserisci il testo

### 2. Eseguire il Programma

**Esecuzione Automatica:**
```
1. Click "▶ Run All"
2. Guarda l'esecuzione (ogni blocco si evidenzia)
3. Vedi output e variabili aggiornarsi
```

**Esecuzione Passo-Passo:**
```
1. Click "⏯ Next Step" → Primo blocco
2. Click "⏯ Next Step" → Secondo blocco
3. Click "⏯ Next Step" → Terzo blocco
... continua ...
```

### 3. Vedere Cosa Succede

Mentre il programma esegue:
- 💛 **Blocco corrente**: Evidenziato con bordo giallo dorato
- 📊 **Pannello Variables**: Mostra tutte le variabili e i loro valori
- 🖥️ **Pannello Output**: Mostra l'output del programma (stile terminale)

---

## 🎬 Esempio Pratico

### Esempio: Calcolo Semplice

1. Menu **Examples** → **Simple Conditional**
2. Vedrai questo flowchart:

```
Start
  ↓
Input: n           ← Chiede il numero
  ↓
n > 0?             ← Condizione
  ↓
Sì ↓         ↓ No
result=n*2   result=0
  ↓         ↓
  └────┬────┘
       ↓
Output: result     ← Stampa risultato
  ↓
End
```

3. Click **⏯ Next Step**:
   - Step 1: Start (evidenziato)
   - Step 2: Input → Ti chiede "n" → Inserisci 5
   - Step 3: Condizione "n > 0?" → Valuta a True
   - Step 4: Branch Sì → "result = 10" (5 * 2)
   - Step 5: Output → Vedi "10" nel pannello Output
   - Step 6: End → Completato!

4. Nel pannello **Variables** vedrai:
   ```
   n      | 5  | Integer
   result | 10 | Integer
   ```

5. Nel pannello **Output** vedrai:
   ```
   10
   ```

---

## ❓ Problemi Comuni

### "NoClassDefFoundError: com/mxgraph/..."
➜ JGraphX non è nel classpath. Segui i passi sopra per aggiungere la libreria.

### "java command not found"
➜ Java non è installato. Installa JDK 8 o superiore.

### "La finestra non si apre"
➜ Verifica di avere un ambiente grafico (non funziona su server senza GUI).

### "I pulsanti sono disabilitati"
➜ Alcuni pulsanti sono disabilitati in base allo stato:
- Durante **Run**: solo Stop è disponibile
- Stato **IDLE**: Run e Next Step disponibili
- Durante **Step**: tutti disponibili

---

## 📚 File di Documentazione

- **BUTTON_GUIDE.md** - Guida dettagliata ai pulsanti
- **EXECUTION_FEATURES.md** - Funzionalità di esecuzione
- **README.md** (src/) - Guida all'uso generale

---

## 🎉 Divertiti!

Una volta eseguito, avrai un editor di flowchart completo con:
- ✅ Creazione visuale di diagrammi
- ✅ Esecuzione automatica o step-by-step
- ✅ Visualizzazione variabili in tempo reale
- ✅ Output del programma
- ✅ Evidenziazione del blocco corrente

Perfetto per imparare la programmazione e gli algoritmi! 🚀

---

## 💾 Download Veloce JGraphX

Se hai problemi a scaricare JGraphX, puoi usare questo file JAR alternativo:

**jgraphx-2.0.0.1.jar** da Maven Central:
```
https://repo1.maven.org/maven2/org/tinyjee/jgraphx/jgraphx/2.0.0.1/jgraphx-2.0.0.1.jar
```

Oppure cerca "jgraphx maven" su Google e scarica da Maven Central.
