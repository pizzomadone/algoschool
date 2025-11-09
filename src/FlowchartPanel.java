import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel that contains the JGraphX flowchart component.
 * NEW INTERACTION MODEL:
 * - Starts with Start -> End
 * - Click on EDGES to insert blocks
 * - IF blocks automatically create merge structure
 */
public class FlowchartPanel extends JPanel {

    private mxGraph graph;
    private mxGraphComponent graphComponent;
    private mxUndoManager undoManager;

    // Track Start and End cells
    private Object startCell;
    private Object endCell;

    // Flag to track if initial layout has been applied
    private boolean initialLayoutApplied = false;

    // Function management
    private Map<String, FunctionDefinition> functions = new HashMap<>();
    private String currentContext = "main";  // "main" or function name
    private mxGraph mainGraph = null;  // Separate reference to main graph
    private Object mainStartCell = null;
    private Object mainEndCell = null;

    // Block type constants
    public static final String ASSIGNMENT = "ASSIGNMENT";  // Assignment block (rectangle)
    public static final String INPUT = "INPUT";  // Input block (parallelogram with I)
    public static final String OUTPUT = "OUTPUT";  // Output block (parallelogram with O)
    public static final String CONDITIONAL = "CONDITIONAL";
    public static final String LOOP = "LOOP";  // While loop
    public static final String FOR_LOOP = "FOR_LOOP";  // For loop
    public static final String DO_WHILE = "DO_WHILE";  // Do-While loop
    public static final String START = "START";
    public static final String END = "END";
    public static final String MERGE = "MERGE";  // Merge point for conditionals
    public static final String FUNCTION_CALL = "FUNCTION_CALL";  // Function call block

    @Deprecated
    public static final String PROCESS = ASSIGNMENT;  // Deprecated: use ASSIGNMENT
    @Deprecated
    public static final String IO = OUTPUT;  // Deprecated: use INPUT or OUTPUT

    // Track merge points for conditionals
    private Map<Object, Object> conditionalMergePoints = new HashMap<>();

    // Track highlighted cell during execution
    private Object highlightedCell = null;
    private String originalCellStyle = null;

    public FlowchartPanel() {
        setLayout(new BorderLayout());

        // Register custom shape for parallelogram (used for Input/Output)
        mxGraphics2DCanvas.putShape("parallelogram", new ParallelogramShape());

        // Create graph
        graph = new mxGraph() {
            @Override
            public boolean isCellEditable(Object cell) {
                // Only allow editing text of vertices (not edges)
                return cell instanceof mxCell && ((mxCell) cell).isVertex() &&
                       !MERGE.equals(((mxCell) cell).getStyle());
            }
        };

        graph.setAllowDanglingEdges(false);
        graph.setCellsEditable(false);  // Disable direct editing - use dialog instead
        graph.setConnectableEdges(false);
        graph.setCellsDisconnectable(false);
        graph.setCellsMovable(false);  // FIXED: Blocks are now non-movable

        // Save reference to main graph
        mainGraph = graph;

        // Setup custom styles for flowchart blocks
        setupStyles();

        // Create graph component
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(Color.WHITE);

        // CRITICAL: Disable inline editing completely - use dialog instead
        graphComponent.setCellEditor(null);

        // Add listener to prevent any editing attempts
        graph.addListener(mxEvent.START_EDITING, new mxEventSource.mxIEventListener() {
            @Override
            public void invoke(Object sender, mxEventObject evt) {
                // Cancel the editing event
                evt.consume();
            }
        });

        // Enable grid with better visibility
        graphComponent.setGridVisible(true);
        graphComponent.setGridStyle(mxGraphComponent.GRID_STYLE_LINE);
        graphComponent.setGridColor(new Color(230, 230, 230));

        // CRITICAL: Enable anti-aliasing for better edge rendering
        graphComponent.setAntiAlias(true);
        graphComponent.setTextAntiAlias(true);

        // CRITICAL: Enable edge labels
        graphComponent.getGraph().setAllowDanglingEdges(false);
        graphComponent.getGraph().setEdgeLabelsMovable(false);

        // Setup mouse listeners for edge clicking
        setupMouseListeners();

        // Setup undo/redo manager
        setupUndoManager();

        // Add component listener to reapply layout when component is first shown
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (!initialLayoutApplied) {
                    // Delay the layout application to ensure viewport has correct size
                    SwingUtilities.invokeLater(() -> {
                        applyHierarchicalLayout();
                        initialLayoutApplied = true;
                    });
                }
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (!initialLayoutApplied && graphComponent.getViewport().getSize().width > 0) {
                    // If resized before shown, apply layout
                    SwingUtilities.invokeLater(() -> {
                        applyHierarchicalLayout();
                        initialLayoutApplied = true;
                    });
                }
            }
        });

        add(graphComponent, BorderLayout.CENTER);

        // Initialize with Start -> End
        initializeStartEnd();
    }

    private void setupStyles() {
        mxStylesheet stylesheet = graph.getStylesheet();

        // Assignment block style (rectangle, blue)
        Map<String, Object> assignmentStyle = new HashMap<>();
        assignmentStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        assignmentStyle.put(mxConstants.STYLE_FILLCOLOR, "#C8DCFF");
        assignmentStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        assignmentStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        assignmentStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        assignmentStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        assignmentStyle.put(mxConstants.STYLE_ROUNDED, false);
        stylesheet.putCellStyle(ASSIGNMENT, assignmentStyle);

        // Input block style (parallelogram, light green)
        Map<String, Object> inputStyle = new HashMap<>();
        inputStyle.put(mxConstants.STYLE_SHAPE, "parallelogram");
        inputStyle.put(mxConstants.STYLE_FILLCOLOR, "#C8FFC8");
        inputStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        inputStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        inputStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        inputStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        stylesheet.putCellStyle(INPUT, inputStyle);

        // Output block style (parallelogram, darker green)
        Map<String, Object> outputStyle = new HashMap<>();
        outputStyle.put(mxConstants.STYLE_SHAPE, "parallelogram");
        outputStyle.put(mxConstants.STYLE_FILLCOLOR, "#A3D9A3");
        outputStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        outputStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        outputStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        outputStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        stylesheet.putCellStyle(OUTPUT, outputStyle);

        // Conditional block style (diamond, yellow)
        Map<String, Object> conditionalStyle = new HashMap<>();
        conditionalStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RHOMBUS);
        conditionalStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFFC8");
        conditionalStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        conditionalStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        conditionalStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        conditionalStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        stylesheet.putCellStyle(CONDITIONAL, conditionalStyle);

        // Loop block style (hexagon, orange) - WHILE
        Map<String, Object> loopStyle = new HashMap<>();
        loopStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_HEXAGON);
        loopStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFDCC8");
        loopStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        loopStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        loopStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        loopStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        loopStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_HEXAGON);
        stylesheet.putCellStyle(LOOP, loopStyle);

        // For loop style (hexagon, light purple)
        Map<String, Object> forLoopStyle = new HashMap<>();
        forLoopStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_HEXAGON);
        forLoopStyle.put(mxConstants.STYLE_FILLCOLOR, "#E8DCFF");
        forLoopStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        forLoopStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        forLoopStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        forLoopStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        forLoopStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_HEXAGON);
        stylesheet.putCellStyle(FOR_LOOP, forLoopStyle);

        // Do-While loop style (hexagon, light pink)
        Map<String, Object> doWhileStyle = new HashMap<>();
        doWhileStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_HEXAGON);
        doWhileStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFDCE8");
        doWhileStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        doWhileStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        doWhileStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        doWhileStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        doWhileStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_HEXAGON);
        stylesheet.putCellStyle(DO_WHILE, doWhileStyle);

        // Start/End block style (rounded rectangle, gray)
        Map<String, Object> startEndStyle = new HashMap<>();
        startEndStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        startEndStyle.put(mxConstants.STYLE_FILLCOLOR, "#E0E0E0");
        startEndStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        startEndStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        startEndStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        startEndStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        startEndStyle.put(mxConstants.STYLE_ROUNDED, true);
        startEndStyle.put(mxConstants.STYLE_ARCSIZE, 50);
        stylesheet.putCellStyle(START, startEndStyle);
        stylesheet.putCellStyle(END, startEndStyle);

        // Merge point style (small circle, black)
        Map<String, Object> mergeStyle = new HashMap<>();
        mergeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        mergeStyle.put(mxConstants.STYLE_FILLCOLOR, "#000000");
        mergeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        mergeStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        mergeStyle.put(mxConstants.STYLE_FONTCOLOR, "#FFFFFF");
        mergeStyle.put(mxConstants.STYLE_FONTSIZE, 1);
        stylesheet.putCellStyle(MERGE, mergeStyle);

        // Function call block style (rectangle, purple/magenta)
        Map<String, Object> functionCallStyle = new HashMap<>();
        functionCallStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        functionCallStyle.put(mxConstants.STYLE_FILLCOLOR, "#E6B3FF");  // Light purple
        functionCallStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        functionCallStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        functionCallStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        functionCallStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        functionCallStyle.put(mxConstants.STYLE_ROUNDED, false);
        stylesheet.putCellStyle(FUNCTION_CALL, functionCallStyle);

        // ============== CORREZIONE DEGLI STILI PER GLI ARCHI ==============
        // Versione semplificata che funziona con tutte le versioni di JGraphX
        

        // Default edge style
        Map<String, Object> edgeStyle = new HashMap<>();
        edgeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 3.0); // 👈 spessore più grande
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);

        stylesheet.setDefaultEdgeStyle(edgeStyle);


        // True branch - GREEN
        Map<String, Object> trueBranchStyle = new HashMap<>();
        trueBranchStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
        trueBranchStyle.put(mxConstants.STYLE_STROKECOLOR, "#00AA00");
        trueBranchStyle.put(mxConstants.STYLE_FONTCOLOR, "#00AA00");
        trueBranchStyle.put(mxConstants.STYLE_STROKEWIDTH, 3.0);
        trueBranchStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        trueBranchStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
        trueBranchStyle.put(mxConstants.STYLE_ROUNDED, true);
        trueBranchStyle.put(mxConstants.STYLE_FONTSIZE, 14);
        trueBranchStyle.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        stylesheet.putCellStyle("TRUE_BRANCH", trueBranchStyle);

        // False branch - RED
        Map<String, Object> falseBranchStyle = new HashMap<>();
        falseBranchStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
        falseBranchStyle.put(mxConstants.STYLE_STROKECOLOR, "#CC0000");
        falseBranchStyle.put(mxConstants.STYLE_FONTCOLOR, "#CC0000");
        falseBranchStyle.put(mxConstants.STYLE_STROKEWIDTH, 3.0);
        falseBranchStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        falseBranchStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
        falseBranchStyle.put(mxConstants.STYLE_ROUNDED, true);
        falseBranchStyle.put(mxConstants.STYLE_FONTSIZE, 14);
        falseBranchStyle.put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        stylesheet.putCellStyle("FALSE_BRANCH", falseBranchStyle);
    }

    private void initializeStartEnd() {
        Object parent = graph.getDefaultParent();

        graph.getModel().beginUpdate();
        try {
            // Clear everything if it exists
            graph.removeCells(graph.getChildVertices(parent));

            // Create start and end nodes
            startCell = graph.insertVertex(parent, null, "Start", 300, 50, 100, 50, START);
            endCell = graph.insertVertex(parent, null, "End", 300, 450, 100, 50, END);

            // Save main start/end cells
            mainStartCell = startCell;
            mainEndCell = endCell;

            // Create initial edge between start and end
            graph.insertEdge(parent, null, "", startCell, endCell);

            // Apply hierarchical layout
            applyHierarchicalLayout();

        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Apply hierarchical layout to organize the flowchart
     */
    private void applyHierarchicalLayout() {
        mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
        layout.setInterHierarchySpacing(50);
        layout.setInterRankCellSpacing(40);  // Dimezzato da 80 a 40 per archi più corti
        layout.setOrientation(SwingConstants.NORTH);

        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
            layout.execute(parent);

            // Ottieni la geometria del blocco Start per usarlo come punto di riferimento
            mxCell startVertex = (mxCell) startCell;
            mxGeometry startGeometry = startVertex.getGeometry();

            // Calcola il centro X del blocco Start (dopo il layout)
            double startCenterX = startGeometry.getX() + startGeometry.getWidth() / 2;

            // Ottieni le dimensioni della viewport
            Dimension viewportSize = graphComponent.getViewport().getSize();

            // Calcola la traslazione per centrare il blocco Start orizzontalmente nella viewport
            double viewportCenterX = viewportSize.width / 2.0;
            double translateX = viewportCenterX - startCenterX;

            // Offset dall'alto - Start vicino al bordo superiore (10px di margine)
            double translateY = 10;

            // Applica la traslazione
            graph.getView().setTranslate(new mxPoint(translateX, translateY));

            // Calcola i bounds del grafico per il ridimensionamento dinamico
            mxRectangle graphBounds = graph.getGraphBounds();

            // Se il grafico è troppo grande, ridimensiona il graphComponent
            // per assicurarti che sia completamente visibile con scrollbar
            double requiredWidth = graphBounds.getWidth() + 2 * Math.abs(translateX) + 100;
            double requiredHeight = graphBounds.getHeight() + translateY + 100;

            // Imposta una dimensione preferita maggiore per permettere lo scroll
            Dimension currentPrefSize = graphComponent.getPreferredSize();
            Dimension newPrefSize = new Dimension(
                (int) Math.max(requiredWidth, currentPrefSize.width),
                (int) Math.max(requiredHeight, currentPrefSize.height)
            );

            graphComponent.setPreferredSize(newPrefSize);
            graphComponent.setMinimumSize(new Dimension((int) requiredWidth, (int) requiredHeight));
            graphComponent.revalidate();

        } finally {
            graph.getModel().endUpdate();
        }
    }

    private void setupMouseListeners() {
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());

                if (cell != null) {
                    if (graph.getModel().isEdge(cell)) {
                        // Click on an edge
                        if (SwingUtilities.isRightMouseButton(e) || e.getClickCount() == 1) {
                            // Show menu to insert block
                            showInsertBlockMenu(cell, e.getX(), e.getY());
                        }
                    } else if (graph.getModel().isVertex(cell)) {
                        // Click on a vertex
                        if (SwingUtilities.isRightMouseButton(e)) {
                            // Show vertex menu
                            showVertexMenu(cell, e.getX(), e.getY());
                        } else if (e.getClickCount() == 2) {
                            // Double-click to edit label via dialog
                            editCellWithDialog(cell);
                        }
                    }
                }
            }
        });
    }

    /**
     * Opens a dialog to edit the cell content
     */
    private void editCellWithDialog(Object cell) {
        if (cell == null) return;

        mxCell mxCell = (mxCell) cell;
        String style = mxCell.getStyle();

        // Don't allow editing Start, End, or Merge points
        if (START.equals(style) || END.equals(style) || MERGE.equals(style)) {
            return;
        }

        // Get current value
        String currentValue = mxCell.getValue() != null ? mxCell.getValue().toString() : "";

        // Determine dialog title based on block type
        String dialogTitle = "Edit Block Content";
        String dialogMessage = "Enter new content:";

        if (ASSIGNMENT.equals(style)) {
            dialogTitle = "Edit Assignment Block";
            dialogMessage = "Enter assignment (e.g., x = 5):";
        } else if (INPUT.equals(style)) {
            dialogTitle = "Edit Input Block";
            dialogMessage = "Enter variable name:";
        } else if (OUTPUT.equals(style)) {
            dialogTitle = "Edit Output Block";
            dialogMessage = "Enter output expression or string:";
        } else if (CONDITIONAL.equals(style)) {
            dialogTitle = "Edit Conditional Block";
            dialogMessage = "Enter condition (e.g., x > 0):";
        } else if (LOOP.equals(style) || FOR_LOOP.equals(style) || DO_WHILE.equals(style)) {
            dialogTitle = "Edit Loop Block";
            dialogMessage = "Enter loop condition or specification:";
        } else if (FUNCTION_CALL.equals(style)) {
            dialogTitle = "Edit Function Call";
            dialogMessage = "Enter function call (e.g., result = func(x)):";
        }

        // Show input dialog
        String newValue = (String) JOptionPane.showInputDialog(
            graphComponent,
            dialogMessage,
            dialogTitle,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            currentValue
        );

        // If user didn't cancel, update the cell value
        if (newValue != null && !newValue.equals(currentValue)) {
            graph.getModel().beginUpdate();
            try {
                graph.getModel().setValue(cell, newValue);
            } finally {
                graph.getModel().endUpdate();
            }
        }
    }

    private void showVertexMenu(Object cell, int x, int y) {
        mxCell vertex = (mxCell) cell;
        String style = vertex.getStyle();

        // Don't show menu for Start, End, or Merge points
        if (START.equals(style) || END.equals(style) || MERGE.equals(style)) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Edit Label (F2)");
        editItem.addActionListener(e -> editCellWithDialog(cell));
        menu.add(editItem);

        menu.addSeparator();

        JMenuItem deleteItem = new JMenuItem("Delete Block");
        deleteItem.addActionListener(e -> deleteBlock(cell));
        menu.add(deleteItem);

        menu.addSeparator();

        JMenuItem layoutItem = new JMenuItem("Re-apply Layout");
        layoutItem.addActionListener(e -> applyHierarchicalLayout());
        menu.add(layoutItem);

        menu.show(graphComponent.getGraphControl(), x, y);
    }

    private void deleteBlock(Object cell) {
        if (cell == startCell || cell == endCell) {
            JOptionPane.showMessageDialog(this,
                "Start and End blocks cannot be deleted.",
                "Cannot Delete",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        graph.getModel().beginUpdate();
        try {
            // Get edges connected to this block
            Object[] edges = graph.getEdges(cell);
            Object sourceEdge = null;
            Object targetEdge = null;
            Object source = null;
            Object target = null;
            String originalLabel = "";
            String originalStyle = null;

            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                if (edgeCell.getTarget() == cell) {
                    sourceEdge = edge;
                    source = edgeCell.getSource();
                    // IMPORTANTE: Salva l'etichetta e lo stile dell'edge in ingresso
                    originalLabel = (String) edgeCell.getValue();
                    originalStyle = edgeCell.getStyle();
                } else if (edgeCell.getSource() == cell) {
                    targetEdge = edge;
                    target = edgeCell.getTarget();
                }
            }

            // Remove the block and its edges
            graph.removeCells(new Object[]{cell});

            // Reconnect if we have both source and target
            // IMPORTANTE: Preserva l'etichetta e lo stile dell'edge originale
            if (source != null && target != null) {
                if (originalLabel == null) originalLabel = "";
                graph.insertEdge(graph.getDefaultParent(), null, originalLabel, source, target, originalStyle);
            }

            // Re-layout
            applyHierarchicalLayout();

        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Clear flowchart and restart with Start -> End
     */
    public void clearFlowchart() {
        initializeStartEnd();
    }

    /**
     * Delete selected cells
     */
    public void deleteSelected() {
        Object[] cells = graph.getSelectionCells();
        if (cells != null && cells.length > 0) {
            for (Object cell : cells) {
                if (cell != startCell && cell != endCell && graph.getModel().isVertex(cell)) {
                    deleteBlock(cell);
                }
            }
        }
    }

    /**
     * Edit label of selected cell
     */
    public void editSelectedLabel() {
        Object cell = graph.getSelectionCell();
        if (cell != null && graph.getModel().isVertex(cell)) {
            editCellWithDialog(cell);
        }
    }

    private void insertBlockOnEdge(Object edge, String blockType) {
        graph.getModel().beginUpdate();
        try {
            mxCell edgeCell = (mxCell) edge;
            Object source = edgeCell.getSource();
            Object target = edgeCell.getTarget();
            Object parent = graph.getDefaultParent();

            // IMPORTANTE: Salva l'etichetta e lo stile dell'edge originale
            String originalLabel = (String) edgeCell.getValue();
            String originalStyle = edgeCell.getStyle();

            // Remove the original edge
            graph.removeCells(new Object[]{edge});

            if (CONDITIONAL.equals(blockType)) {
                // Special handling for conditional blocks
                String conditionText = JOptionPane.showInputDialog(this,
                    "Inserisci la condizione:", "x > 0");

                // Se l'utente ha premuto Annulla, ripristina l'edge e non creare il blocco
                if (conditionText == null) {
                    if (originalLabel == null) originalLabel = "";
                    graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);
                    return;
                }

                if (conditionText.trim().isEmpty()) {
                    conditionText = "condizione";
                }

                // Create the conditional block
                Object condBlock = graph.insertVertex(parent, null, conditionText,
                    0, 0, 120, 80, CONDITIONAL);

                // Create merge point
                Object mergePoint = graph.insertVertex(parent, null, "",
                    0, 0, 15, 15, MERGE);
                conditionalMergePoints.put(condBlock, mergePoint);

                // Connect source to conditional - IMPORTANTE: preserva etichetta e stile originali
                if (originalLabel == null) originalLabel = "";
                graph.insertEdge(parent, null, originalLabel, source, condBlock, originalStyle);

                // Create True branch - direttamente al merge point (senza blocchi intermedi)
                graph.insertEdge(parent, null, "Sì", condBlock, mergePoint, "TRUE_BRANCH");

                // Create False branch - direttamente al merge point (senza blocchi intermedi)
                graph.insertEdge(parent, null, "No", condBlock, mergePoint, "FALSE_BRANCH");

                // Connect merge point to target
                graph.insertEdge(parent, null, "", mergePoint, target);

            } else if (LOOP.equals(blockType)) {
                // Special handling for WHILE loop blocks
                String loopText = JOptionPane.showInputDialog(this,
                    "Inserisci la condizione del ciclo WHILE:", "i < n");

                // Se l'utente ha premuto Annulla, ripristina l'edge e non creare il blocco
                if (loopText == null) {
                    if (originalLabel == null) originalLabel = "";
                    graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);
                    return;
                }

                if (loopText.trim().isEmpty()) {
                    loopText = "condizione";
                }

                // Create the loop block
                Object loopBlock = graph.insertVertex(parent, null, loopText,
                    0, 0, 120, 70, LOOP);

                // Create merge point for loop body (where users can insert blocks)
                Object bodyMergePoint = graph.insertVertex(parent, null, "",
                    0, 0, 15, 15, MERGE);

                // Connect source to loop condition - IMPORTANTE: preserva etichetta e stile originali
                if (originalLabel == null) originalLabel = "";
                graph.insertEdge(parent, null, originalLabel, source, loopBlock, originalStyle);

                // Yes branch: enter loop body (GREEN arrow)
                // Users can insert blocks on this edge
                graph.insertEdge(parent, null, "Yes", loopBlock, bodyMergePoint, "TRUE_BRANCH");

                // Loop back: from body merge point to loop condition
                graph.insertEdge(parent, null, "", bodyMergePoint, loopBlock);

                // No branch: exit loop (RED arrow) - point directly to target
                graph.insertEdge(parent, null, "No", loopBlock, target, "FALSE_BRANCH");

            } else if (FOR_LOOP.equals(blockType)) {
                // Special handling for FOR loop blocks
                // For loop: init; condition; increment
                String initText = JOptionPane.showInputDialog(this,
                    "Inserisci l'inizializzazione del FOR (es. i = 0):", "i = 0");
                // Se l'utente ha premuto Annulla, ripristina l'edge e non creare il blocco
                if (initText == null) {
                    if (originalLabel == null) originalLabel = "";
                    graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);
                    return;
                }
                if (initText.trim().isEmpty()) initText = "i = 0";

                String condText = JOptionPane.showInputDialog(this,
                    "Inserisci la condizione del FOR (es. i < n):", "i < n");
                // Se l'utente ha premuto Annulla, ripristina l'edge e non creare il blocco
                if (condText == null) {
                    if (originalLabel == null) originalLabel = "";
                    graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);
                    return;
                }
                if (condText.trim().isEmpty()) condText = "i < n";

                String incrText = JOptionPane.showInputDialog(this,
                    "Inserisci l'incremento del FOR (es. i = i + 1):", "i = i + 1");
                // Se l'utente ha premuto Annulla, ripristina l'edge e non creare il blocco
                if (incrText == null) {
                    if (originalLabel == null) originalLabel = "";
                    graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);
                    return;
                }
                if (incrText.trim().isEmpty()) incrText = "i = i + 1";

                // Combine into a single string for display
                String forText = initText + "; " + condText + "; " + incrText;

                // Create the for loop block
                Object forBlock = graph.insertVertex(parent, null, forText,
                    0, 0, 180, 70, FOR_LOOP);

                // Create merge point for loop body
                Object bodyMergePoint = graph.insertVertex(parent, null, "",
                    0, 0, 15, 15, MERGE);

                // Connect source to for loop - IMPORTANTE: preserva etichetta e stile originali
                if (originalLabel == null) originalLabel = "";
                graph.insertEdge(parent, null, originalLabel, source, forBlock, originalStyle);

                // Yes branch: enter loop body (GREEN arrow)
                graph.insertEdge(parent, null, "Yes", forBlock, bodyMergePoint, "TRUE_BRANCH");

                // Loop back: from body merge point to for loop
                graph.insertEdge(parent, null, "", bodyMergePoint, forBlock);

                // No branch: exit loop (RED arrow)
                graph.insertEdge(parent, null, "No", forBlock, target, "FALSE_BRANCH");

            } else if (DO_WHILE.equals(blockType)) {
                // Special handling for DO-WHILE loop blocks
                String condText = JOptionPane.showInputDialog(this,
                    "Inserisci la condizione del DO-WHILE:", "i < n");

                // Se l'utente ha premuto Annulla, ripristina l'edge e non creare il blocco
                if (condText == null) {
                    if (originalLabel == null) originalLabel = "";
                    graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);
                    return;
                }

                if (condText.trim().isEmpty()) {
                    condText = "condizione";
                }

                // Create merge point for loop body (entered first)
                Object bodyMergePoint = graph.insertVertex(parent, null, "",
                    0, 0, 15, 15, MERGE);

                // Create the do-while condition block
                Object doWhileBlock = graph.insertVertex(parent, null, condText,
                    0, 0, 120, 70, DO_WHILE);

                // Connect source to body merge point (body is executed first) - IMPORTANTE: preserva etichetta e stile originali
                if (originalLabel == null) originalLabel = "";
                graph.insertEdge(parent, null, originalLabel, source, bodyMergePoint, originalStyle);

                // From body, go to condition
                graph.insertEdge(parent, null, "", bodyMergePoint, doWhileBlock);

                // Yes branch: loop back to body (GREEN arrow)
                graph.insertEdge(parent, null, "Yes", doWhileBlock, bodyMergePoint, "TRUE_BRANCH");

                // No branch: exit loop (RED arrow)
                graph.insertEdge(parent, null, "No", doWhileBlock, target, "FALSE_BRANCH");

            } else {
                // Regular blocks (Assignment, Input, Output)
                String blockText = JOptionPane.showInputDialog(this,
                    "Enter block text:", getDefaultTextForBlockType(blockType));

                // Se l'utente ha premuto Annulla, ripristina l'edge e non creare il blocco
                if (blockText == null) {
                    if (originalLabel == null) originalLabel = "";
                    graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);
                    return;
                }

                if (blockText.trim().isEmpty()) {
                    blockText = getDefaultTextForBlockType(blockType);
                }

                // Per blocchi INPUT, valida che il nome variabile non contenga spazi
                if (INPUT.equals(blockType)) {
                    String varPart = blockText.replaceFirst("^I:\\s*", "").trim();
                    if (varPart.contains(" ")) {
                        // Ripristina l'edge originale prima di mostrare l'errore
                        if (originalLabel == null) originalLabel = "";
                        graph.insertEdge(parent, null, originalLabel, source, target, originalStyle);

                        JOptionPane.showMessageDialog(this,
                            "Errore: Il nome della variabile non può contenere spazi.\n" +
                            "Usa un nome singolo come 'n', 'x', 'sum', ecc.",
                            "Nome variabile non valido",
                            JOptionPane.ERROR_MESSAGE);
                        return; // Non creare il blocco
                    }
                }

                // Determine dimensions based on type
                int width = 140;
                int height = 60;
                if (INPUT.equals(blockType) || OUTPUT.equals(blockType)) {
                    height = 70;  // Parallelograms are slightly taller
                }

                // Create the new block
                Object newBlock = graph.insertVertex(parent, null, blockText,
                    0, 0, width, height, blockType);

                // Reconnect: source -> newBlock -> target
                // IMPORTANTE: Preserva l'etichetta e lo stile dell'edge originale sulla prima freccia
                if (originalLabel == null) originalLabel = "";
                graph.insertEdge(parent, null, originalLabel, source, newBlock, originalStyle);
                graph.insertEdge(parent, null, "", newBlock, target);
            }

            // Apply layout to reorganize
            applyHierarchicalLayout();

        } finally {
            graph.getModel().endUpdate();
        }
    }

    private void showInsertBlockMenu(Object edge, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem assignmentItem = new JMenuItem("Insert Assignment Block (□)");
        assignmentItem.addActionListener(e -> insertBlockOnEdge(edge, ASSIGNMENT));
        menu.add(assignmentItem);

        JMenuItem inputItem = new JMenuItem("Insert Input Block (▱ I)");
        inputItem.addActionListener(e -> insertBlockOnEdge(edge, INPUT));
        menu.add(inputItem);

        JMenuItem outputItem = new JMenuItem("Insert Output Block (▱ O)");
        outputItem.addActionListener(e -> insertBlockOnEdge(edge, OUTPUT));
        menu.add(outputItem);

        menu.addSeparator();

        JMenuItem conditionalItem = new JMenuItem("Insert Conditional (IF) Block");
        conditionalItem.addActionListener(e -> insertBlockOnEdge(edge, CONDITIONAL));
        menu.add(conditionalItem);

        JMenuItem loopItem = new JMenuItem("Insert While Loop Block");
        loopItem.addActionListener(e -> insertBlockOnEdge(edge, LOOP));
        menu.add(loopItem);

        JMenuItem forLoopItem = new JMenuItem("Insert For Loop Block");
        forLoopItem.addActionListener(e -> insertBlockOnEdge(edge, FOR_LOOP));
        menu.add(forLoopItem);

        JMenuItem doWhileItem = new JMenuItem("Insert Do-While Loop Block");
        doWhileItem.addActionListener(e -> insertBlockOnEdge(edge, DO_WHILE));
        menu.add(doWhileItem);

        menu.addSeparator();

        JMenuItem functionCallItem = new JMenuItem("Insert Function Call Block");
        functionCallItem.addActionListener(e -> insertBlockOnEdge(edge, FUNCTION_CALL));
        menu.add(functionCallItem);

        menu.addSeparator();

        JMenuItem layoutItem = new JMenuItem("Re-apply Layout");
        layoutItem.addActionListener(e -> applyHierarchicalLayout());
        menu.add(layoutItem);

        menu.show(graphComponent.getGraphControl(), x, y);
    }

    /**
     * Zoom in
     */
    public void zoomIn() {
        graphComponent.zoomIn();
    }

    /**
     * Zoom out
     */
    public void zoomOut() {
        graphComponent.zoomOut();
    }

    /**
     * Reset zoom to 100%
     */
    public void resetZoom() {
        applyHierarchicalLayout(); // Usa il nostro metodo per resettare E centrare
    }

    private String getDefaultTextForBlockType(String blockType) {
        switch (blockType) {
            case ASSIGNMENT:
                return "x = 0";
            case INPUT:
                return "n";
            case OUTPUT:
                return "n";
            case CONDITIONAL:
                return "x > 0";
            case LOOP:
                return "i < n";
            case START:
                return "Start";
            case END:
                return "End";
            case FUNCTION_CALL:
                return "myFunction(x, y)";
            default:
                return "Block";
        }
    }

    // ===== EXAMPLE FLOWCHARTS =====

    /**
     * Create a simple conditional flowchart example
     */
    public void createSimpleConditionalExample() {
        clearFlowchart();
        Object parent = graph.getDefaultParent();
        Object start = startCell;
        Object end = endCell;

        graph.getModel().beginUpdate();
        try {
            Object[] edges = graph.getEdgesBetween(start, end);
            if (edges.length > 0) graph.removeCells(edges);

            Object input = graph.insertVertex(parent, null, "n", 0, 0, 140, 70, INPUT);
            graph.insertEdge(parent, null, "", start, input);

            Object condition = graph.insertVertex(parent, null, "n > 0?", 0, 0, 120, 80, CONDITIONAL);
            graph.insertEdge(parent, null, "", input, condition);

            Object mergePoint = graph.insertVertex(parent, null, "", 0, 0, 15, 15, MERGE);
            conditionalMergePoints.put(condition, mergePoint);

            Object processTrue = graph.insertVertex(parent, null, "result = n * 2", 0, 0, 140, 60, ASSIGNMENT);
            graph.insertEdge(parent, null, "Sì", condition, processTrue, "TRUE_BRANCH");
            graph.insertEdge(parent, null, "", processTrue, mergePoint);

            Object processFalse = graph.insertVertex(parent, null, "result = 0", 0, 0, 140, 60, ASSIGNMENT);
            graph.insertEdge(parent, null, "No", condition, processFalse, "FALSE_BRANCH");
            graph.insertEdge(parent, null, "", processFalse, mergePoint);

            Object output = graph.insertVertex(parent, null, "result", 0, 0, 140, 70, OUTPUT);
            graph.insertEdge(parent, null, "", mergePoint, output);
            graph.insertEdge(parent, null, "", output, end);

            applyHierarchicalLayout();

        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Create a loop flowchart example
     */
    public void createLoopExample() {
        clearFlowchart();
        Object parent = graph.getDefaultParent();
        Object start = startCell;
        Object end = endCell;

        graph.getModel().beginUpdate();
        try {
            Object[] edges = graph.getEdgesBetween(start, end);
            if (edges.length > 0) graph.removeCells(edges);

            // Input: ask for n
            Object input = graph.insertVertex(parent, null, "n", 0, 0, 140, 70, INPUT);
            graph.insertEdge(parent, null, "", start, input);

            // Initialize counter
            Object init = graph.insertVertex(parent, null, "i = 0", 0, 0, 140, 60, ASSIGNMENT);
            graph.insertEdge(parent, null, "", input, init);

            // Loop condition (no question mark)
            Object loop = graph.insertVertex(parent, null, "i < n", 0, 0, 120, 70, LOOP);
            graph.insertEdge(parent, null, "", init, loop);

            // Output current i value
            Object outputI = graph.insertVertex(parent, null, "i", 0, 0, 140, 70, OUTPUT);
            graph.insertEdge(parent, null, "Yes", loop, outputI, "TRUE_BRANCH");

            // Increment i
            Object increment = graph.insertVertex(parent, null, "i = i + 1", 0, 0, 140, 60, ASSIGNMENT);
            graph.insertEdge(parent, null, "", outputI, increment);

            // Loop back to condition
            graph.insertEdge(parent, null, "", increment, loop);

            // Done message
            Object output = graph.insertVertex(parent, null, "\"Done\"", 0, 0, 140, 70, OUTPUT);
            // No branch exits loop directly to output (no merge point needed)
            graph.insertEdge(parent, null, "No", loop, output, "FALSE_BRANCH");
            graph.insertEdge(parent, null, "", output, end);

            applyHierarchicalLayout();

        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Create a nested conditional flowchart example
     */
    public void createNestedConditionalExample() {
        clearFlowchart();
        Object parent = graph.getDefaultParent();
        Object start = startCell;
        Object end = endCell;

        graph.getModel().beginUpdate();
        try {
            Object[] edges = graph.getEdgesBetween(start, end);
            if (edges.length > 0) graph.removeCells(edges);

            Object inputX = graph.insertVertex(parent, null, "x", 0, 0, 140, 70, INPUT);
            graph.insertEdge(parent, null, "", start, inputX);

            Object inputY = graph.insertVertex(parent, null, "y", 0, 0, 140, 70, INPUT);
            graph.insertEdge(parent, null, "", inputX, inputY);

            Object outerCond = graph.insertVertex(parent, null, "x > 0?", 0, 0, 120, 80, CONDITIONAL);
            graph.insertEdge(parent, null, "", inputY, outerCond);

            Object outerMerge = graph.insertVertex(parent, null, "", 0, 0, 15, 15, MERGE);

            Object innerCond = graph.insertVertex(parent, null, "y > 0?", 0, 0, 120, 80, CONDITIONAL);
            graph.insertEdge(parent, null, "Sì", outerCond, innerCond, "TRUE_BRANCH");

            Object innerMerge = graph.insertVertex(parent, null, "", 0, 0, 15, 15, MERGE);

            Object innerTrue = graph.insertVertex(parent, null, "result = x + y", 0, 0, 140, 60, ASSIGNMENT);
            graph.insertEdge(parent, null, "Sì", innerCond, innerTrue, "TRUE_BRANCH");
            graph.insertEdge(parent, null, "", innerTrue, innerMerge);

            Object innerFalse = graph.insertVertex(parent, null, "result = x - y", 0, 0, 140, 60, ASSIGNMENT);
            graph.insertEdge(parent, null, "No", innerCond, innerFalse, "FALSE_BRANCH");
            graph.insertEdge(parent, null, "", innerFalse, innerMerge);

            graph.insertEdge(parent, null, "", innerMerge, outerMerge);

            Object outerFalse = graph.insertVertex(parent, null, "result = 0", 0, 0, 140, 60, ASSIGNMENT);
            graph.insertEdge(parent, null, "No", outerCond, outerFalse, "FALSE_BRANCH");
            graph.insertEdge(parent, null, "", outerFalse, outerMerge);

            Object output = graph.insertVertex(parent, null, "result", 0, 0, 140, 70, OUTPUT);
            graph.insertEdge(parent, null, "", outerMerge, output);
            graph.insertEdge(parent, null, "", output, end);

            applyHierarchicalLayout();

        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Method called from toolbar - no longer used with new interaction model
     */
    public void addBlock(String blockType) {
        JOptionPane.showMessageDialog(this,
            "To add blocks:\n" +
            "1. Click on an EDGE (arrow) in the flowchart\n" +
            "2. Select the block type to insert\n\n" +
            "The new block will be inserted in the middle of the edge.",
            "How to Add Blocks",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ===== EXECUTION HIGHLIGHTING =====

    /**
     * Highlight a cell during execution
     */
    public void highlightCell(Object cell) {
        // Remove previous highlight
        clearHighlight();

        if (cell != null && graph.getModel().isVertex(cell)) {
            mxCell vertex = (mxCell) cell;

            // Save original style
            highlightedCell = cell;
            originalCellStyle = vertex.getStyle();

            // Apply highlight style
            graph.getModel().beginUpdate();
            try {
                String currentStyle = originalCellStyle != null ? originalCellStyle : "";

                // Add yellow border and shadow to highlight
                String highlightStyle = currentStyle + ";strokeColor=#FFD700;strokeWidth=4;shadow=1";
                graph.getModel().setStyle(cell, highlightStyle);

            } finally {
                graph.getModel().endUpdate();
            }

            // Refresh the view
            graphComponent.refresh();
        }
    }

    /**
     * Clear the current highlight
     */
    public void clearHighlight() {
        if (highlightedCell != null) {
            graph.getModel().beginUpdate();
            try {
                // Restore original style
                graph.getModel().setStyle(highlightedCell, originalCellStyle);

            } finally {
                graph.getModel().endUpdate();
            }

            highlightedCell = null;
            originalCellStyle = null;
            graphComponent.refresh();
        }
    }

    /**
     * Get the graph for external use
     */
    public mxGraph getGraph() {
        return graph;
    }

    /**
     * Get start cell
     */
    public Object getStartCell() {
        return startCell;
    }

    /**
     * Get end cell
     */
    public Object getEndCell() {
        return endCell;
    }

    // ===== UNDO/REDO FUNCTIONALITY =====

    /**
     * Setup undo/redo manager
     */
    private void setupUndoManager() {
        // Create undo manager
        undoManager = new mxUndoManager();

        // Add listener to the graph model to record changes
        graph.getModel().addListener(mxEvent.UNDO, new mxEventSource.mxIEventListener() {
            @Override
            public void invoke(Object sender, mxEventObject evt) {
                undoManager.undoableEditHappened((mxUndoableEdit) evt.getProperty("edit"));
            }
        });

        // Enable undo in graph component
        graphComponent.getGraphControl().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                // Ctrl+Z for undo
                if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_Z) {
                    if (!e.isShiftDown()) {
                        undo();
                    } else {
                        // Ctrl+Shift+Z for redo
                        redo();
                    }
                }
                // Ctrl+Y for redo (alternative)
                else if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_Y) {
                    redo();
                }
            }
        });
    }

    /**
     * Undo the last change
     */
    public void undo() {
        if (undoManager != null && undoManager.canUndo()) {
            undoManager.undo();
            graphComponent.refresh();

            // Verifica che START e END esistano ancora dopo l'undo
            if (!verifyStartEndExist()) {
                // Se START o END sono stati cancellati, annulla l'undo
                undoManager.redo();
                graphComponent.refresh();

                JOptionPane.showMessageDialog(
                    this,
                    "Cannot undo: this would remove the Start or End blocks.\nThe flowchart must always have Start and End blocks.",
                    "Undo Limit Reached",
                    JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }

    /**
     * Redo the last undone change
     */
    public void redo() {
        if (undoManager != null && undoManager.canRedo()) {
            undoManager.redo();
            graphComponent.refresh();
        }
    }

    /**
     * Check if undo is available
     */
    public boolean canUndo() {
        return undoManager != null && undoManager.canUndo();
    }

    /**
     * Check if redo is available
     */
    public boolean canRedo() {
        return undoManager != null && undoManager.canRedo();
    }

    /**
     * Clear undo history
     */
    public void clearUndoHistory() {
        if (undoManager != null) {
            undoManager.clear();
        }
    }

    // ===== SAVE/LOAD FUNCTIONALITY =====

    /**
     * Save the flowchart to an XML file
     */
    public void saveFlowchart(File file) throws Exception {
        mxCodec codec = new mxCodec();
        String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
        mxUtils.writeFile(xml, file.getAbsolutePath());
    }

    /**
     * Load a flowchart from an XML file
     */
    public void loadFlowchart(File file) throws Exception {
        // Parse XML document
        Document document = mxXmlUtils.parseXml(mxUtils.readFile(file.getAbsolutePath()));

        // Create codec and decode
        mxCodec codec = new mxCodec(document);

        // Clear current graph
        graph.getModel().beginUpdate();
        try {
            // Remove all cells
            graph.removeCells(graph.getChildVertices(graph.getDefaultParent()));

            // Decode the model
            codec.decode(document.getDocumentElement(), graph.getModel());

            // Find Start and End cells after loading
            Object parent = graph.getDefaultParent();
            Object[] vertices = graph.getChildVertices(parent);

            startCell = null;
            endCell = null;
            conditionalMergePoints.clear();

            for (Object vertex : vertices) {
                mxCell cell = (mxCell) vertex;
                String style = cell.getStyle();
                String value = cell.getValue() != null ? cell.getValue().toString() : "";

                if (START.equals(style) || "Start".equals(value)) {
                    startCell = vertex;
                }
                if (END.equals(style) || "End".equals(value)) {
                    endCell = vertex;
                }

                // Rebuild conditional merge points map
                if (CONDITIONAL.equals(style)) {
                    // Find the merge point for this conditional
                    Object[] edges = graph.getEdges(vertex);
                    for (Object edge : edges) {
                        mxCell edgeCell = (mxCell) edge;
                        if (edgeCell.getSource() == vertex) {
                            Object target = edgeCell.getTarget();
                            if (target instanceof mxCell) {
                                mxCell targetCell = (mxCell) target;
                                if (MERGE.equals(targetCell.getStyle())) {
                                    conditionalMergePoints.put(vertex, target);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

        } finally {
            graph.getModel().endUpdate();
        }

        // Reset the layout flag and apply layout after component is ready
        // This ensures the flowchart is centered correctly
        initialLayoutApplied = false;
        SwingUtilities.invokeLater(() -> {
            applyHierarchicalLayout();
            initialLayoutApplied = true;
        });
    }

    // ===== FUNCTION MANAGEMENT =====

    /**
     * Creates a new function with the given name.
     *
     * @param functionName Name of the function
     * @return true if created successfully, false if function already exists
     */
    public boolean createFunction(String functionName) {
        if (functionName == null || functionName.trim().isEmpty()) {
            return false;
        }

        if (functions.containsKey(functionName) || "main".equals(functionName)) {
            return false; // Function already exists or name is reserved
        }

        FunctionDefinition functionDef = new FunctionDefinition(functionName);

        // Initialize the function graph with styles
        mxGraph funcGraph = functionDef.getFunctionGraph();
        funcGraph.setStylesheet(graph.getStylesheet()); // Copy styles from main graph

        // Create Start and End blocks for the function
        Object parent = funcGraph.getDefaultParent();
        funcGraph.getModel().beginUpdate();
        try {
            Object funcStart = funcGraph.insertVertex(parent, null, "Start", 300, 50, 100, 50, START);
            Object funcEnd = funcGraph.insertVertex(parent, null, "End", 300, 450, 100, 50, END);
            funcGraph.insertEdge(parent, null, "", funcStart, funcEnd);

            functionDef.setStartCell(funcStart);
            functionDef.setEndCell(funcEnd);
        } finally {
            funcGraph.getModel().endUpdate();
        }

        functions.put(functionName, functionDef);
        return true;
    }

    /**
     * Creates a new function with a pre-defined FunctionDefinition.
     *
     * @param functionName Name of the function
     * @param functionDef The function definition with parameters
     * @return true if created successfully, false if function already exists
     */
    public boolean createFunctionWithDefinition(String functionName, FunctionDefinition functionDef) {
        if (functionName == null || functionName.trim().isEmpty() || functionDef == null) {
            return false;
        }

        if (functions.containsKey(functionName) || "main".equals(functionName)) {
            return false; // Function already exists or name is reserved
        }

        // Initialize the function graph with styles
        mxGraph funcGraph = functionDef.getFunctionGraph();
        funcGraph.setStylesheet(graph.getStylesheet()); // Copy styles from main graph

        // Create Start and End blocks for the function
        Object parent = funcGraph.getDefaultParent();
        funcGraph.getModel().beginUpdate();
        try {
            Object funcStart = funcGraph.insertVertex(parent, null, "Start", 300, 50, 100, 50, START);
            Object funcEnd = funcGraph.insertVertex(parent, null, "End", 300, 450, 100, 50, END);
            funcGraph.insertEdge(parent, null, "", funcStart, funcEnd);

            functionDef.setStartCell(funcStart);
            functionDef.setEndCell(funcEnd);
        } finally {
            funcGraph.getModel().endUpdate();
        }

        functions.put(functionName, functionDef);
        return true;
    }

    /**
     * Inserts an INPUT block after the given cell.
     *
     * @param afterCell The cell after which to insert the INPUT block
     * @param variableName The name of the input variable
     * @return The newly created INPUT cell
     */
    public Object insertInputBlockAfter(Object afterCell, String variableName) {
        if (afterCell == null) {
            return null;
        }

        graph.getModel().beginUpdate();
        try {
            // Find the edge from afterCell
            Object[] edges = graph.getOutgoingEdges(afterCell);
            if (edges == null || edges.length == 0) {
                return null;
            }

            com.mxgraph.model.mxCell edge = (com.mxgraph.model.mxCell) edges[0];
            Object targetCell = edge.getTarget();

            // Remove the old edge
            graph.removeCells(new Object[]{edge});

            // Create the INPUT block
            Object parent = graph.getDefaultParent();
            Object inputCell = graph.insertVertex(parent, null, variableName, 0, 0, 150, 60, INPUT);

            // Insert edges: afterCell -> inputCell -> targetCell
            graph.insertEdge(parent, null, "", afterCell, inputCell);
            graph.insertEdge(parent, null, "", inputCell, targetCell);

            // Apply layout
            applyHierarchicalLayout();

            return inputCell;
        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Deletes a function.
     *
     * @param functionName Name of the function to delete
     * @return true if deleted successfully, false if function doesn't exist
     */
    public boolean deleteFunction(String functionName) {
        if (!functions.containsKey(functionName)) {
            return false;
        }

        functions.remove(functionName);

        // If we're currently viewing this function, switch back to main
        if (currentContext.equals(functionName)) {
            switchToContext("main");
        }

        return true;
    }

    /**
     * Switches the current context to main or a function.
     *
     * @param contextName "main" or a function name
     * @return true if switched successfully, false if context doesn't exist
     */
    public boolean switchToContext(String contextName) {
        if ("main".equals(contextName)) {
            // Save current graph state if we're in a function
            if (!"main".equals(currentContext)) {
                FunctionDefinition currentFunc = functions.get(currentContext);
                if (currentFunc != null) {
                    // The function graph is already up to date
                }
            }

            // Switch to main
            currentContext = "main";
            graph = mainGraph;
            startCell = mainStartCell;
            endCell = mainEndCell;

            // Update the graphComponent to display the main graph
            graphComponent.setGraph(mainGraph);
            graphComponent.refresh();

            return true;

        } else {
            // Switch to a function
            FunctionDefinition funcDef = functions.get(contextName);
            if (funcDef == null) {
                return false; // Function doesn't exist
            }

            // Save current graph state if we're in main
            if ("main".equals(currentContext)) {
                // Main graph is already up to date
            }

            // Switch to function
            currentContext = contextName;
            graph = funcDef.getFunctionGraph();
            startCell = funcDef.getStartCell();
            endCell = funcDef.getEndCell();

            // Update the graphComponent to display the function graph
            graphComponent.setGraph(funcDef.getFunctionGraph());
            graphComponent.refresh();

            return true;
        }
    }

    /**
     * Gets the current context name.
     *
     * @return "main" or the name of the current function
     */
    public String getCurrentContext() {
        return currentContext;
    }

    /**
     * Gets all defined functions.
     *
     * @return Map of function names to function definitions
     */
    public Map<String, FunctionDefinition> getFunctions() {
        return functions;
    }

    /**
     * Gets a specific function definition.
     *
     * @param functionName Name of the function
     * @return FunctionDefinition or null if not found
     */
    public FunctionDefinition getFunction(String functionName) {
        return functions.get(functionName);
    }

    /**
     * Verifies that START and END blocks still exist in the graph.
     * Used to prevent undo operations from removing essential blocks.
     *
     * @return true if both START and END exist, false otherwise
     */
    private boolean verifyStartEndExist() {
        Object parent = graph.getDefaultParent();
        Object[] vertices = graph.getChildVertices(parent);

        boolean hasStart = false;
        boolean hasEnd = false;

        for (Object vertex : vertices) {
            if (vertex instanceof mxCell) {
                mxCell cell = (mxCell) vertex;
                String style = cell.getStyle();

                if (START.equals(style)) {
                    hasStart = true;
                    startCell = vertex;  // Update reference
                } else if (END.equals(style)) {
                    hasEnd = true;
                    endCell = vertex;  // Update reference
                }

                if (hasStart && hasEnd) {
                    return true;  // Found both, can return early
                }
            }
        }

        return hasStart && hasEnd;
    }
}