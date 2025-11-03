# Guida Installazione in Eclipse - Passo per Passo

## ⚠️ Problema Comune: NoClassDefFoundError

Se ottieni l'errore `java.lang.NoClassDefFoundError: com/mxgraph/swing/mxGraphComponent`,
significa che la libreria JGraphX non è nel classpath.

## 📋 Soluzione - Configurazione Corretta in Eclipse

### Opzione 1: Configurare la Source Folder (RACCOMANDATO)

1. **Apri Eclipse** e vai al tuo progetto `grafico`

2. **Click destro sulla cartella `src_jgraphx`** → **Build Path** → **Use as Source Folder**

3. **Verifica che JGraphX sia nel Build Path del progetto**:
   - Click destro sul progetto `grafico` → **Properties**
   - Vai a **Java Build Path** → tab **Libraries**
   - Verifica che `jgraphx.jar` sia presente nella lista
   - Se NON c'è:
     - Click **Add External JARs...** (o **Add JARs...** se è già nel progetto)
     - Seleziona il file `jgraphx.jar`
     - Click **OK**

4. **Pulisci e Ricompila**:
   - Menu **Project** → **Clean...**
   - Seleziona il progetto `grafico`
   - Click **OK**

5. **Esegui**:
   - Apri `FlowchartEditorApp.java`
   - Click destro → **Run As** → **Java Application**

### Opzione 2: Creare un Nuovo Source Folder

Se l'Opzione 1 non funziona:

1. **Click destro sul progetto** → **New** → **Source Folder**
2. Nome: `src_jgraphx`
3. Click **Finish**
4. **Copia i file Java** da `src_jgraphx` nella nuova source folder
5. Segui i passi 3-5 dell'Opzione 1

### Opzione 3: Mettere i file nella cartella `src` esistente

Se preferisci non creare una nuova source folder:

1. **Crea un package** nella cartella `src` esistente: `jgraphx` o `flowchart`
2. **Copia i file** `FlowchartEditorApp.java` e `FlowchartPanel.java` in questo package
3. **Aggiungi la dichiarazione del package** all'inizio di ogni file:
   ```java
   package jgraphx; // o package flowchart;
   ```
4. Segui i passi 3-5 dell'Opzione 1

## 🔍 Verifica che JGraphX sia Installato Correttamente

Crea questo file di test per verificare:

**File: TestJGraphX.java**
```java
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class TestJGraphX {
    public static void main(String[] args) {
        System.out.println("Testing JGraphX...");
        try {
            mxGraph graph = new mxGraph();
            System.out.println("✓ JGraphX is working!");
            System.out.println("✓ mxGraph class found: " + graph.getClass().getName());
        } catch (Exception e) {
            System.err.println("✗ JGraphX NOT working!");
            e.printStackTrace();
        }
    }
}
```

Esegui questo file. Se vedi "✓ JGraphX is working!", allora la libreria è configurata correttamente.

## 📦 Download JGraphX (se non ce l'hai)

Se non hai ancora scaricato JGraphX:

1. Vai su: https://github.com/jgraph/jgraphx
2. Oppure scarica direttamente: https://github.com/jgraph/jgraphx/releases
3. Estrai il file ZIP
4. Trova `jgraphx.jar` nella cartella `lib/`
5. Copialo nel tuo progetto (esempio: nella cartella `lib/` del progetto)
6. Aggiungi al Build Path come descritto sopra

## 🐛 Troubleshooting

### Errore persiste dopo aver aggiunto il JAR?

1. **Verifica il JAR**:
   - Apri **Project** → **Properties** → **Java Build Path** → **Libraries**
   - Espandi `jgraphx.jar`
   - Dovresti vedere le classi di JGraphX

2. **Riavvia Eclipse**:
   - A volte Eclipse ha bisogno di un riavvio per riconoscere nuove librerie

3. **Verifica la JDK**:
   - Assicurati di usare Java 8 o superiore
   - **Window** → **Preferences** → **Java** → **Installed JREs**

4. **Clean e Rebuild**:
   - **Project** → **Clean...**
   - **Project** → **Build All**

## ✅ Checklist Finale

- [ ] Cartella `src_jgraphx` configurata come Source Folder
- [ ] File `jgraphx.jar` presente nel Build Path
- [ ] Progetto pulito e ricompilato
- [ ] Test `TestJGraphX` eseguito con successo
- [ ] `FlowchartEditorApp` si avvia senza errori

## 📧 Ancora Problemi?

Se l'errore persiste, verifica:
- Quale versione di JGraphX stai usando?
- Dove si trova esattamente il file `jgraphx.jar`?
- Hai altri errori di compilazione in Eclipse?

Fammi sapere e ti aiuterò a risolvere!
