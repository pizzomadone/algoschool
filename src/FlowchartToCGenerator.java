import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

import java.util.*;

/**
 * Converte un flowchart in codice C-like.
 * Attraversa il grafo e genera codice C per ogni blocco.
 */
public class FlowchartToCGenerator {

    private mxGraph graph;
    private Object startCell;
    private Object endCell;
    private StringBuilder code;
    private int indentLevel;
    private Set<Object> visitedCells;
    private Map<Object, String> loopLabels;
    private int loopCounter;
    private FlowchartPanel flowchartPanel;  // For accessing function definitions

    public FlowchartToCGenerator(mxGraph graph, Object startCell, Object endCell) {
        this(graph, startCell, endCell, null);
    }

    public FlowchartToCGenerator(mxGraph graph, Object startCell, Object endCell, FlowchartPanel flowchartPanel) {
        this.graph = graph;
        this.startCell = startCell;
        this.endCell = endCell;
        this.flowchartPanel = flowchartPanel;
        this.code = new StringBuilder();
        this.indentLevel = 0;
        this.visitedCells = new HashSet<>();
        this.loopLabels = new HashMap<>();
        this.loopCounter = 0;
    }

    /**
     * Genera il codice C-like dal flowchart
     */
    public String generateCode() {
        code.setLength(0);
        visitedCells.clear();
        loopLabels.clear();
        loopCounter = 0;
        indentLevel = 0;

        // Verifica che abbiamo Start e End
        if (startCell == null || endCell == null) {
            return "// Flowchart incompleto: manca Start o End\n";
        }

        // Header del programma
        appendLine("#include <stdio.h>");
        appendLine("#include <stdlib.h>");
        appendLine("");

        // Genera le funzioni definite (se presenti)
        if (flowchartPanel != null) {
            Map<String, FunctionDefinition> functions = flowchartPanel.getFunctions();
            if (functions != null && !functions.isEmpty()) {
                for (Map.Entry<String, FunctionDefinition> entry : functions.entrySet()) {
                    generateFunction(entry.getKey(), entry.getValue());
                    appendLine("");
                }
            }
        }

        // Main function
        appendLine("int main() {");
        indentLevel++;

        // Genera il corpo del main
        try {
            generateFromCell(startCell);
        } catch (Exception e) {
            appendLine("// Errore durante la generazione: " + e.getMessage());
        }

        // Footer del programma
        indentLevel--;
        appendLine("    return 0;");
        appendLine("}");

        return code.toString();
    }

    /**
     * Genera codice partendo da una cella specifica
     */
    private void generateFromCell(Object cell) {
        if (cell == null || cell == endCell) {
            return;
        }

        // Evita cicli infiniti - se la cella è già stata visitata, non processarla di nuovo
        if (visitedCells.contains(cell)) {
            return;
        }

        mxCell mxCell = (mxCell) cell;
        String style = mxCell.getStyle();
        String value = mxCell.getValue() != null ? mxCell.getValue().toString() : "";

        // Skip Start e Merge points
        if (FlowchartPanel.START.equals(style)) {
            // Passa al prossimo blocco
            Object next = getNextCell(cell);
            generateFromCell(next);
            return;
        }

        if (FlowchartPanel.MERGE.equals(style)) {
            // I merge points non generano codice, passa oltre
            visitedCells.add(cell);
            Object next = getNextCell(cell);
            generateFromCell(next);
            return;
        }

        // Marca come visitato
        visitedCells.add(cell);

        // Genera codice in base al tipo
        if (FlowchartPanel.ASSIGNMENT.equals(style)) {
            generateAssignment(value);
            Object next = getNextCell(cell);
            generateFromCell(next);

        } else if (FlowchartPanel.INPUT.equals(style)) {
            generateInput(value);
            Object next = getNextCell(cell);
            generateFromCell(next);

        } else if (FlowchartPanel.OUTPUT.equals(style)) {
            generateOutput(value);
            Object next = getNextCell(cell);
            generateFromCell(next);

        } else if (FlowchartPanel.CONDITIONAL.equals(style)) {
            generateConditional(cell, value);

        } else if (FlowchartPanel.LOOP.equals(style)) {
            generateWhileLoop(cell, value);

        } else if (FlowchartPanel.FOR_LOOP.equals(style)) {
            generateForLoop(cell, value);

        } else if (FlowchartPanel.DO_WHILE.equals(style)) {
            generateDoWhileLoop(cell, value);

        } else if (FlowchartPanel.RETURN.equals(style)) {
            generateReturn(value);
            // RETURN termina l'esecuzione, non proseguire

        } else {
            // Blocco sconosciuto
            appendLine("// Blocco sconosciuto: " + value);
            Object next = getNextCell(cell);
            generateFromCell(next);
        }
    }

    /**
     * Genera codice per un assignment
     */
    private void generateAssignment(String value) {
        String cleanValue = value.trim();
        if (!cleanValue.endsWith(";")) {
            cleanValue += ";";
        }
        appendLine(cleanValue);
    }

    /**
     * Genera codice per input
     */
    private void generateInput(String value) {
        String varName = value.trim();
        // Rimuovi eventuale prefisso "I:" o "I: "
        varName = varName.replaceFirst("^I:\\s*", "");

        appendLine("int " + varName + ";");
        appendLine("scanf(\"%d\", &" + varName + ");");
    }

    /**
     * Genera codice per output
     */
    private void generateOutput(String value) {
        String outputValue = value.trim();
        // Rimuovi eventuale prefisso "O:" o "O: "
        outputValue = outputValue.replaceFirst("^O:\\s*", "");

        // Se è una stringa letterale (tra virgolette)
        if (outputValue.startsWith("\"") && outputValue.endsWith("\"")) {
            appendLine("printf(" + outputValue + ");");
        } else {
            // È una variabile o espressione
            appendLine("printf(\"%d\\n\", " + outputValue + ");");
        }
    }

    /**
     * Genera codice per un blocco condizionale (IF)
     */
    private void generateConditional(Object cell, String condition) {
        // Rimuovi il "?" finale se presente
        String cleanCondition = condition.trim();
        if (cleanCondition.endsWith("?")) {
            cleanCondition = cleanCondition.substring(0, cleanCondition.length() - 1).trim();
        }

        appendLine("if (" + cleanCondition + ") {");
        indentLevel++;

        // Trova il ramo TRUE (Sì)
        Object trueBranch = findBranchTarget(cell, "Sì");
        if (trueBranch != null) {
            generateFromCell(trueBranch);
        }

        indentLevel--;

        // Trova il ramo FALSE (No)
        Object falseBranch = findBranchTarget(cell, "No");
        if (falseBranch != null) {
            appendLine("} else {");
            indentLevel++;
            generateFromCell(falseBranch);
            indentLevel--;
        }

        appendLine("}");

        // Dopo l'if-else, continua con il merge point
        Object mergePoint = findMergePoint(cell);
        if (mergePoint != null) {
            Object next = getNextCell(mergePoint);
            generateFromCell(next);
        }
    }

    /**
     * Genera codice per un while loop
     */
    private void generateWhileLoop(Object cell, String condition) {
        String cleanCondition = condition.trim();

        appendLine("while (" + cleanCondition + ") {");
        indentLevel++;

        // Trova il corpo del loop (ramo Yes)
        Object loopBody = findBranchTarget(cell, "Yes");
        if (loopBody != null) {
            generateFromCell(loopBody);
        }

        indentLevel--;
        appendLine("}");

        // Continua con il ramo No (uscita dal loop)
        Object exitBranch = findBranchTarget(cell, "No");
        if (exitBranch != null) {
            generateFromCell(exitBranch);
        }
    }

    /**
     * Genera codice per un for loop
     */
    private void generateForLoop(Object cell, String forSpec) {
        // Il forSpec è nel formato: "i = 0; i < n; i = i + 1"
        String cleanSpec = forSpec.trim();

        appendLine("for (" + cleanSpec + ") {");
        indentLevel++;

        // Trova il corpo del loop (ramo Yes)
        Object loopBody = findBranchTarget(cell, "Yes");
        if (loopBody != null) {
            generateFromCell(loopBody);
        }

        indentLevel--;
        appendLine("}");

        // Continua con il ramo No (uscita dal loop)
        Object exitBranch = findBranchTarget(cell, "No");
        if (exitBranch != null) {
            generateFromCell(exitBranch);
        }
    }

    /**
     * Genera codice per un do-while loop
     */
    private void generateDoWhileLoop(Object cell, String condition) {
        String cleanCondition = condition.trim();

        appendLine("do {");
        indentLevel++;

        // Nel do-while, il corpo viene prima della condizione
        // Trova il merge point che rappresenta il corpo
        Object[] incomingEdges = graph.getIncomingEdges(cell);
        if (incomingEdges.length > 0) {
            mxCell edge = (mxCell) incomingEdges[0];
            Object bodyStart = edge.getSource();
            if (bodyStart != null && !FlowchartPanel.DO_WHILE.equals(((mxCell) bodyStart).getStyle())) {
                generateFromCell(bodyStart);
            }
        }

        indentLevel--;
        appendLine("} while (" + cleanCondition + ");");

        // Continua con il ramo No (uscita dal loop)
        Object exitBranch = findBranchTarget(cell, "No");
        if (exitBranch != null) {
            generateFromCell(exitBranch);
        }
    }

    /**
     * Trova la cella di destinazione di un branch (Sì/No/Yes)
     */
    private Object findBranchTarget(Object cell, String branchLabel) {
        Object[] edges = graph.getOutgoingEdges(cell);
        for (Object edge : edges) {
            mxCell edgeCell = (mxCell) edge;
            String label = edgeCell.getValue() != null ? edgeCell.getValue().toString() : "";
            if (branchLabel.equals(label)) {
                return edgeCell.getTarget();
            }
        }
        return null;
    }

    /**
     * Trova il merge point dopo un condizionale
     */
    private Object findMergePoint(Object cell) {
        // Il merge point è il primo MERGE che troviamo seguendo i rami
        Object[] edges = graph.getOutgoingEdges(cell);
        for (Object edge : edges) {
            mxCell edgeCell = (mxCell) edge;
            Object target = edgeCell.getTarget();
            if (target != null) {
                mxCell targetCell = (mxCell) target;
                if (FlowchartPanel.MERGE.equals(targetCell.getStyle())) {
                    return target;
                }
                // Cerca ricorsivamente
                Object merge = findMergePointRecursive(target, new HashSet<>());
                if (merge != null) {
                    return merge;
                }
            }
        }
        return null;
    }

    private Object findMergePointRecursive(Object cell, Set<Object> visited) {
        if (cell == null || visited.contains(cell)) {
            return null;
        }
        visited.add(cell);

        mxCell mxCell = (mxCell) cell;
        if (FlowchartPanel.MERGE.equals(mxCell.getStyle())) {
            return cell;
        }

        Object[] edges = graph.getOutgoingEdges(cell);
        for (Object edge : edges) {
            mxCell edgeCell = (mxCell) edge;
            Object target = edgeCell.getTarget();
            Object merge = findMergePointRecursive(target, visited);
            if (merge != null) {
                return merge;
            }
        }
        return null;
    }

    /**
     * Ottiene la prossima cella da processare
     */
    private Object getNextCell(Object cell) {
        Object[] edges = graph.getOutgoingEdges(cell);
        if (edges.length > 0) {
            // Se ci sono più edge, evita di seguire quelli che puntano a loop già visitati
            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                Object target = edgeCell.getTarget();
                // Salta i loop back (edge che puntano a loop già visitati)
                if (target != null && isLoopCell(target) && visitedCells.contains(target)) {
                    continue;
                }
                return target;
            }
            // Se tutti gli edge puntano a loop visitati, non c'è prossima cella
            return null;
        }
        return null;
    }

    /**
     * Verifica se una cella è un loop
     */
    private boolean isLoopCell(Object cell) {
        if (cell == null) return false;
        mxCell mxCell = (mxCell) cell;
        String style = mxCell.getStyle();
        return FlowchartPanel.LOOP.equals(style) ||
               FlowchartPanel.FOR_LOOP.equals(style) ||
               FlowchartPanel.DO_WHILE.equals(style);
    }

    /**
     * Aggiunge una riga di codice con indentazione
     */
    private void appendLine(String line) {
        for (int i = 0; i < indentLevel; i++) {
            code.append("    ");
        }
        code.append(line);
        code.append("\n");
    }

    // ===== FUNCTION GENERATION =====

    /**
     * Genera il codice C per una funzione definita
     */
    private void generateFunction(String functionName, FunctionDefinition funcDef) {
        mxGraph funcGraph = funcDef.getFunctionGraph();
        Object funcStart = funcDef.getStartCell();
        Object funcEnd = funcDef.getEndCell();

        if (funcGraph == null || funcStart == null || funcEnd == null) {
            appendLine("// Funzione " + functionName + " incompleta");
            return;
        }

        // Get parameter names from INPUT blocks
        List<String> paramNames = getFunctionParameterNames(funcGraph, funcStart);

        // Generate function signature
        StringBuilder signature = new StringBuilder("int ");
        signature.append(functionName).append("(");

        if (paramNames.isEmpty()) {
            signature.append("void");
        } else {
            for (int i = 0; i < paramNames.size(); i++) {
                if (i > 0) signature.append(", ");
                signature.append("int ").append(paramNames.get(i));
            }
        }
        signature.append(") {");

        appendLine(signature.toString());
        indentLevel++;

        // Save current state
        mxGraph savedGraph = this.graph;
        Object savedStart = this.startCell;
        Object savedEnd = this.endCell;
        Set<Object> savedVisited = this.visitedCells;
        Map<Object, String> savedLoopLabels = this.loopLabels;

        // Switch to function graph
        this.graph = funcGraph;
        this.startCell = funcStart;
        this.endCell = funcEnd;
        this.visitedCells = new HashSet<>();
        this.loopLabels = new HashMap<>();

        // Generate function body
        try {
            generateFromCell(funcStart);
        } catch (Exception e) {
            appendLine("// Errore nella generazione della funzione: " + e.getMessage());
        }

        // Restore state
        this.graph = savedGraph;
        this.startCell = savedStart;
        this.endCell = savedEnd;
        this.visitedCells = savedVisited;
        this.loopLabels = savedLoopLabels;

        indentLevel--;
        appendLine("}");
    }

    /**
     * Gets parameter names from INPUT blocks at the start of a function
     */
    private List<String> getFunctionParameterNames(mxGraph funcGraph, Object funcStart) {
        List<String> params = new ArrayList<>();
        Object current = getNextCellInGraph(funcGraph, funcStart);

        while (current != null) {
            if (current instanceof mxCell) {
                mxCell cell = (mxCell) current;
                String style = cell.getStyle();

                if (FlowchartPanel.INPUT.equals(style)) {
                    // Extract parameter name from INPUT block
                    String value = cell.getValue() != null ? cell.getValue().toString() : "";
                    String varName = value.trim().replaceFirst("^I:\\s*", "");
                    params.add(varName);
                    current = getNextCellInGraph(funcGraph, current);
                } else {
                    // No more INPUT blocks
                    break;
                }
            } else {
                break;
            }
        }

        return params;
    }

    /**
     * Gets the next cell in a specific graph
     */
    private Object getNextCellInGraph(mxGraph graph, Object cell) {
        Object[] edges = graph.getOutgoingEdges(cell);
        if (edges != null && edges.length > 0) {
            mxCell edgeCell = (mxCell) edges[0];
            return edgeCell.getTarget();
        }
        return null;
    }

    /**
     * Genera codice per un blocco RETURN
     */
    private void generateReturn(String value) {
        String returnValue = value.trim();

        // Remove "return" keyword if present
        if (returnValue.toLowerCase().startsWith("return")) {
            returnValue = returnValue.substring(6).trim();
        }

        if (returnValue.isEmpty()) {
            appendLine("return 0;");
        } else {
            appendLine("return " + returnValue + ";");
        }
    }
}
