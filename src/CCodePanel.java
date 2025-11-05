import javax.swing.*;
import java.awt.*;

/**
 * Pannello per visualizzare il codice C-like generato dal flowchart.
 */
public class CCodePanel extends JPanel {

    private JTextArea codeArea;

    public CCodePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Codice C Generato"));

        // Area di testo per il codice C
        codeArea = new JTextArea();
        codeArea.setEditable(false);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        codeArea.setBackground(new Color(40, 44, 52)); // Dark background
        codeArea.setForeground(new Color(171, 178, 191)); // Light gray text
        codeArea.setCaretColor(Color.WHITE);
        codeArea.setTabSize(4);
        codeArea.setLineWrap(false);

        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        // Pulsante per copiare il codice
        JButton copyButton = new JButton("Copia Codice");
        copyButton.addActionListener(e -> copyCodeToClipboard());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(copyButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Messaggio iniziale
        setCode("// Il codice C apparirà qui quando costruisci il flowchart\n");
    }

    /**
     * Imposta il codice da visualizzare
     */
    public void setCode(String code) {
        codeArea.setText(code);
        codeArea.setCaretPosition(0);
    }

    /**
     * Ottiene il codice corrente
     */
    public String getCode() {
        return codeArea.getText();
    }

    /**
     * Pulisce il codice
     */
    public void clear() {
        codeArea.setText("");
    }

    /**
     * Copia il codice negli appunti
     */
    private void copyCodeToClipboard() {
        String code = codeArea.getText();
        if (code != null && !code.isEmpty()) {
            java.awt.datatransfer.StringSelection selection =
                new java.awt.datatransfer.StringSelection(code);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

            JOptionPane.showMessageDialog(this,
                "Codice copiato negli appunti!",
                "Copia completata",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
