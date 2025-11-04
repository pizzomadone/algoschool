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
    }
}
