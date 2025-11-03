# Flowchart Editor - JGraphX Version

Questa è una versione del Flowchart Editor che usa **JGraphX** (JGraph 6) e **solo Swing** (no JavaFX).

## ⚠️ IMPORTANTE: Se ottieni errori NoClassDefFoundError

Se ottieni l'errore `NoClassDefFoundError: com/mxgraph/swing/mxGraphComponent`,
**leggi il file `INSTALLAZIONE_ECLIPSE.md`** per istruzioni dettagliate!

## Installazione Rapida in Eclipse

### Passo 0: TEST PRIMA DI TUTTO! ⭐

**ESEGUI PRIMA IL TEST** per verificare che JGraphX sia configurato:
1. Apri il file `TestJGraphX.java`
2. Fai clic destro → **Run As** → **Java Application**
3. Se vedi "✓✓✓ ALL TESTS PASSED!" → JGraphX funziona! Vai al Passo 3
4. Se ottieni errori → Segui il Passo 1

### Passo 1: Configurare src_jgraphx come Source Folder

1. In Eclipse, fai clic destro sulla cartella `src_jgraphx`
2. Seleziona **Build Path** → **Use as Source Folder**

### Passo 2: Verificare che JGraphX sia nel Build Path

1. Fai clic destro sul progetto → **Properties**
2. Vai a **Java Build Path** → tab **Libraries**
3. Verifica che `jgraphx.jar` sia presente
4. Se NON c'è:
   - Clicca **Add External JARs...** (o **Add JARs...** se è già nel progetto)
   - Seleziona il file `jgraphx.jar`
   - Clicca **Apply and Close**

### Passo 3: Pulire e Ricompilare

1. Menu **Project** → **Clean...**
2. Seleziona il progetto
3. Click **OK**

### Passo 4: Eseguire il programma

1. Apri il file `FlowchartEditorApp.java`
2. Fai clic destro → **Run As** → **Java Application**
3. L'applicazione dovrebbe partire!

### 🔧 Problemi?

Se hai problemi, leggi **`INSTALLAZIONE_ECLIPSE.md`** per istruzioni dettagliate e troubleshooting!

## Struttura del Progetto

```
src_jgraphx/
├── FlowchartEditorApp.java        # Applicazione principale con menu e toolbar
├── FlowchartPanel.java            # Pannello con JGraphX per visualizzare il flowchart
├── TestJGraphX.java               # ⭐ Test per verificare che JGraphX funzioni
├── README.md                      # Questo file (guida rapida)
└── INSTALLAZIONE_ECLIPSE.md       # Guida dettagliata e troubleshooting
```

## Caratteristiche

- ✅ **Solo Swing** - Nessuna dipendenza da JavaFX
- ✅ **JGraphX** - Usa la libreria JGraphX per i grafi
- ✅ **Tipi di blocchi**:
  - Process (Rettangolo blu)
  - Conditional (Diamante giallo)
  - I/O (Cilindro verde - rappresenta storage/input-output)
  - Loop (Esagono arancione)
  - Start/End (Rettangolo arrotondato grigio)
- ✅ **Layout automatico** - Layout gerarchico automatico
- ✅ **Editing interattivo**:
  - Click per selezionare
  - Double-click per editare il testo
  - Right-click per menu contestuale
  - Delete per eliminare
- ✅ **Zoom e Pan**:
  - Rotellina del mouse per zoom
  - Ctrl+Click e trascina per pan
  - Ctrl+Plus/Minus per zoom in/out
- ✅ **Esempi predefiniti** - Menu Examples con flowchart di esempio

## 🎯 Come Usare - NUOVO SISTEMA!

### ⭐ Concetto Chiave: Click sugli ARCHI, non sui blocchi!

Il flowchart **inizia sempre** con:  `Start → End`

### Aggiungere Blocchi

1. **CLICCA su un ARCO** (freccia) tra due blocchi
2. Scegli il tipo di blocco da inserire (Process, Conditional, I/O, Loop)
3. Inserisci il testo del blocco
4. **Il blocco viene inserito nel mezzo dell'arco!**

### Blocchi Condizionali (IF)

Quando inserisci un **Conditional**:
- Viene creato un **rombo** con la condizione
- **Due rami** (True/False) appaiono automaticamente
- I rami si **uniscono in un punto** (pallino nero)
- **Puoi cliccare sugli archi dei rami** per aggiungere altri blocchi!

### IF Annidati

1. Clicca sull'arco True o False di un IF
2. Inserisci un altro Conditional
3. Il layout si **riorganizza automaticamente**!

### Editare Blocchi

- **Double-click** su un blocco → Modifica testo
- **Click** → Seleziona
- **Delete** → Elimina
- **Right-click su arco** → Inserisci blocco qui
- **Right-click su blocco** → Menu Edit/Delete

### Navigazione

- **Mouse wheel** → Zoom in/out
- **Trascina blocchi** → Sposta manualmente
- **Ctrl+Plus/Minus** → Zoom in/out
- **Ctrl+0** → Reset zoom

### Menu

- **File → New** - Nuovo flowchart vuoto
- **File → Exit** - Esci
- **Examples** - Carica flowchart di esempio
- **Edit** - Modifica/elimina blocchi
- **View** - Zoom in/out/reset
- **Help** - Aiuto e informazioni

## Differenze dalla Versione Originale

La versione originale (cartella `src`) disegna i blocchi manualmente usando Graphics2D.

Questa versione (cartella `src_jgraphx`) usa JGraphX che:
- ✅ Gestisce automaticamente il rendering dei blocchi
- ✅ Fornisce zoom e pan integrati
- ✅ Ha layout automatico gerarchico
- ✅ Permette di spostare i blocchi con drag & drop
- ✅ Gestisce automaticamente le connessioni

## Requisiti

- Java 8 o superiore
- JGraphX (jgraphx.jar) - libreria già importata in Eclipse
- Nessuna altra dipendenza esterna

## Note

- Questa versione non salva/carica file (versione semplificata)
- Il layout automatico può essere riapplicato dal menu contestuale
- I blocchi possono essere spostati manualmente trascinandoli

## Esempi Disponibili

1. **Simple Conditional** - Esempio con un singolo if/else
2. **Loop Example** - Esempio con un ciclo while
3. **Nested Conditional** - Esempio con condizionali annidati

Buon divertimento con il Flowchart Editor! 🎨
