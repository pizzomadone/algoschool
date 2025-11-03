import javax.swing.*;
import java.awt.*;

/**
 * Pannello per i controlli di esecuzione del flowchart.
 * Contiene i pulsanti Run, Step, Stop e Reset.
 */
public class ExecutionControlPanel extends JPanel {

    private JButton runButton;
    private JButton stepButton;
    private JButton stopButton;
    private JButton resetButton;
    private JLabel statusLabel;

    private ExecutionControlListener listener;

    public interface ExecutionControlListener {
        void onRun();
        void onStep();
        void onStop();
        void onReset();
    }

    public ExecutionControlPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Execution Controls"));

        // Pannello pulsanti
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        runButton = new JButton("▶ Run");
        runButton.setToolTipText("Execute the entire flowchart");
        runButton.setFont(new Font("Arial", Font.BOLD, 12));
        runButton.setBackground(new Color(76, 175, 80));
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.addActionListener(e -> {
            if (listener != null) {
                listener.onRun();
            }
        });

        stepButton = new JButton("⏯ Step");
        stepButton.setToolTipText("Execute one step at a time");
        stepButton.setFont(new Font("Arial", Font.BOLD, 12));
        stepButton.setBackground(new Color(33, 150, 243));
        stepButton.setForeground(Color.WHITE);
        stepButton.setFocusPainted(false);
        stepButton.addActionListener(e -> {
            if (listener != null) {
                listener.onStep();
            }
        });

        stopButton = new JButton("⏹ Stop");
        stopButton.setToolTipText("Stop execution");
        stopButton.setFont(new Font("Arial", Font.BOLD, 12));
        stopButton.setBackground(new Color(244, 67, 54));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> {
            if (listener != null) {
                listener.onStop();
            }
        });

        resetButton = new JButton("↻ Reset");
        resetButton.setToolTipText("Reset execution state");
        resetButton.setFont(new Font("Arial", Font.BOLD, 12));
        resetButton.setBackground(new Color(158, 158, 158));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> {
            if (listener != null) {
                listener.onReset();
            }
        });

        buttonsPanel.add(runButton);
        buttonsPanel.add(stepButton);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(resetButton);

        add(buttonsPanel, BorderLayout.WEST);

        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.CENTER);
    }

    public void setExecutionControlListener(ExecutionControlListener listener) {
        this.listener = listener;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setRunning(boolean running) {
        runButton.setEnabled(!running);
        stepButton.setEnabled(!running);
        stopButton.setEnabled(running);
        resetButton.setEnabled(!running);
    }

    public void setStepping(boolean stepping) {
        if (stepping) {
            runButton.setEnabled(true);
            stepButton.setEnabled(true);
            stopButton.setEnabled(true);
            resetButton.setEnabled(true);
        }
    }
}
