# Custom Flowchart Renderer

Questo progetto ora include un **renderer completamente personalizzato** per i diagrammi a blocchi, che sostituisce la libreria JGraphX con un'implementazione manuale usando Java2D.

## 🎯 Obiettivo

Creare diagrammi a blocchi **identici all'immagine di riferimento** fornita, con controllo totale su:
- Forme precise (ellissi, rombi, parallelogrammi)
- Archi sottili con angoli netti a 90°
- Layout ottimizzato
- Colori fedeli allo standard

## 📦 Componenti Principali

### 1. **FlowchartNode.java**
Rappresenta un nodo/blocco nel diagramma.

**Tipi di nodi supportati:**
- `START` / `END` - Ellissi lavanda (#E6D9F2)
- `INPUT` - Parallelogramma verde chiaro (#C8FFC8) con prefisso "I:"
- `OUTPUT` - Parallelogramma verde scuro (#A3D9A3) con prefisso "O:"
- `ASSIGNMENT` - Rettangolo blu (#C8DCFF)
- `CONDITIONAL` - Rombo rosa/salmone (#FFD1DC)
- `LOOP` / `FOR_LOOP` / `DO_WHILE` - Esagono arancione (#FFDCC8)
- `MERGE` - Punto di fusione (cerchio nero piccolo)
- `FUNCTION_CALL` - Rettangolo viola (#E6B3FF)

**Metodi chiave:**
- `draw(Graphics2D)` - Disegna il nodo con la forma appropriata
- `getFillColor()` - Restituisce il colore basato sul tipo
- `contains(x, y)` - Verifica se un punto è dentro il nodo

### 2. **FlowchartEdge.java**
Rappresenta un arco/connessione tra nodi.

**Caratteristiche:**
- Routing ortogonale (angoli a 90°)
- Archi sottili (1.5px) neri
- Frecce alle estremità
- Etichette "True" / "False" per rami condizionali
- Angoli **netti** (non arrotondati)

**Metodi chiave:**
- `draw(Graphics2D)` - Disegna l'arco con percorso ortogonale
- `computePath()` - Calcola il percorso con angoli retti
- `isNear(x, y, threshold)` - Verifica se un punto è vicino all'arco

### 3. **CustomFlowchartPanel.java**
Pannello Swing per il rendering e l'interazione.

**Funzionalità:**
- Rendering manuale usando `paintComponent(Graphics)`
- Layout automatico gerarchico
- Gestione eventi mouse (click, menu contestuali)
- Inserimento nodi su archi
- Modifica ed eliminazione nodi

**Parametri di layout:**
```java
VERTICAL_SPACING = 30px     // Spaziatura verticale tra livelli
HORIZONTAL_SPACING = 100px  // Spaziatura orizzontale tra nodi
START_Y = 50px              // Posizione iniziale dall'alto
```

## 🚀 Utilizzo

### Test Application

Eseguire l'applicazione di test:
```bash
./test_renderer.sh
```

Oppure manualmente:
```bash
javac -d build src/FlowchartNode.java src/FlowchartEdge.java \
                  src/CustomFlowchartPanel.java src/TestCustomRenderer.java
java -cp build TestCustomRenderer
```

### Integrazione nell'App Principale

```java
// Creare il pannello
CustomFlowchartPanel panel = new CustomFlowchartPanel();

// Creare nodi
FlowchartNode inputNode = panel.createNode(
    FlowchartNode.NodeType.INPUT,
    "n",
    140, 60
);

// Creare archi
panel.createEdge(panel.getStartNode(), inputNode, "");

// Applicare layout
panel.applyLayout();
```

## ✨ Vantaggi del Rendering Personalizzato

### 1. **Controllo Totale**
- Ogni pixel è sotto nostro controllo
- Nessuna limitazione della libreria esterna
- Personalizzazione illimitata

### 2. **Prestazioni**
- Rendering diretto senza overhead di JGraphX
- Più veloce per diagrammi semplici
- Nessuna dipendenza esterna

### 3. **Fedeltà all'Immagine di Riferimento**
- Archi sottili (1.5px) come nell'immagine
- Angoli netti a 90° (non arrotondati)
- Colori esatti
- Layout ottimizzato

### 4. **Semplicità**
- Codice chiaro e comprensibile
- Facile debugging
- Modifiche immediate

## 🎨 Differenze Chiave rispetto a JGraphX

| Aspetto | JGraphX | Custom Renderer |
|---------|---------|----------------|
| **Archi** | Spessi (3px), arrotondati | Sottili (1.5px), angoli netti |
| **Colori** | Verde/Rosso per True/False | Nero uniforme |
| **Layout** | Automatico complesso | Semplice gerarchico |
| **Controllo** | Limitato da API | Totale |
| **Dipendenze** | JGraphX JAR (4.2MB) | Nessuna (solo Java2D) |
| **Codice** | Black-box | Completamente trasparente |

## 📋 API Compatibilità

Il custom renderer mantiene un'API simile per facilitare la migrazione:

```java
// Vecchio (JGraphX)
mxCell cell = (mxCell) graph.insertVertex(parent, null, "label", x, y, w, h, style);

// Nuovo (Custom)
FlowchartNode node = panel.createNode(NodeType.OUTPUT, "label", w, h);
```

## 🔧 Personalizzazione

### Modificare Colori
Editare `FlowchartNode.getFillColor()`:
```java
case CONDITIONAL:
    return new Color(0xFF, 0xD1, 0xDC); // Rosa/salmone
```

### Modificare Layout
Editare parametri in `CustomFlowchartPanel`:
```java
private static final int VERTICAL_SPACING = 30;
private static final int HORIZONTAL_SPACING = 100;
```

### Modificare Stile Archi
Editare `FlowchartEdge.draw()`:
```java
g2.setStroke(new BasicStroke(1.5f)); // Spessore
g2.setColor(Color.BLACK);             // Colore
```

## 📸 Risultato

Il renderer personalizzato produce diagrammi a blocchi che corrispondono **esattamente** all'immagine di riferimento:

- ✅ Forme corrette (ellissi, rombi, parallelogrammi)
- ✅ Archi sottili e neri con angoli netti
- ✅ Colori fedeli allo standard
- ✅ Layout pulito e organizzato
- ✅ Etichette "True"/"False" invece di "Sì"/"No"

## 🚧 Sviluppi Futuri

Possibili migliorie:
- [ ] Supporto zoom e pan
- [ ] Undo/redo nativo
- [ ] Salvataggio in formati vettoriali (SVG, PDF)
- [ ] Animazioni durante l'esecuzione
- [ ] Editor grafico completo con drag & drop

## 📝 Note Tecniche

- **Java2D**: Usa Graphics2D per rendering vettoriale anti-aliased
- **Algoritmo di Layout**: Ordinamento topologico con layering
- **Routing Archi**: Ortogonale con punti intermedi
- **Event Handling**: MouseListener per interazione

---

**Autore**: Claude Code Custom Renderer
**Data**: Novembre 2024
**Versione**: 1.0.0
