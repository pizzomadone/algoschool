import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window for the Flowchart Editor using JGraphX.
 * Uses only Swing and JGraphX library (no JavaFX).
 */
public class FlowchartEditorApp extends JFrame {

    private JTabbedPane tabbedPane;
    private FlowchartPanel mainFlowchartPanel;
    private Map<String, FlowchartPanel> functionPanels;  // functionName -> panel
    private FlowchartPanel currentFlowchartPanel;
    private ExecutionControlPanel controlPanel;
    private OutputPanel outputPanel;
    private CCodePanel cCodePanel;
    private VariablesPanel variablesPanel;
    private FlowchartInterpreter interpreter;

    public FlowchartEditorApp() {
        setTitle("Flowchart Editor - JGraphX Version");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        initializeComponents();
        setupMenuBar();
        setupToolbar();

        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeComponents() {
        // Initialize function panels map
        functionPanels = new HashMap<>();

        // Create main flowchart panel
        mainFlowchartPanel = new FlowchartPanel();
        currentFlowchartPanel = mainFlowchartPanel;

        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Main", createScrollPaneForPanel(mainFlowchartPanel));

        // Add tab change listener to update interpreter when switching tabs
        tabbedPane.addChangeListener(e -> onTabChanged());

        // Add mouse listener for right-click on tabs to delete functions
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex >= 0) {
                        String tabTitle = tabbedPane.getTitleAt(tabIndex);
                        // Only show delete option for function tabs (not Main)
                        if (!"Main".equals(tabTitle)) {
                            showTabContextMenu(e, tabIndex, tabTitle);
                        }
                    }
                }
            }
        });

        // Create execution panels
        controlPanel = new ExecutionControlPanel();
        outputPanel = new OutputPanel();
        cCodePanel = new CCodePanel();
        variablesPanel = new VariablesPanel();

        // Create interpreter with mainFlowchartPanel reference for function support
        interpreter = new FlowchartInterpreter(
            mainFlowchartPanel.getGraph(),
            mainFlowchartPanel.getStartCell(),
            mainFlowchartPanel.getEndCell(),
            mainFlowchartPanel
        );

        // Setup interpreter listener
        setupInterpreter();

        // Setup control panel listener
        setupControlPanel();

        // Setup graph model listener for real-time C code generation
        setupGraphListener();

        // Create right panel with output, C code, and variables (3 panels in vertical)
        // Top: Output panel
        // Middle: C Code panel
        // Bottom: Variables panel
        JSplitPane middleSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cCodePanel, variablesPanel);
        middleSplitPane.setDividerLocation(250);
        middleSplitPane.setResizeWeight(0.5);

        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputPanel, middleSplitPane);
        rightSplitPane.setDividerLocation(200);
        rightSplitPane.setResizeWeight(0.33);

        // Create main split pane with tabbed pane on the left
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, rightSplitPane);
        // Pannello destro: 1400 - 750 = 650px
        mainSplitPane.setDividerLocation(750);
        mainSplitPane.setResizeWeight(750.0 / 1400.0);  // = 0.536

        // Layout - DON'T add controlPanel here yet, will be added in setupToolbar()
        add(mainSplitPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Creates a scroll pane for a flowchart panel
     */
    private JScrollPane createScrollPaneForPanel(FlowchartPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    /**
     * Called when a tab is changed
     */
    private void onTabChanged() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        String tabTitle = tabbedPane.getTitleAt(selectedIndex);

        // Save previous tab's graph to main panel if it was a function
        if (currentFlowchartPanel != null && currentFlowchartPanel != mainFlowchartPanel) {
            // Find the function name for current panel
            String currentFunctionName = null;
            for (Map.Entry<String, FlowchartPanel> entry : functionPanels.entrySet()) {
                if (entry.getValue() == currentFlowchartPanel) {
                    currentFunctionName = entry.getKey();
                    break;
                }
            }

            // Sync function graph back to main panel
            if (currentFunctionName != null) {
                syncFunctionToMain(currentFunctionName, currentFlowchartPanel);
            }
        }

        // Update current flowchart panel
        if ("Main".equals(tabTitle)) {
            currentFlowchartPanel = mainFlowchartPanel;
        } else {
            currentFlowchartPanel = functionPanels.get(tabTitle);

            // Sync function graph from main panel to function panel
            syncFunctionFromMain(tabTitle, currentFlowchartPanel);
        }

        // Update interpreter to use the new panel's graph
        if (currentFlowchartPanel != null) {
            interpreter = new FlowchartInterpreter(
                currentFlowchartPanel.getGraph(),
                currentFlowchartPanel.getStartCell(),
                currentFlowchartPanel.getEndCell(),
                mainFlowchartPanel  // Always pass main panel for function access
            );

            setupInterpreter();
            setupGraphListener();
            updateCCode();

            // Update title
            setTitle("Flowchart Editor - " + tabTitle);
        }
    }

    /**
     * Syncs a function's graph from its panel to the main panel
     */
    private void syncFunctionToMain(String functionName, FlowchartPanel functionPanel) {
        FunctionDefinition funcDef = mainFlowchartPanel.getFunction(functionName);
        if (funcDef != null) {
            // Copy the graph from function panel to function definition
            funcDef.setFunctionGraph(functionPanel.getGraph());
            funcDef.setStartCell(functionPanel.getStartCell());
            funcDef.setEndCell(functionPanel.getEndCell());
        }
    }

    /**
     * Syncs a function's graph from the main panel to its panel
     */
    private void syncFunctionFromMain(String functionName, FlowchartPanel functionPanel) {
        // Currently the function panel is created fresh, so no need to sync on first creation
        // This method is here for future enhancements where we might reload tabs
    }

    private void setupInterpreter() {
        interpreter.setExecutionListener(new FlowchartInterpreter.ExecutionListener() {
            @Override
            public void onExecutionStep(Object cell, Map<String, Object> variables, String output) {
                // Update UI
                SwingUtilities.invokeLater(() -> {
                    currentFlowchartPanel.highlightCell(cell);
                    variablesPanel.updateVariables(variables);
                    outputPanel.setOutput(output);
                });

                // Add delay for visualization only in automatic mode
                // In step-by-step mode, the user controls the pace
                if (!interpreter.isPaused() && interpreter.isRunning()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            @Override
            public void onExecutionComplete() {
                SwingUtilities.invokeLater(() -> {
                    currentFlowchartPanel.clearHighlight();
                    controlPanel.setStatus("Execution completed");
                    controlPanel.setState(ExecutionControlPanel.ExecutionState.IDLE);
                });
            }

            @Override
            public void onExecutionError(String error) {
                SwingUtilities.invokeLater(() -> {
                    currentFlowchartPanel.clearHighlight();
                    controlPanel.setStatus("Error: " + error);
                    controlPanel.setState(ExecutionControlPanel.ExecutionState.IDLE);
                    JOptionPane.showMessageDialog(
                        FlowchartEditorApp.this,
                        error,
                        "Execution Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                });
            }

            @Override
            public void onInputRequired(String variableName, FlowchartInterpreter.InputCallback callback) {
                SwingUtilities.invokeLater(() -> {
                    String input = JOptionPane.showInputDialog(
                        FlowchartEditorApp.this,
                        "Enter value for: " + variableName,
                        "Input Required",
                        JOptionPane.QUESTION_MESSAGE
                    );

                    if (input != null) {
                        callback.onInputProvided(input);
                    } else {
                        // User cancelled
                        interpreter.stop();
                    }
                });
            }
        });
    }

    private void setupGraphListener() {
        // Aggiungi un listener al modello del grafo per aggiornare il codice C in real-time
        currentFlowchartPanel.getGraph().getModel().addListener(com.mxgraph.util.mxEvent.CHANGE,
            (sender, evt) -> {
                // Aggiorna il codice C quando il grafo cambia
                SwingUtilities.invokeLater(() -> updateCCode());
            }
        );

        // Genera il codice iniziale
        SwingUtilities.invokeLater(() -> updateCCode());
    }

    private void setupControlPanel() {
        controlPanel.setExecutionControlListener(new ExecutionControlPanel.ExecutionControlListener() {
            @Override
            public void onRun() {
                // Se siamo in modalità stepping, continua da dove eravamo
                if (controlPanel.getState() == ExecutionControlPanel.ExecutionState.STEPPING) {
                    // Passa da stepping a running: continua l'esecuzione automatica
                    controlPanel.setStatus("Running...");
                    controlPanel.setState(ExecutionControlPanel.ExecutionState.RUNNING);

                    new Thread(() -> {
                        // Riprendi esecuzione automatica
                        while (interpreter.isRunning() &&
                               interpreter.getCurrentCell() != currentFlowchartPanel.getEndCell()) {
                            interpreter.step();
                        }

                        // Esecuzione completata
                        SwingUtilities.invokeLater(() -> {
                            currentFlowchartPanel.clearHighlight();
                            controlPanel.setStatus("Execution completed");
                            controlPanel.setState(ExecutionControlPanel.ExecutionState.IDLE);
                        });
                    }).start();

                } else {
                    // Nuova esecuzione - reset UI
                    outputPanel.clear();
                    variablesPanel.clear();
                    currentFlowchartPanel.clearHighlight();

                    // Update interpreter with current graph state
                    interpreter = new FlowchartInterpreter(
                        currentFlowchartPanel.getGraph(),
                        currentFlowchartPanel.getStartCell(),
                        currentFlowchartPanel.getEndCell()
                    );
                    setupInterpreter();

                    // Start execution in background thread
                    controlPanel.setStatus("Running...");
                    controlPanel.setState(ExecutionControlPanel.ExecutionState.RUNNING);

                    new Thread(() -> {
                        interpreter.start();
                    }).start();
                }
            }

            @Override
            public void onStep() {
                if (!interpreter.isRunning()) {
                    // First step - reset UI
                    outputPanel.clear();
                    variablesPanel.clear();
                    currentFlowchartPanel.clearHighlight();

                    // Update interpreter
                    interpreter = new FlowchartInterpreter(
                        currentFlowchartPanel.getGraph(),
                        currentFlowchartPanel.getStartCell(),
                        currentFlowchartPanel.getEndCell()
                    );
                    setupInterpreter();

                    controlPanel.setStatus("Step-by-step mode - Click 'Next Step' to continue");
                } else {
                    controlPanel.setStatus("Executing step...");
                }

                // Imposta stato stepping
                controlPanel.setState(ExecutionControlPanel.ExecutionState.STEPPING);

                new Thread(() -> {
                    interpreter.step();

                    SwingUtilities.invokeLater(() -> {
                        if (!interpreter.isRunning() ||
                            interpreter.getCurrentCell() == currentFlowchartPanel.getEndCell()) {
                            // Esecuzione completata
                            currentFlowchartPanel.clearHighlight();
                            controlPanel.setStatus("Execution completed");
                            controlPanel.setState(ExecutionControlPanel.ExecutionState.IDLE);
                        } else {
                            // Pronto per il prossimo step
                            controlPanel.setStatus("Ready for next step - Click 'Next Step' to continue");
                            controlPanel.setState(ExecutionControlPanel.ExecutionState.STEPPING);
                        }
                    });
                }).start();
            }

            @Override
            public void onStop() {
                interpreter.stop();
                currentFlowchartPanel.clearHighlight();
                controlPanel.setStatus("Stopped");
                controlPanel.setState(ExecutionControlPanel.ExecutionState.IDLE);
            }

            @Override
            public void onReset() {
                interpreter.reset();
                currentFlowchartPanel.clearHighlight();
                outputPanel.clear();
                variablesPanel.clear();
                controlPanel.setStatus("Ready");
                controlPanel.setState(ExecutionControlPanel.ExecutionState.IDLE);
            }
        });
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
        newItem.addActionListener(e -> newFlowchart());
        fileMenu.add(newItem);

        fileMenu.addSeparator();

        JMenuItem saveItem = new JMenuItem("Save...");
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> saveFlowchart());
        fileMenu.add(saveItem);

        JMenuItem loadItem = new JMenuItem("Load...");
        loadItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        loadItem.addActionListener(e -> loadFlowchart());
        fileMenu.add(loadItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke("control Q"));
        exitItem.addActionListener(e -> exitApplication());
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // Examples menu
        JMenu examplesMenu = new JMenu("Examples");
        examplesMenu.setMnemonic('E');

        JMenuItem simpleExample = new JMenuItem("Simple Conditional");
        simpleExample.addActionListener(e -> loadExample(1));
        examplesMenu.add(simpleExample);

        JMenuItem loopExample = new JMenuItem("Loop Example");
        loopExample.addActionListener(e -> loadExample(2));
        examplesMenu.add(loopExample);

        JMenuItem nestedExample = new JMenuItem("Nested Conditional");
        nestedExample.addActionListener(e -> loadExample(3));
        examplesMenu.add(nestedExample);

        menuBar.add(examplesMenu);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');

        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke("control Z"));
        undoItem.addActionListener(e -> currentFlowchartPanel.undo());
        editMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke("control Y"));
        redoItem.addActionListener(e -> currentFlowchartPanel.redo());
        editMenu.add(redoItem);

        editMenu.addSeparator();

        JMenuItem deleteItem = new JMenuItem("Delete Selected");
        deleteItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
        deleteItem.addActionListener(e -> currentFlowchartPanel.deleteSelected());
        editMenu.add(deleteItem);

        JMenuItem editLabelItem = new JMenuItem("Edit Label");
        editLabelItem.setAccelerator(KeyStroke.getKeyStroke("F2"));
        editLabelItem.addActionListener(e -> currentFlowchartPanel.editSelectedLabel());
        editMenu.add(editLabelItem);

        menuBar.add(editMenu);

        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');

        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke("control PLUS"));
        zoomInItem.addActionListener(e -> currentFlowchartPanel.zoomIn());
        viewMenu.add(zoomInItem);

        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke("control MINUS"));
        zoomOutItem.addActionListener(e -> currentFlowchartPanel.zoomOut());
        viewMenu.add(zoomOutItem);

        JMenuItem resetZoomItem = new JMenuItem("Reset Zoom");
        resetZoomItem.setAccelerator(KeyStroke.getKeyStroke("control 0"));
        resetZoomItem.addActionListener(e -> currentFlowchartPanel.resetZoom());
        viewMenu.add(resetZoomItem);

        menuBar.add(viewMenu);

        // Functions menu
        JMenu functionsMenu = new JMenu("Functions");
        functionsMenu.setMnemonic('F');

        JMenuItem newFunctionItem = new JMenuItem("New Function...");
        newFunctionItem.setAccelerator(KeyStroke.getKeyStroke("control shift F"));
        newFunctionItem.addActionListener(e -> createNewFunction());
        functionsMenu.add(newFunctionItem);

        menuBar.add(functionsMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        JMenuItem helpItem = new JMenuItem("How to Use");
        helpItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        helpItem.addActionListener(e -> showHelpDialog());
        helpMenu.add(helpItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void setupToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // New button
        JButton newBtn = new JButton("New");
        newBtn.setToolTipText("Create new flowchart (Start -> End)");
        newBtn.addActionListener(e -> newFlowchart());
        toolBar.add(newBtn);

        toolBar.addSeparator();

        // New Function button
        JButton newFunctionBtn = new JButton("➕ New Function");
        newFunctionBtn.setToolTipText("Create a new function (Ctrl+Shift+F)");
        newFunctionBtn.setFont(newFunctionBtn.getFont().deriveFont(Font.BOLD));
        newFunctionBtn.setForeground(new Color(0, 0, 150));
        newFunctionBtn.addActionListener(e -> createNewFunction());
        toolBar.add(newFunctionBtn);

        toolBar.addSeparator();

        // Create Undo and Redo buttons
        JButton undoBtn = new JButton("← Indietro");
        undoBtn.setToolTipText("Undo last change (Ctrl+Z)");

        JButton redoBtn = new JButton("Avanti →");
        redoBtn.setToolTipText("Redo last undone change (Ctrl+Y)");

        // Add action listeners
        undoBtn.addActionListener(e -> {
            currentFlowchartPanel.undo();
            updateUndoRedoButtons(undoBtn, redoBtn);
        });

        redoBtn.addActionListener(e -> {
            currentFlowchartPanel.redo();
            updateUndoRedoButtons(undoBtn, redoBtn);
        });

        toolBar.add(undoBtn);
        toolBar.add(redoBtn);

        // Update button states initially
        updateUndoRedoButtons(undoBtn, redoBtn);

        toolBar.addSeparator();

        // Instructions label - NEW INTERACTION MODEL
        JLabel infoLabel = new JLabel(" ✦ Click on EDGES (arrows) to insert blocks | Double-click blocks to edit | Right-click for menu ");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));
        infoLabel.setForeground(new Color(0, 100, 0));
        toolBar.add(infoLabel);

        // Create a panel to hold both toolbar and execution controls
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(toolBar, BorderLayout.NORTH);
        northPanel.add(controlPanel, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel();
        statusBar.setLayout(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JLabel statusLabel = new JLabel("Ready - JGraphX Version (Swing only)");
        statusBar.add(statusLabel, BorderLayout.WEST);

        return statusBar;
    }

    private void newFlowchart() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Create new flowchart? This will clear the current diagram.",
            "New Flowchart",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.OK_OPTION) {
            currentFlowchartPanel.clearFlowchart();
        }
    }

    private void loadExample(int exampleNumber) {
        switch (exampleNumber) {
            case 1:
                currentFlowchartPanel.createSimpleConditionalExample();
                break;
            case 2:
                currentFlowchartPanel.createLoopExample();
                break;
            case 3:
                currentFlowchartPanel.createNestedConditionalExample();
                break;
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(
            this,
            "Flowchart Editor v1.0 - JGraphX Version\n\n" +
            "A flowchart editor built with JGraphX and Swing.\n\n" +
            "Features:\n" +
            "• Multiple block types (Process, Conditional, I/O, Loop)\n" +
            "• Hierarchical automatic layout\n" +
            "• Interactive editing\n" +
            "• Zoom and pan support\n\n" +
            "Built with Java Swing and JGraphX library!",
            "About Flowchart Editor",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showHelpDialog() {
        String help = "══════════════════════════════════════════════════════\n" +
                "       FLOWCHART EDITOR - QUICK START GUIDE\n" +
                "══════════════════════════════════════════════════════\n\n" +
                "CORE CONCEPT - Click on EDGES, not blocks!\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "The flowchart always starts with: Start → End\n\n" +
                "To add blocks:\n" +
                "1. CLICK ON AN EDGE (arrow) between blocks\n" +
                "2. Select the block type (Process, IF, I/O, Loop)\n" +
                "3. Enter the block text\n" +
                "4. The block is inserted in the middle!\n\n" +
                "CONDITIONAL (IF) BLOCKS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "When you insert a Conditional block:\n" +
                "• A diamond shape is created\n" +
                "• Two branches (Sì/No) automatically appear\n" +
                "• Both branches merge at a point (black dot)\n" +
                "• Click on these branch edges to add more blocks!\n\n" +
                "CONDITION SYNTAX:\n" +
                "• Comparison: x > 5, n <= 10, age == 18, name != \"Bob\"\n" +
                "• AND: x > 0 AND y < 10  (also: &&, &)\n" +
                "• OR: x < 0 OR x > 100  (also: ||, |)\n" +
                "• NOT: NOT x > 0  (also: !)\n" +
                "• Parentheses: (x > 0 AND y > 0) OR z == 1\n\n" +
                "NESTED IFs:\n" +
                "• Click on a Sì or No branch edge\n" +
                "• Insert another Conditional block\n" +
                "• The layout automatically reorganizes!\n\n" +
                "EDITING\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Double-click a block → Edit its text (or press F2)\n" +
                "• Click to select a block\n" +
                "• Press Delete → Remove selected block\n" +
                "• Right-click edge → Quick insert menu\n" +
                "• Right-click block → Edit/Delete menu\n\n" +
                "NAVIGATION\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Mouse wheel → Zoom in/out\n" +
                "• Ctrl+Plus/Minus → Zoom in/out\n" +
                "• Ctrl+0 → Reset zoom\n" +
                "• Blocks are FIXED (cannot be moved manually)\n\n" +
                "KEYBOARD SHORTCUTS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Ctrl+N → New flowchart\n" +
                "• Delete → Delete selected block\n" +
                "• F2 → Edit label\n" +
                "• F1 → Show this help\n\n" +
                "BLOCK TYPES\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Assignment: Blue rectangle (x = 5, result = x + y)\n" +
                "• Conditional (IF): Yellow diamond (x > 0?, n == 5?)\n" +
                "• Input: Green parallelogram with bold I: (enter: n)\n" +
                "• Output: Green parallelogram with bold O: (enter: result or \"Hello\")\n" +
                "• Loop: Orange hexagon (i < n?, while conditions)\n" +
                "• Start/End: Gray rounded rectangle (fixed)\n" +
                "• Merge Point: Black dot (automatic for IFs)\n\n" +
                "INPUT/OUTPUT FORMAT:\n" +
                "• Input block: Write only the variable name (e.g., \"n\")\n" +
                "  → The bold I: appears automatically on the left\n" +
                "• Output block: Write the expression or string\n" +
                "  → For variables: \"result\" or \"x + y\"\n" +
                "  → For strings: \"Hello\" or \"The value is\"\n" +
                "  → The bold O: appears automatically on the left\n\n" +
                "TIPS & TRICKS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "✓ Use Examples menu to see pre-built flowcharts\n" +
                "✓ Right-click → Re-apply Layout to reorganize\n" +
                "✓ Start and End blocks cannot be deleted\n" +
                "✓ The layout automatically adjusts when you add blocks\n" +
                "✓ True branches are GREEN, False branches are RED\n\n" +
                "EXAMPLE WORKFLOW\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "1. Start with: Start → End\n" +
                "2. Click the edge between Start and End\n" +
                "3. Insert \"Input Block\" and enter: number\n" +
                "   → You'll see: [I:] ╱number╲\n" +
                "4. Click edge after Input block\n" +
                "5. Insert \"Conditional\" and enter: number > 0\n" +
                "   → Two branches (Sì/No) appear automatically!\n" +
                "6. Click the Sì (green) branch\n" +
                "7. Insert \"Assignment\": result = number * 2\n" +
                "8. Click the No (red) branch\n" +
                "9. Insert \"Assignment\": result = 0\n" +
                "10. After the merge, add Output block: result\n" +
                "    → You'll see: [O:] ╱result╲\n" +
                "11. Click ▶ Run All or ⏯ Next Step to execute!\n\n" +
                "Ready to create flowcharts! 🎨";

        JTextArea textArea = new JTextArea(help);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 600));

        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            "Flowchart Editor - Help",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void saveFlowchart() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Flowchart");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Flowchart Files (*.xml)", "xml"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Add .xml extension if not present
            if (!file.getName().toLowerCase().endsWith(".xml")) {
                file = new File(file.getAbsolutePath() + ".xml");
            }

            try {
                currentFlowchartPanel.saveFlowchart(file);
                JOptionPane.showMessageDialog(
                    this,
                    "Flowchart saved successfully to:\n" + file.getAbsolutePath(),
                    "Save Successful",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Error saving flowchart:\n" + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                );
                ex.printStackTrace();
            }
        }
    }

    private void loadFlowchart() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Load a flowchart? This will replace the current diagram.",
            "Load Flowchart",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.OK_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load Flowchart");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Flowchart Files (*.xml)", "xml"));

            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                try {
                    currentFlowchartPanel.loadFlowchart(file);

                    // Reset execution state
                    interpreter = new FlowchartInterpreter(
                        currentFlowchartPanel.getGraph(),
                        currentFlowchartPanel.getStartCell(),
                        currentFlowchartPanel.getEndCell()
                    );
                    setupInterpreter();

                    outputPanel.clear();
                    variablesPanel.clear();
                    controlPanel.setStatus("Ready");
                    controlPanel.setState(ExecutionControlPanel.ExecutionState.IDLE);

                    // Aggiorna il codice C dopo il caricamento
                    updateCCode();

                    JOptionPane.showMessageDialog(
                        this,
                        "Flowchart loaded successfully from:\n" + file.getAbsolutePath(),
                        "Load Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Error loading flowchart:\n" + ex.getMessage(),
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    ex.printStackTrace();
                }
            }
        }
    }

    private void exitApplication() {
        System.exit(0);
    }

    /**
     * Update the enabled state of undo/redo buttons based on availability
     */
    private void updateUndoRedoButtons(JButton undoBtn, JButton redoBtn) {
        undoBtn.setEnabled(currentFlowchartPanel.canUndo());
        redoBtn.setEnabled(currentFlowchartPanel.canRedo());
    }

    /**
     * Aggiorna il codice C generato dal flowchart corrente
     */
    private void updateCCode() {
        try {
            FlowchartToCGenerator generator = new FlowchartToCGenerator(
                currentFlowchartPanel.getGraph(),
                currentFlowchartPanel.getStartCell(),
                currentFlowchartPanel.getEndCell()
            );
            String code = generator.generateCode();
            cCodePanel.setCode(code);
        } catch (Exception e) {
            cCodePanel.setCode("// Errore nella generazione del codice:\n// " + e.getMessage());
        }
    }

    // ===== FUNCTION MANAGEMENT METHODS =====

    private void createNewFunction() {
        String functionName = JOptionPane.showInputDialog(
            this,
            "Enter function name:",
            "New Function",
            JOptionPane.PLAIN_MESSAGE
        );

        if (functionName == null || functionName.trim().isEmpty()) {
            return; // User cancelled
        }

        functionName = functionName.trim();

        // Validate function name
        if (!functionName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            JOptionPane.showMessageDialog(
                this,
                "Invalid function name. Use only letters, numbers, and underscores.\nMust start with a letter or underscore.",
                "Invalid Name",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Check if function already exists
        if ("Main".equalsIgnoreCase(functionName) || functionPanels.containsKey(functionName)) {
            JOptionPane.showMessageDialog(
                this,
                "Function '" + functionName + "' already exists or name is reserved.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Create function in main panel (to keep track of all functions)
        if (mainFlowchartPanel.createFunction(functionName)) {
            // Create a new FlowchartPanel for this function
            FlowchartPanel functionPanel = new FlowchartPanel();

            // Store the function panel
            functionPanels.put(functionName, functionPanel);

            // Add a new tab
            tabbedPane.addTab(functionName, createScrollPaneForPanel(functionPanel));

            // Switch to the new tab
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

            JOptionPane.showMessageDialog(
                this,
                "Function '" + functionName + "' created successfully!\nYou can now design it using flowchart blocks.\n\nUse INPUT blocks at the beginning to define parameters.\nUse RETURN block to return a value.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to create function.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Shows a context menu when right-clicking on a function tab
     */
    private void showTabContextMenu(MouseEvent e, int tabIndex, String functionName) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem deleteItem = new JMenuItem("🗑️ Delete Function '" + functionName + "'");
        deleteItem.addActionListener(ev -> deleteFunction(tabIndex, functionName));
        popup.add(deleteItem);

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Deletes a function and its tab
     */
    private void deleteFunction(int tabIndex, String functionName) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete function '" + functionName + "'?\nThis action cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Delete function from main panel
        if (mainFlowchartPanel.deleteFunction(functionName)) {
            // Remove from function panels map
            functionPanels.remove(functionName);

            // Remove the tab
            tabbedPane.removeTabAt(tabIndex);

            // If we just deleted the current tab, switch to Main
            if (currentFlowchartPanel != mainFlowchartPanel && !functionPanels.containsValue(currentFlowchartPanel)) {
                tabbedPane.setSelectedIndex(0); // Switch to Main tab
            }

            JOptionPane.showMessageDialog(
                this,
                "Function '" + functionName + "' deleted successfully.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to delete function '" + functionName + "'.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlowchartEditorApp app = new FlowchartEditorApp();
            app.setVisible(true);
        });
    }
}
