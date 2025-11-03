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

    // Stati di esecuzione
    public enum ExecutionState {
        IDLE,       // Nessuna esecuzione in corso
        RUNNING,    // Esecuzione automatica in corso
        STEPPING    // Esecuzione step-by-step in corso
    }

    private ExecutionState currentState = ExecutionState.IDLE;

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

        runButton = new JButton("▶ Run All");
        runButton.setToolTipText("Execute the entire flowchart automatically");
        runButton.setFont(new Font("Arial", Font.BOLD, 12));
        runButton.setBackground(new Color(76, 175, 80));
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.addActionListener(e -> {
            if (listener != null) {
                listener.onRun();
            }
        });

        stepButton = new JButton("⏯ Next Step");
        stepButton.setToolTipText("<html>Execute one step at a time<br>Click repeatedly to advance through the flowchart</html>");
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
        stopButton.setToolTipText("Stop the current execution");
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
        resetButton.setToolTipText("Reset execution state and clear output");
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

    /**
     * Imposta lo stato di esecuzione e aggiorna i pulsanti di conseguenza
     */
    public void setState(ExecutionState state) {
        currentState = state;
        updateButtonStates();
    }

    /**
     * Aggiorna lo stato dei pulsanti in base allo stato corrente
     */
    private void updateButtonStates() {
        switch (currentState) {
            case IDLE:
                // Pronto per iniziare: Run e Step disponibili
                runButton.setEnabled(true);
                stepButton.setEnabled(true);
                stopButton.setEnabled(false);
                resetButton.setEnabled(true);
                break;

            case RUNNING:
                // Esecuzione automatica: solo Stop disponibile
                runButton.setEnabled(false);
                stepButton.setEnabled(false);
                stopButton.setEnabled(true);
                resetButton.setEnabled(false);
                break;

            case STEPPING:
                // Esecuzione step-by-step: Run per continuare automaticamente,
                // Step per il prossimo step, Stop per fermare
                runButton.setEnabled(true);
                stepButton.setEnabled(true);
                stopButton.setEnabled(true);
                resetButton.setEnabled(true);
                break;
        }
    }

    /**
     * Ritorna lo stato corrente
     */
    public ExecutionState getState() {
        return currentState;
    }

    // Metodi deprecati per compatibilità
    @Deprecated
    public void setRunning(boolean running) {
        setState(running ? ExecutionState.RUNNING : ExecutionState.IDLE);
    }

    @Deprecated
    public void setStepping(boolean stepping) {
        if (stepping) {
            setState(ExecutionState.STEPPING);
        }
    }
}
