import javax.swing.*;
import java.awt.*;

/**
 * Pannello per visualizzare l'output del programma durante l'esecuzione.
 */
public class OutputPanel extends JPanel {

    private JTextArea outputArea;

    public OutputPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Output"));

        // Area di testo per l'output
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.GREEN);
        outputArea.setCaretColor(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);

        // Pulsante per cancellare l'output
        JButton clearButton = new JButton("Clear Output");
        clearButton.addActionListener(e -> clear());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(clearButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void appendOutput(String text) {
        outputArea.append(text);
        // Scroll automaticamente verso il basso
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public void setOutput(String text) {
        outputArea.setText(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public void clear() {
        outputArea.setText("");
    }

    public String getOutput() {
        return outputArea.getText();
    }
}
