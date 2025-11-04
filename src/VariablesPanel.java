import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * Pannello per visualizzare le variabili durante l'esecuzione.
 */
public class VariablesPanel extends JPanel {

    private JTable variablesTable;
    private DefaultTableModel tableModel;

    public VariablesPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Variables"));

        // Crea la tabella per le variabili
        String[] columnNames = {"Variable", "Value", "Type"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabella in sola lettura
            }
        };

        variablesTable = new JTable(tableModel);
        variablesTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        variablesTable.setRowHeight(25);

        // Colori alternati per le righe
        variablesTable.setShowGrid(true);
        variablesTable.setGridColor(new Color(200, 200, 200));

        JScrollPane scrollPane = new JScrollPane(variablesTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        // Info label
        JLabel infoLabel = new JLabel(" Variables will appear here during execution");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        infoLabel.setForeground(Color.GRAY);
        add(infoLabel, BorderLayout.SOUTH);
    }

    public void updateVariables(Map<String, Object> variables) {
        // Rimuovi tutte le righe esistenti
        tableModel.setRowCount(0);

        // Aggiungi le nuove variabili
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            String type = getTypeString(value);

            tableModel.addRow(new Object[]{name, value, type});
        }
    }

    private String getTypeString(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof Integer) {
            return "Integer";
        } else if (value instanceof Double) {
            return "Double";
        } else if (value instanceof Boolean) {
            return "Boolean";
        } else if (value instanceof String) {
            return "String";
        } else {
            return value.getClass().getSimpleName();
        }
    }

    public void clear() {
        tableModel.setRowCount(0);
    }
}
