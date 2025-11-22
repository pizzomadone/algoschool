import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the flowchart with custom rendering.
 * This replaces mxCell from JGraphX.
 */
public class FlowchartNode {

    // Node types
    public enum NodeType {
        START, END, INPUT, OUTPUT, ASSIGNMENT, CONDITIONAL,
        LOOP, FOR_LOOP, DO_WHILE, MERGE, FUNCTION_CALL
    }

    private String id;
    private NodeType type;
    private String label;
    private int x, y;           // Position
    private int width, height;  // Dimensions
    private List<FlowchartEdge> outgoingEdges;
    private List<FlowchartEdge> incomingEdges;

    public FlowchartNode(String id, NodeType type, String label, int x, int y, int width, int height) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.outgoingEdges = new ArrayList<>();
        this.incomingEdges = new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public NodeType getType() { return type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getCenterX() { return x + width / 2; }
    public int getCenterY() { return y + height / 2; }

    public List<FlowchartEdge> getOutgoingEdges() { return outgoingEdges; }
    public List<FlowchartEdge> getIncomingEdges() { return incomingEdges; }

    public void addOutgoingEdge(FlowchartEdge edge) {
        if (!outgoingEdges.contains(edge)) {
            outgoingEdges.add(edge);
        }
    }

    public void addIncomingEdge(FlowchartEdge edge) {
        if (!incomingEdges.contains(edge)) {
            incomingEdges.add(edge);
        }
    }

    /**
     * Check if a point is inside this node
     */
    public boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    /**
     * Get the color for this node type based on the reference image
     */
    public Color getFillColor() {
        switch (type) {
            case START:
            case END:
                return new Color(0xE6, 0xD9, 0xF2); // Lavender purple
            case INPUT:
                return new Color(0xC8, 0xFF, 0xC8); // Light green
            case OUTPUT:
                return new Color(0xA3, 0xD9, 0xA3); // Darker green
            case ASSIGNMENT:
                return new Color(0xC8, 0xDC, 0xFF); // Light blue
            case CONDITIONAL:
                return new Color(0xFF, 0xD1, 0xDC); // Pink/salmon
            case LOOP:
            case FOR_LOOP:
            case DO_WHILE:
                return new Color(0xFF, 0xDC, 0xC8); // Light orange
            case FUNCTION_CALL:
                return new Color(0xE6, 0xB3, 0xFF); // Light purple
            case MERGE:
                return Color.BLACK;
            default:
                return Color.WHITE;
        }
    }

    /**
     * Draw this node on the graphics context
     */
    public void draw(Graphics2D g2) {
        // Save original settings
        Color originalColor = g2.getColor();
        Stroke originalStroke = g2.getStroke();

        // Set stroke for border
        g2.setStroke(new BasicStroke(2));

        switch (type) {
            case START:
            case END:
                drawEllipse(g2);
                break;
            case INPUT:
            case OUTPUT:
                drawParallelogram(g2);
                break;
            case ASSIGNMENT:
            case FUNCTION_CALL:
                drawRectangle(g2);
                break;
            case CONDITIONAL:
                drawDiamond(g2);
                break;
            case LOOP:
            case FOR_LOOP:
            case DO_WHILE:
                drawHexagon(g2);
                break;
            case MERGE:
                drawMergePoint(g2);
                break;
        }

        // Restore original settings
        g2.setColor(originalColor);
        g2.setStroke(originalStroke);
    }

    private void drawEllipse(Graphics2D g2) {
        // Fill
        g2.setColor(getFillColor());
        g2.fillOval(x, y, width, height);

        // Border
        g2.setColor(Color.BLACK);
        g2.drawOval(x, y, width, height);

        // Label
        drawCenteredText(g2, label);
    }

    private void drawRectangle(Graphics2D g2) {
        // Fill
        g2.setColor(getFillColor());
        g2.fillRect(x, y, width, height);

        // Border
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, width, height);

        // Label
        drawCenteredText(g2, label);
    }

    private void drawParallelogram(Graphics2D g2) {
        int offset = (int) (width * 0.15);

        int[] xPoints = {x + offset, x + width, x + width - offset, x};
        int[] yPoints = {y, y, y + height, y + height};

        // Fill
        g2.setColor(getFillColor());
        g2.fillPolygon(xPoints, yPoints, 4);

        // Border
        g2.setColor(Color.BLACK);
        g2.drawPolygon(xPoints, yPoints, 4);

        // Label with I: or O: prefix
        String prefix = (type == NodeType.INPUT) ? "I: " : "O: ";
        drawCenteredText(g2, prefix + label);
    }

    private void drawDiamond(Graphics2D g2) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;

        int[] xPoints = {centerX, x + width, centerX, x};
        int[] yPoints = {y, centerY, y + height, centerY};

        // Fill
        g2.setColor(getFillColor());
        g2.fillPolygon(xPoints, yPoints, 4);

        // Border
        g2.setColor(Color.BLACK);
        g2.drawPolygon(xPoints, yPoints, 4);

        // Label
        drawCenteredText(g2, label);
    }

    private void drawHexagon(Graphics2D g2) {
        int offset = (int) (width * 0.15);

        int[] xPoints = {x + offset, x + width - offset, x + width, x + width - offset, x + offset, x};
        int[] yPoints = {y, y, y + height/2, y + height, y + height, y + height/2};

        // Fill
        g2.setColor(getFillColor());
        g2.fillPolygon(xPoints, yPoints, 6);

        // Border
        g2.setColor(Color.BLACK);
        g2.drawPolygon(xPoints, yPoints, 6);

        // Label
        drawCenteredText(g2, label);
    }

    private void drawMergePoint(Graphics2D g2) {
        // Small black circle
        g2.setColor(Color.BLACK);
        g2.fillOval(x, y, width, height);
    }

    private void drawCenteredText(Graphics2D g2, String text) {
        if (text == null || text.isEmpty()) return;

        g2.setColor(Color.BLACK);
        Font font = new Font("SansSerif", Font.PLAIN, 12);
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();

        // Handle multiline text
        String[] lines = text.split("\n");
        int totalHeight = lines.length * fm.getHeight();
        int startY = y + (height - totalHeight) / 2 + fm.getAscent();

        for (String line : lines) {
            int textWidth = fm.stringWidth(line);
            int textX = x + (width - textWidth) / 2;
            g2.drawString(line, textX, startY);
            startY += fm.getHeight();
        }
    }

    @Override
    public String toString() {
        return "FlowchartNode{" + "id='" + id + "', type=" + type + ", label='" + label + "'}";
    }
}
