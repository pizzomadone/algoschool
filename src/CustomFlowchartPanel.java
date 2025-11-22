import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Custom flowchart panel with manual rendering.
 * Replaces JGraphX-based FlowchartPanel for pixel-perfect control.
 */
public class CustomFlowchartPanel extends JPanel {

    private List<FlowchartNode> nodes;
    private List<FlowchartEdge> edges;
    private FlowchartNode startNode;
    private FlowchartNode endNode;

    private int nodeIdCounter = 0;
    private int edgeIdCounter = 0;

    // Layout parameters - matched to reference image
    private static final int VERTICAL_SPACING = 30;
    private static final int HORIZONTAL_SPACING = 100;
    private static final int START_Y = 50;
    private static final int START_X = 300;

    // Selection and interaction
    private FlowchartNode selectedNode = null;
    private FlowchartEdge selectedEdge = null;
    private FlowchartNode highlightedNode = null; // For execution

    public CustomFlowchartPanel() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();

        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));

        // Initialize with Start -> End
        initializeStartEnd();

        // Setup mouse listeners
        setupMouseListeners();
    }

    private void initializeStartEnd() {
        // Create Start node (Main)
        startNode = createNode(FlowchartNode.NodeType.START, "Main", 120, 60);
        startNode.setX(START_X);
        startNode.setY(START_Y);

        // Create End node
        endNode = createNode(FlowchartNode.NodeType.END, "End", 120, 60);
        endNode.setX(START_X);
        endNode.setY(START_Y + 400);

        // Connect them
        createEdge(startNode, endNode, "");

        // Apply layout
        applyLayout();
    }

    /**
     * Create a new node
     */
    public FlowchartNode createNode(FlowchartNode.NodeType type, String label, int width, int height) {
        String id = "node_" + (nodeIdCounter++);
        FlowchartNode node = new FlowchartNode(id, type, label, 0, 0, width, height);
        nodes.add(node);
        return node;
    }

    /**
     * Create a new edge
     */
    public FlowchartEdge createEdge(FlowchartNode source, FlowchartNode target, String label) {
        String id = "edge_" + (edgeIdCounter++);
        FlowchartEdge edge = new FlowchartEdge(id, source, target, label);
        edges.add(edge);
        return edge;
    }

    /**
     * Remove a node and its connected edges
     */
    public void removeNode(FlowchartNode node) {
        if (node == startNode || node == endNode) {
            return; // Cannot remove start/end
        }

        // Remove connected edges
        List<FlowchartEdge> toRemove = new ArrayList<>();
        for (FlowchartEdge edge : edges) {
            if (edge.getSource() == node || edge.getTarget() == node) {
                toRemove.add(edge);
            }
        }
        edges.removeAll(toRemove);

        // Remove node
        nodes.remove(node);

        applyLayout();
        repaint();
    }

    /**
     * Remove an edge
     */
    public void removeEdge(FlowchartEdge edge) {
        edges.remove(edge);
        repaint();
    }

    /**
     * Apply automatic layout
     */
    public void applyLayout() {
        if (nodes.isEmpty()) return;

        // Build adjacency structure
        Map<FlowchartNode, List<FlowchartNode>> children = new HashMap<>();
        Map<FlowchartNode, Integer> inDegree = new HashMap<>();

        for (FlowchartNode node : nodes) {
            children.put(node, new ArrayList<>());
            inDegree.put(node, 0);
        }

        for (FlowchartEdge edge : edges) {
            children.get(edge.getSource()).add(edge.getTarget());
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
        }

        // Topological sort for layering
        Queue<FlowchartNode> queue = new LinkedList<>();
        Map<FlowchartNode, Integer> layer = new HashMap<>();

        queue.offer(startNode);
        layer.put(startNode, 0);

        while (!queue.isEmpty()) {
            FlowchartNode current = queue.poll();
            int currentLayer = layer.get(current);

            for (FlowchartNode child : children.get(current)) {
                if (!layer.containsKey(child)) {
                    layer.put(child, currentLayer + 1);
                    queue.offer(child);
                } else {
                    // Update layer to maximum
                    layer.put(child, Math.max(layer.get(child), currentLayer + 1));
                }
            }
        }

        // Group nodes by layer
        Map<Integer, List<FlowchartNode>> layerNodes = new HashMap<>();
        for (FlowchartNode node : nodes) {
            int l = layer.getOrDefault(node, 0);
            layerNodes.putIfAbsent(l, new ArrayList<>());
            layerNodes.get(l).add(node);
        }

        // Position nodes
        int maxLayer = layerNodes.keySet().stream().max(Integer::compareTo).orElse(0);

        for (int l = 0; l <= maxLayer; l++) {
            List<FlowchartNode> nodesInLayer = layerNodes.get(l);
            if (nodesInLayer == null) continue;

            int y = START_Y + l * (60 + VERTICAL_SPACING);

            // Center nodes horizontally
            int totalWidth = nodesInLayer.size() * 140 + (nodesInLayer.size() - 1) * HORIZONTAL_SPACING;
            int startX = (getWidth() - totalWidth) / 2;
            if (startX < 50) startX = 50;

            for (int i = 0; i < nodesInLayer.size(); i++) {
                FlowchartNode node = nodesInLayer.get(i);
                int x = startX + i * (140 + HORIZONTAL_SPACING);

                node.setX(x);
                node.setY(y);
            }
        }

        // Compute edge paths
        for (FlowchartEdge edge : edges) {
            edge.computePath();
        }

        repaint();
    }

    /**
     * Insert a node on an edge
     */
    public FlowchartNode insertNodeOnEdge(FlowchartEdge edge, FlowchartNode.NodeType type, String label) {
        if (edge == null) return null;

        FlowchartNode source = edge.getSource();
        FlowchartNode target = edge.getTarget();

        // Remove original edge
        removeEdge(edge);

        // Create new node
        int width = 140;
        int height = 60;
        FlowchartNode newNode = createNode(type, label, width, height);

        // Create new edges
        createEdge(source, newNode, "");
        createEdge(newNode, target, "");

        applyLayout();
        repaint();

        return newNode;
    }

    /**
     * Insert a conditional block on an edge
     */
    public void insertConditionalOnEdge(FlowchartEdge edge, String condition) {
        if (edge == null) return;

        FlowchartNode source = edge.getSource();
        FlowchartNode target = edge.getTarget();

        // Remove original edge
        removeEdge(edge);

        // Create conditional node
        FlowchartNode condNode = createNode(FlowchartNode.NodeType.CONDITIONAL, condition, 120, 80);

        // Create merge point
        FlowchartNode mergeNode = createNode(FlowchartNode.NodeType.MERGE, "", 15, 15);

        // Connect: source -> conditional
        createEdge(source, condNode, "");

        // True branch: conditional -> merge
        createEdge(condNode, mergeNode, "True");

        // False branch: conditional -> merge
        createEdge(condNode, mergeNode, "False");

        // merge -> target
        createEdge(mergeNode, target, "");

        applyLayout();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // Enable anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw edges first (behind nodes)
        for (FlowchartEdge edge : edges) {
            edge.draw(g2);
        }

        // Draw nodes
        for (FlowchartNode node : nodes) {
            node.draw(g2);

            // Highlight selected node
            if (node == selectedNode) {
                g2.setColor(new Color(255, 215, 0)); // Gold
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(node.getX() - 3, node.getY() - 3, node.getWidth() + 6, node.getHeight() + 6);
            }

            // Highlight during execution
            if (node == highlightedNode && node != startNode && node != endNode) {
                g2.setColor(new Color(255, 215, 0, 100)); // Transparent gold
                g2.setStroke(new BasicStroke(4));
                g2.drawRect(node.getX() - 2, node.getY() - 2, node.getWidth() + 4, node.getHeight() + 4);
            }
        }
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });
    }

    private void handleMouseClick(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        // Check if clicked on a node
        FlowchartNode clickedNode = findNodeAt(x, y);
        if (clickedNode != null) {
            if (SwingUtilities.isRightMouseButton(e)) {
                showNodeMenu(clickedNode, e.getX(), e.getY());
            } else if (e.getClickCount() == 2) {
                editNode(clickedNode);
            }
            return;
        }

        // Check if clicked on an edge
        FlowchartEdge clickedEdge = findEdgeAt(x, y);
        if (clickedEdge != null) {
            showEdgeMenu(clickedEdge, e.getX(), e.getY());
        }
    }

    private FlowchartNode findNodeAt(int x, int y) {
        for (FlowchartNode node : nodes) {
            if (node.contains(x, y)) {
                return node;
            }
        }
        return null;
    }

    private FlowchartEdge findEdgeAt(int x, int y) {
        for (FlowchartEdge edge : edges) {
            if (edge.isNear(x, y, 5.0)) {
                return edge;
            }
        }
        return null;
    }

    private void editNode(FlowchartNode node) {
        if (node == startNode || node == endNode) {
            return; // Cannot edit start/end
        }

        if (node.getType() == FlowchartNode.NodeType.MERGE) {
            return; // Cannot edit merge points
        }

        String newLabel = JOptionPane.showInputDialog(this, "Edit label:", node.getLabel());
        if (newLabel != null && !newLabel.trim().isEmpty()) {
            node.setLabel(newLabel);
            repaint();
        }
    }

    private void showNodeMenu(FlowchartNode node, int x, int y) {
        if (node == startNode || node == endNode || node.getType() == FlowchartNode.NodeType.MERGE) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Edit");
        editItem.addActionListener(e -> editNode(node));
        menu.add(editItem);

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> removeNode(node));
        menu.add(deleteItem);

        menu.show(this, x, y);
    }

    private void showEdgeMenu(FlowchartEdge edge, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem assignmentItem = new JMenuItem("Insert Assignment");
        assignmentItem.addActionListener(e -> {
            String label = JOptionPane.showInputDialog(this, "Enter assignment:", "x = 0");
            if (label != null) {
                insertNodeOnEdge(edge, FlowchartNode.NodeType.ASSIGNMENT, label);
            }
        });
        menu.add(assignmentItem);

        JMenuItem inputItem = new JMenuItem("Insert Input");
        inputItem.addActionListener(e -> {
            String label = JOptionPane.showInputDialog(this, "Enter variable name:", "n");
            if (label != null) {
                insertNodeOnEdge(edge, FlowchartNode.NodeType.INPUT, label);
            }
        });
        menu.add(inputItem);

        JMenuItem outputItem = new JMenuItem("Insert Output");
        outputItem.addActionListener(e -> {
            String label = JOptionPane.showInputDialog(this, "Enter output:", "n");
            if (label != null) {
                insertNodeOnEdge(edge, FlowchartNode.NodeType.OUTPUT, label);
            }
        });
        menu.add(outputItem);

        menu.addSeparator();

        JMenuItem conditionalItem = new JMenuItem("Insert Conditional");
        conditionalItem.addActionListener(e -> {
            String label = JOptionPane.showInputDialog(this, "Enter condition:", "x > 0");
            if (label != null) {
                insertConditionalOnEdge(edge, label);
            }
        });
        menu.add(conditionalItem);

        menu.show(this, x, y);
    }

    // API methods for compatibility
    public FlowchartNode getStartNode() {
        return startNode;
    }

    public FlowchartNode getEndNode() {
        return endNode;
    }

    public List<FlowchartNode> getNodes() {
        return new ArrayList<>(nodes);
    }

    public List<FlowchartEdge> getEdges() {
        return new ArrayList<>(edges);
    }

    public void highlightNode(FlowchartNode node) {
        this.highlightedNode = node;
        repaint();
    }

    public void clearHighlight() {
        this.highlightedNode = null;
        repaint();
    }

    public void clearFlowchart() {
        nodes.clear();
        edges.clear();
        nodeIdCounter = 0;
        edgeIdCounter = 0;
        initializeStartEnd();
        repaint();
    }
}
