import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * Main application window for the Flowchart Editor using JGraphX.
 * Uses only Swing and JGraphX library (no JavaFX).
 */
public class FlowchartEditorApp extends JFrame {

    private FlowchartPanel flowchartPanel;
    private ExecutionControlPanel controlPanel;
    private OutputPanel outputPanel;
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
        // Create flowchart panel
        flowchartPanel = new FlowchartPanel();

        // Add panel in scroll pane
        JScrollPane scrollPane = new JScrollPane(flowchartPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Create execution panels
        controlPanel = new ExecutionControlPanel();
        outputPanel = new OutputPanel();
        variablesPanel = new VariablesPanel();

        // Create interpreter
        interpreter = new FlowchartInterpreter(
            flowchartPanel.getGraph(),
            flowchartPanel.getStartCell(),
            flowchartPanel.getEndCell()
        );

        // Setup interpreter listener
        setupInterpreter();

        // Setup control panel listener
        setupControlPanel();

        // Create right panel with output and variables
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputPanel, variablesPanel);
        rightSplitPane.setDividerLocation(300);
        rightSplitPane.setResizeWeight(0.5);

        // Create main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, rightSplitPane);
        mainSplitPane.setDividerLocation(900);
        mainSplitPane.setResizeWeight(0.7);

        // Layout
        add(controlPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private void setupInterpreter() {
        interpreter.setExecutionListener(new FlowchartInterpreter.ExecutionListener() {
            @Override
            public void onExecutionStep(Object cell, Map<String, Object> variables, String output) {
                // Update UI
                SwingUtilities.invokeLater(() -> {
                    flowchartPanel.highlightCell(cell);
                    variablesPanel.updateVariables(variables);
                    outputPanel.setOutput(output);
                });

                // Add delay for visualization
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void onExecutionComplete() {
                SwingUtilities.invokeLater(() -> {
                    flowchartPanel.clearHighlight();
                    controlPanel.setStatus("Execution completed");
                    controlPanel.setRunning(false);
                });
            }

            @Override
            public void onExecutionError(String error) {
                SwingUtilities.invokeLater(() -> {
                    flowchartPanel.clearHighlight();
                    controlPanel.setStatus("Error: " + error);
                    controlPanel.setRunning(false);
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

    private void setupControlPanel() {
        controlPanel.setExecutionControlListener(new ExecutionControlPanel.ExecutionControlListener() {
            @Override
            public void onRun() {
                // Reset UI
                outputPanel.clear();
                variablesPanel.clear();
                flowchartPanel.clearHighlight();

                // Update interpreter with current graph state
                interpreter = new FlowchartInterpreter(
                    flowchartPanel.getGraph(),
                    flowchartPanel.getStartCell(),
                    flowchartPanel.getEndCell()
                );
                setupInterpreter();

                // Start execution in background thread
                controlPanel.setStatus("Running...");
                controlPanel.setRunning(true);

                new Thread(() -> {
                    interpreter.start();
                }).start();
            }

            @Override
            public void onStep() {
                if (!interpreter.isRunning()) {
                    // First step - reset UI
                    outputPanel.clear();
                    variablesPanel.clear();
                    flowchartPanel.clearHighlight();

                    // Update interpreter
                    interpreter = new FlowchartInterpreter(
                        flowchartPanel.getGraph(),
                        flowchartPanel.getStartCell(),
                        flowchartPanel.getEndCell()
                    );
                    setupInterpreter();
                }

                controlPanel.setStatus("Stepping...");
                controlPanel.setStepping(true);

                new Thread(() -> {
                    interpreter.step();

                    SwingUtilities.invokeLater(() -> {
                        if (!interpreter.isRunning()) {
                            controlPanel.setStatus("Execution completed");
                            controlPanel.setRunning(false);
                        }
                    });
                }).start();
            }

            @Override
            public void onStop() {
                interpreter.stop();
                flowchartPanel.clearHighlight();
                controlPanel.setStatus("Stopped");
                controlPanel.setRunning(false);
            }

            @Override
            public void onReset() {
                interpreter.reset();
                flowchartPanel.clearHighlight();
                outputPanel.clear();
                variablesPanel.clear();
                controlPanel.setStatus("Ready");
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

        JMenuItem deleteItem = new JMenuItem("Delete Selected");
        deleteItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
        deleteItem.addActionListener(e -> flowchartPanel.deleteSelected());
        editMenu.add(deleteItem);

        JMenuItem editLabelItem = new JMenuItem("Edit Label");
        editLabelItem.setAccelerator(KeyStroke.getKeyStroke("F2"));
        editLabelItem.addActionListener(e -> flowchartPanel.editSelectedLabel());
        editMenu.add(editLabelItem);

        menuBar.add(editMenu);

        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');

        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke("control PLUS"));
        zoomInItem.addActionListener(e -> flowchartPanel.zoomIn());
        viewMenu.add(zoomInItem);

        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke("control MINUS"));
        zoomOutItem.addActionListener(e -> flowchartPanel.zoomOut());
        viewMenu.add(zoomOutItem);

        JMenuItem resetZoomItem = new JMenuItem("Reset Zoom");
        resetZoomItem.setAccelerator(KeyStroke.getKeyStroke("control 0"));
        resetZoomItem.addActionListener(e -> flowchartPanel.resetZoom());
        viewMenu.add(resetZoomItem);

        menuBar.add(viewMenu);

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

        // Instructions label - NEW INTERACTION MODEL
        JLabel infoLabel = new JLabel(" ✦ Click on EDGES (arrows) to insert blocks | Double-click blocks to edit | Right-click for menu ");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));
        infoLabel.setForeground(new Color(0, 100, 0));
        toolBar.add(infoLabel);

        add(toolBar, BorderLayout.NORTH);
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
            flowchartPanel.clearFlowchart();
        }
    }

    private void loadExample(int exampleNumber) {
        switch (exampleNumber) {
            case 1:
                flowchartPanel.createSimpleConditionalExample();
                break;
            case 2:
                flowchartPanel.createLoopExample();
                break;
            case 3:
                flowchartPanel.createNestedConditionalExample();
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
                "• Two branches (True/False) automatically appear\n" +
                "• Both branches merge at a point (black dot)\n" +
                "• Click on these branch edges to add more blocks!\n\n" +
                "NESTED IFs:\n" +
                "• Click on a True or False branch edge\n" +
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
                "• Process: Blue rectangle (actions/operations)\n" +
                "• Conditional (IF): Yellow diamond (decisions)\n" +
                "• I/O: Green cylinder (input/output/storage)\n" +
                "• Loop: Orange hexagon (while/for loops)\n" +
                "• Start/End: Gray rounded rectangle (fixed)\n" +
                "• Merge Point: Black dot (automatic for IFs)\n\n" +
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
                "3. Insert \"I/O\" block: \"Input: number\"\n" +
                "4. Click edge after I/O block\n" +
                "5. Insert \"Conditional\": \"number > 0?\"\n" +
                "   → Two branches appear automatically!\n" +
                "6. Click the True (green) branch\n" +
                "7. Insert \"Process\": \"result = number * 2\"\n" +
                "8. Click the False (red) branch\n" +
                "9. Insert \"Process\": \"result = 0\"\n" +
                "10. Both branches merge automatically!\n\n" +
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

    private void exitApplication() {
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlowchartEditorApp app = new FlowchartEditorApp();
            app.setVisible(true);
        });
    }
}
