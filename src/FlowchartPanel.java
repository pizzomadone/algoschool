import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    // Track Start and End cells
    private Object startCell;
    private Object endCell;

    // Block type constants
    public static final String PROCESS = "PROCESS";
    public static final String CONDITIONAL = "CONDITIONAL";
    public static final String IO = "IO";
    public static final String LOOP = "LOOP";
    public static final String START = "START";
    public static final String END = "END";
    public static final String MERGE = "MERGE";  // Merge point for conditionals

    // Track merge points for conditionals
    private Map<Object, Object> conditionalMergePoints = new HashMap<>();

    public FlowchartPanel() {
        setLayout(new BorderLayout());

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
        graph.setCellsEditable(true);
        graph.setConnectableEdges(false);
        graph.setCellsDisconnectable(false);
        graph.setCellsMovable(false);  // FIXED: Blocks are now non-movable

        // Setup custom styles for flowchart blocks
        setupStyles();

        // Create graph component
        graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        graphComponent.getViewport().setOpaque(true);
        graphComponent.getViewport().setBackground(Color.WHITE);

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

        add(graphComponent, BorderLayout.CENTER);

        // Initialize with Start -> End
        initializeStartEnd();
    }

    private void setupStyles() {
        mxStylesheet stylesheet = graph.getStylesheet();

        // Process block style (rectangle, blue)
        Map<String, Object> processStyle = new HashMap<>();
        processStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        processStyle.put(mxConstants.STYLE_FILLCOLOR, "#C8DCFF");
        processStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        processStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        processStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        processStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        processStyle.put(mxConstants.STYLE_ROUNDED, false);
        stylesheet.putCellStyle(PROCESS, processStyle);

        // Conditional block style (diamond, yellow)
        Map<String, Object> conditionalStyle = new HashMap<>();
        conditionalStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RHOMBUS);
        conditionalStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFFFC8");
        conditionalStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        conditionalStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        conditionalStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        conditionalStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        stylesheet.putCellStyle(CONDITIONAL, conditionalStyle);

        // I/O block style (rounded rectangle, green - since PARALLELOGRAM might not exist)
        Map<String, Object> ioStyle = new HashMap<>();
        ioStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        ioStyle.put(mxConstants.STYLE_FILLCOLOR, "#C8FFC8");
        ioStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        ioStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        ioStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        ioStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        ioStyle.put(mxConstants.STYLE_ROUNDED, true);
        ioStyle.put(mxConstants.STYLE_ARCSIZE, 15);
        stylesheet.putCellStyle(IO, ioStyle);

        // Loop block style (hexagon, orange)
        Map<String, Object> loopStyle = new HashMap<>();
        loopStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_HEXAGON);
        loopStyle.put(mxConstants.STYLE_FILLCOLOR, "#FFDCC8");
        loopStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
        loopStyle.put(mxConstants.STYLE_STROKEWIDTH, 2);
        loopStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
        loopStyle.put(mxConstants.STYLE_FONTSIZE, 12);
        loopStyle.put(mxConstants.STYLE_PERIMETER, mxConstants.PERIMETER_HEXAGON);
        stylesheet.putCellStyle(LOOP, loopStyle);

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
        layout.setInterRankCellSpacing(80);
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

            // Offset dall'alto - leggermente distaccato dalla parte superiore
            double translateY = 80;

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
                            // Double-click to edit label
                            startEditingCell(cell);
                        }
                    }
                }
            }
        });
    }

    // Helper method to start editing a cell - uses available JGraphX method
    private void startEditingCell(Object cell) {
        if (graph.isCellEditable(cell)) {
            graphComponent.startEditingAtCell(cell);
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
        editItem.addActionListener(e -> startEditingCell(cell));
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

            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                if (edgeCell.getTarget() == cell) {
                    sourceEdge = edge;
                    source = edgeCell.getSource();
                } else if (edgeCell.getSource() == cell) {
                    targetEdge = edge;
                    target = edgeCell.getTarget();
                }
            }

            // Remove the block and its edges
            graph.removeCells(new Object[]{cell});

            // Reconnect if we have both source and target
            if (source != null && target != null) {
                graph.insertEdge(graph.getDefaultParent(), null, "", source, target);
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
            mxCell vertex = (mxCell) cell;
            String style = vertex.getStyle();
            if (!MERGE.equals(style)) {
                startEditingCell(cell);
            }
        }
    }

    private void insertBlockOnEdge(Object edge, String blockType) {
        graph.getModel().beginUpdate();
        try {
            mxCell edgeCell = (mxCell) edge;
            Object source = edgeCell.getSource();
            Object target = edgeCell.getTarget();
            Object parent = graph.getDefaultParent();

            // Remove the original edge
            graph.removeCells(new Object[]{edge});

            if (CONDITIONAL.equals(blockType)) {
                // Special handling for conditional blocks
                String conditionText = JOptionPane.showInputDialog(this,
                    "Enter condition text:", "Condition?");
                if (conditionText == null || conditionText.trim().isEmpty()) {
                    conditionText = "Condition?";
                }

                // Create the conditional block
                Object condBlock = graph.insertVertex(parent, null, conditionText,
                    0, 0, 120, 80, CONDITIONAL);

                // Create merge point
                Object mergePoint = graph.insertVertex(parent, null, "",
                    0, 0, 15, 15, MERGE);
                conditionalMergePoints.put(condBlock, mergePoint);

                // Connect source to conditional
                graph.insertEdge(parent, null, "", source, condBlock);

                // Create True branch (empty for now)
                Object truePlaceholder = graph.insertVertex(parent, null, "True Action",
                    0, 0, 140, 60, PROCESS);
                graph.insertEdge(parent, null, "Sì", condBlock, truePlaceholder, "TRUE_BRANCH");
                graph.insertEdge(parent, null, "", truePlaceholder, mergePoint);

                // Create False branch (empty for now)
                Object falsePlaceholder = graph.insertVertex(parent, null, "False Action",
                    0, 0, 140, 60, PROCESS);
                graph.insertEdge(parent, null, "No", condBlock, falsePlaceholder, "FALSE_BRANCH");
                graph.insertEdge(parent, null, "", falsePlaceholder, mergePoint);

                // Connect merge point to target
                graph.insertEdge(parent, null, "", mergePoint, target);

            } else if (LOOP.equals(blockType)) {
                // Special handling for loop blocks
                String loopText = JOptionPane.showInputDialog(this,
                    "Enter loop condition:", "Loop condition?");
                if (loopText == null || loopText.trim().isEmpty()) {
                    loopText = "Loop condition?";
                }

                // Create the loop block
                Object loopBlock = graph.insertVertex(parent, null, loopText,
                    0, 0, 120, 70, LOOP);

                // Create merge point after loop
                Object mergePoint = graph.insertVertex(parent, null, "",
                    0, 0, 15, 15, MERGE);

                // Connect source to loop
                graph.insertEdge(parent, null, "", source, loopBlock);

                // Create loop body
                Object loopBody = graph.insertVertex(parent, null, "Loop body",
                    0, 0, 140, 60, PROCESS);
                graph.insertEdge(parent, null, "Yes", loopBlock, loopBody, "TRUE_BRANCH");

                // Loop back from body to loop condition
                graph.insertEdge(parent, null, "", loopBody, loopBlock);

                // Exit loop
                graph.insertEdge(parent, null, "No", loopBlock, mergePoint, "FALSE_BRANCH");
                graph.insertEdge(parent, null, "", mergePoint, target);

            } else {
                // Regular blocks (Process, I/O)
                String blockText = JOptionPane.showInputDialog(this,
                    "Enter block text:", getDefaultTextForBlockType(blockType));
                if (blockText == null || blockText.trim().isEmpty()) {
                    blockText = getDefaultTextForBlockType(blockType);
                }

                // Determine dimensions based on type
                int width = 140;
                int height = (IO.equals(blockType)) ? 70 : 60;

                // Create the new block
                Object newBlock = graph.insertVertex(parent, null, blockText,
                    0, 0, width, height, blockType);

                // Reconnect: source -> newBlock -> target
                graph.insertEdge(parent, null, "", source, newBlock);
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

        JMenuItem processItem = new JMenuItem("Insert Process Block");
        processItem.addActionListener(e -> insertBlockOnEdge(edge, PROCESS));
        menu.add(processItem);

        JMenuItem conditionalItem = new JMenuItem("Insert Conditional (IF) Block");
        conditionalItem.addActionListener(e -> insertBlockOnEdge(edge, CONDITIONAL));
        menu.add(conditionalItem);

        JMenuItem ioItem = new JMenuItem("Insert I/O Block");
        ioItem.addActionListener(e -> insertBlockOnEdge(edge, IO));
        menu.add(ioItem);

        JMenuItem loopItem = new JMenuItem("Insert Loop Block");
        loopItem.addActionListener(e -> insertBlockOnEdge(edge, LOOP));
        menu.add(loopItem);

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
            case PROCESS:
                return "Process";
            case CONDITIONAL:
                return "Condition?";
            case IO:
                return "Input/Output";
            case LOOP:
                return "Loop condition?";
            case START:
                return "Start";
            case END:
                return "End";
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

            Object input = graph.insertVertex(parent, null, "Input: n", 0, 0, 140, 70, IO);
            graph.insertEdge(parent, null, "", start, input);

            Object condition = graph.insertVertex(parent, null, "n > 0?", 0, 0, 120, 80, CONDITIONAL);
            graph.insertEdge(parent, null, "", input, condition);

            Object mergePoint = graph.insertVertex(parent, null, "", 0, 0, 15, 15, MERGE);
            conditionalMergePoints.put(condition, mergePoint);

            Object processTrue = graph.insertVertex(parent, null, "result = n * 2", 0, 0, 140, 60, PROCESS);
            graph.insertEdge(parent, null, "Sì", condition, processTrue, "TRUE_BRANCH");
            graph.insertEdge(parent, null, "", processTrue, mergePoint);

            Object processFalse = graph.insertVertex(parent, null, "result = 0", 0, 0, 140, 60, PROCESS);
            graph.insertEdge(parent, null, "No", condition, processFalse, "FALSE_BRANCH");
            graph.insertEdge(parent, null, "", processFalse, mergePoint);

            Object output = graph.insertVertex(parent, null, "Output: result", 0, 0, 140, 70, IO);
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

            Object input = graph.insertVertex(parent, null, "Input: n\ni = 0", 0, 0, 140, 70, IO);
            graph.insertEdge(parent, null, "", start, input);

            Object loop = graph.insertVertex(parent, null, "i < n?", 0, 0, 120, 70, LOOP);
            graph.insertEdge(parent, null, "", input, loop);

            Object mergePoint = graph.insertVertex(parent, null, "", 0, 0, 15, 15, MERGE);

            Object loopBody = graph.insertVertex(parent, null, "Print i\ni = i + 1", 0, 0, 140, 60, PROCESS);
            graph.insertEdge(parent, null, "Yes", loop, loopBody, "TRUE_BRANCH");

            graph.insertEdge(parent, null, "", loopBody, loop); // Loop back
            graph.insertEdge(parent, null, "No", loop, mergePoint, "FALSE_BRANCH");

            Object output = graph.insertVertex(parent, null, "Done", 0, 0, 140, 70, IO);
            graph.insertEdge(parent, null, "", mergePoint, output);
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

            Object input = graph.insertVertex(parent, null, "Input: x, y", 0, 0, 140, 70, IO);
            graph.insertEdge(parent, null, "", start, input);

            Object outerCond = graph.insertVertex(parent, null, "x > 0?", 0, 0, 120, 80, CONDITIONAL);
            graph.insertEdge(parent, null, "", input, outerCond);

            Object outerMerge = graph.insertVertex(parent, null, "", 0, 0, 15, 15, MERGE);

            Object innerCond = graph.insertVertex(parent, null, "y > 0?", 0, 0, 120, 80, CONDITIONAL);
            graph.insertEdge(parent, null, "Sì", outerCond, innerCond, "TRUE_BRANCH");

            Object innerMerge = graph.insertVertex(parent, null, "", 0, 0, 15, 15, MERGE);

            Object innerTrue = graph.insertVertex(parent, null, "result = x + y", 0, 0, 140, 60, PROCESS);
            graph.insertEdge(parent, null, "Sì", innerCond, innerTrue, "TRUE_BRANCH");
            graph.insertEdge(parent, null, "", innerTrue, innerMerge);

            Object innerFalse = graph.insertVertex(parent, null, "result = x - y", 0, 0, 140, 60, PROCESS);
            graph.insertEdge(parent, null, "No", innerCond, innerFalse, "FALSE_BRANCH");
            graph.insertEdge(parent, null, "", innerFalse, innerMerge);

            graph.insertEdge(parent, null, "", innerMerge, outerMerge);

            Object outerFalse = graph.insertVertex(parent, null, "result = 0", 0, 0, 140, 60, PROCESS);
            graph.insertEdge(parent, null, "No", outerCond, outerFalse, "FALSE_BRANCH");
            graph.insertEdge(parent, null, "", outerFalse, outerMerge);

            Object output = graph.insertVertex(parent, null, "Output: result", 0, 0, 140, 70, IO);
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
}