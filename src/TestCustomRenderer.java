import javax.swing.*;
import java.awt.*;

/**
 * Test application for the custom flowchart renderer
 */
public class TestCustomRenderer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Custom Flowchart Renderer - Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);

            // Create custom panel
            CustomFlowchartPanel panel = new CustomFlowchartPanel();

            // Add some test nodes to match the reference image
            createTestFlowchart(panel);

            // Wrap in scroll pane
            JScrollPane scrollPane = new JScrollPane(panel);
            frame.add(scrollPane, BorderLayout.CENTER);

            // Add toolbar
            JToolBar toolbar = new JToolBar();
            JButton clearBtn = new JButton("Clear");
            clearBtn.addActionListener(e -> panel.clearFlowchart());
            toolbar.add(clearBtn);

            JButton layoutBtn = new JButton("Re-layout");
            layoutBtn.addActionListener(e -> panel.applyLayout());
            toolbar.add(layoutBtn);

            frame.add(toolbar, BorderLayout.NORTH);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Create a test flowchart similar to the reference image
     */
    private static void createTestFlowchart(CustomFlowchartPanel panel) {
        // Clear default
        panel.clearFlowchart();

        FlowchartNode start = panel.getStartNode();
        FlowchartNode end = panel.getEndNode();

        // Remove default edge
        panel.getEdges().clear();

        // Output "Inserire il numero"
        FlowchartNode output1 = panel.createNode(
            FlowchartNode.NodeType.OUTPUT,
            "Inserire il numero",
            180, 60
        );
        panel.createEdge(start, output1, "");

        // Input N
        FlowchartNode inputN = panel.createNode(
            FlowchartNode.NodeType.INPUT,
            "N",
            140, 60
        );
        panel.createEdge(output1, inputN, "");

        // Output "Il numero inserito è "
        FlowchartNode output2 = panel.createNode(
            FlowchartNode.NodeType.OUTPUT,
            "Il numero inserito è ",
            200, 60
        );
        panel.createEdge(inputN, output2, "");

        // Conditional N > 0
        FlowchartNode cond1 = panel.createNode(
            FlowchartNode.NodeType.CONDITIONAL,
            "N > 0",
            120, 80
        );
        panel.createEdge(output2, cond1, "");

        // Merge point 1
        FlowchartNode merge1 = panel.createNode(
            FlowchartNode.NodeType.MERGE,
            "",
            15, 15
        );

        // True branch - Output "positivo"
        FlowchartNode outputPos = panel.createNode(
            FlowchartNode.NodeType.OUTPUT,
            "positivo",
            140, 60
        );
        panel.createEdge(cond1, outputPos, "True");
        panel.createEdge(outputPos, merge1, "");

        // False branch - Conditional N = 0
        FlowchartNode cond2 = panel.createNode(
            FlowchartNode.NodeType.CONDITIONAL,
            "N = 0",
            120, 80
        );
        panel.createEdge(cond1, cond2, "False");

        // Merge point 2
        FlowchartNode merge2 = panel.createNode(
            FlowchartNode.NodeType.MERGE,
            "",
            15, 15
        );

        // True branch from N=0 - Output "nullo"
        FlowchartNode outputNull = panel.createNode(
            FlowchartNode.NodeType.OUTPUT,
            "nullo",
            140, 60
        );
        panel.createEdge(cond2, outputNull, "True");
        panel.createEdge(outputNull, merge2, "");

        // False branch from N=0 - Output "negativo"
        FlowchartNode outputNeg = panel.createNode(
            FlowchartNode.NodeType.OUTPUT,
            "negativo",
            140, 60
        );
        panel.createEdge(cond2, outputNeg, "False");
        panel.createEdge(outputNeg, merge2, "");

        // Connect merges
        panel.createEdge(merge2, merge1, "");
        panel.createEdge(merge1, end, "");

        // Apply layout
        panel.applyLayout();
    }
}
