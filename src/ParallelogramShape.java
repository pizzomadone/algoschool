import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxBasicShape;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * Custom shape for drawing a parallelogram in JGraphX.
 * Used for Input/Output blocks in flowcharts.
 */
public class ParallelogramShape extends mxBasicShape {

    /**
     * Offset ratio for the parallelogram slant (0.2 = 20% of width)
     */
    private static final double OFFSET_RATIO = 0.15;

    @Override
    public void paintShape(mxGraphics2DCanvas canvas, mxCellState state) {
        Graphics2D g2 = canvas.getGraphics();

        // Get bounds
        int x = (int) state.getX();
        int y = (int) state.getY();
        int w = (int) state.getWidth();
        int h = (int) state.getHeight();

        // Calculate offset for parallelogram slant
        int offset = (int) (w * OFFSET_RATIO);

        // Create parallelogram path
        GeneralPath path = new GeneralPath();
        path.moveTo(x + offset, y);           // Top-left (with offset)
        path.lineTo(x + w, y);                // Top-right
        path.lineTo(x + w - offset, y + h);   // Bottom-right (with offset)
        path.lineTo(x, y + h);                // Bottom-left
        path.closePath();

        // Fill
        if (state.getStyle().containsKey(mxConstants.STYLE_FILLCOLOR)) {
            String fillColor = mxUtils.getString(state.getStyle(), mxConstants.STYLE_FILLCOLOR);
            g2.setColor(mxUtils.parseColor(fillColor));
            g2.fill(path);
        }

        // Draw border
        if (state.getStyle().containsKey(mxConstants.STYLE_STROKECOLOR)) {
            String strokeColor = mxUtils.getString(state.getStyle(), mxConstants.STYLE_STROKECOLOR);
            g2.setColor(mxUtils.parseColor(strokeColor));

            float strokeWidth = mxUtils.getFloat(state.getStyle(), mxConstants.STYLE_STROKEWIDTH, 1);
            g2.setStroke(new BasicStroke(strokeWidth));

            g2.draw(path);
        }

        // Determina se è Input o Output dal nome dello stile
        String style = null;
        Object baseStyleObj = state.getStyle().get("baseStyleName");
        if (baseStyleObj instanceof String) {
            style = (String) baseStyleObj;
        }

        if (style == null) {
            // Prova a determinare dallo stile completo della cella
            Object cellObj = state.getCell();
            if (cellObj instanceof com.mxgraph.model.mxCell) {
                com.mxgraph.model.mxCell cell = (com.mxgraph.model.mxCell) cellObj;
                style = cell.getStyle();
            }
        }

        // Disegna "I:" o "O:" in grassetto a sinistra del parallelogramma
        String label = null;
        if (style != null) {
            if (style.contains("INPUT")) {
                label = "I:";
            } else if (style.contains("OUTPUT")) {
                label = "O:";
            }
        }

        if (label != null) {
            // Salva il font originale
            Font originalFont = g2.getFont();

            // Imposta font grassetto
            Font boldFont = originalFont.deriveFont(Font.BOLD, 14f);
            g2.setFont(boldFont);

            // Calcola dimensioni del testo
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getHeight();

            // Posiziona il testo a sinistra del parallelogramma
            int textX = x - textWidth - 8;  // 8 pixel di padding
            int textY = y + (h + textHeight) / 2 - fm.getDescent();

            // Disegna il testo in nero
            g2.setColor(Color.BLACK);
            g2.drawString(label, textX, textY);

            // Ripristina il font originale
            g2.setFont(originalFont);
        }
    }
}
