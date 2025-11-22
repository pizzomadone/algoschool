import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an edge/connection in the flowchart.
 * This replaces edge cells from JGraphX.
 */
public class FlowchartEdge {

    private String id;
    private FlowchartNode source;
    private FlowchartNode target;
    private String label;
    private boolean isTrueBranch;
    private boolean isFalseBranch;

    // Computed path points for rendering
    private List<Point> pathPoints;

    public FlowchartEdge(String id, FlowchartNode source, FlowchartNode target, String label) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.label = label;
        this.pathPoints = new ArrayList<>();
        this.isTrueBranch = "True".equals(label);
        this.isFalseBranch = "False".equals(label);

        // Register with nodes
        if (source != null) source.addOutgoingEdge(this);
        if (target != null) target.addIncomingEdge(this);
    }

    public String getId() { return id; }
    public FlowchartNode getSource() { return source; }
    public FlowchartNode getTarget() { return target; }
    public String getLabel() { return label; }
    public void setLabel(String label) {
        this.label = label;
        this.isTrueBranch = "True".equals(label);
        this.isFalseBranch = "False".equals(label);
    }

    public List<Point> getPathPoints() { return pathPoints; }
    public void setPathPoints(List<Point> pathPoints) { this.pathPoints = pathPoints; }

    /**
     * Draw this edge with orthogonal (right-angle) routing
     */
    public void draw(Graphics2D g2) {
        if (source == null || target == null || pathPoints.isEmpty()) {
            return;
        }

        // Save original settings
        Color originalColor = g2.getColor();
        Stroke originalStroke = g2.getStroke();

        // Set thin black line as per the reference image
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));

        // Draw path segments
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Draw arrow at the end
        if (pathPoints.size() >= 2) {
            Point lastPoint = pathPoints.get(pathPoints.size() - 1);
            Point prevPoint = pathPoints.get(pathPoints.size() - 2);
            drawArrowHead(g2, prevPoint, lastPoint);
        }

        // Draw label if present
        if (label != null && !label.isEmpty()) {
            drawLabel(g2);
        }

        // Restore original settings
        g2.setColor(originalColor);
        g2.setStroke(originalStroke);
    }

    private void drawArrowHead(Graphics2D g2, Point from, Point to) {
        int arrowSize = 8;

        // Calculate angle
        double angle = Math.atan2(to.y - from.y, to.x - from.x);

        // Arrow head points
        int x1 = (int) (to.x - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (to.y - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (to.x - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (to.y - arrowSize * Math.sin(angle + Math.PI / 6));

        // Fill arrow head
        int[] xPoints = {to.x, x1, x2};
        int[] yPoints = {to.y, y1, y2};
        g2.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawLabel(Graphics2D g2) {
        if (pathPoints.size() < 2) return;

        // Position label near the start of the edge for conditional branches
        Point labelPos;
        if (isTrueBranch || isFalseBranch) {
            // For branches, place label near the source
            labelPos = pathPoints.get(Math.min(1, pathPoints.size() - 1));
        } else {
            // For other edges, place in the middle
            int midIndex = pathPoints.size() / 2;
            labelPos = pathPoints.get(midIndex);
        }

        g2.setColor(Color.BLACK);
        Font font = new Font("SansSerif", Font.PLAIN, 11);
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getHeight();

        // Draw label with small offset from the line
        g2.drawString(label, labelPos.x + 5, labelPos.y - 5);
    }

    /**
     * Compute orthogonal path from source to target
     */
    public void computePath() {
        pathPoints.clear();

        if (source == null || target == null) {
            return;
        }

        // Start from bottom center of source
        Point start = new Point(source.getCenterX(), source.getY() + source.getHeight());

        // End at top center of target
        Point end = new Point(target.getCenterX(), target.getY());

        // For special cases (conditional branches), adjust start point
        if (isTrueBranch) {
            // True branch goes from right side
            start = new Point(source.getX() + source.getWidth(), source.getCenterY());
        } else if (isFalseBranch) {
            // False branch goes from left side
            start = new Point(source.getX(), source.getCenterY());
        }

        pathPoints.add(start);

        // Simple orthogonal routing
        if (start.x != end.x) {
            // Add intermediate point for orthogonal routing
            int midY = (start.y + end.y) / 2;
            pathPoints.add(new Point(start.x, midY));
            pathPoints.add(new Point(end.x, midY));
        }

        pathPoints.add(end);
    }

    /**
     * Check if a point is near this edge (for selection)
     */
    public boolean isNear(int px, int py, double threshold) {
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);

            double distance = Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, px, py);
            if (distance <= threshold) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "FlowchartEdge{" + "id='" + id + "', label='" + label + "'}";
    }
}
