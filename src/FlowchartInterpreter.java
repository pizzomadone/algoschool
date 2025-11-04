import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import javax.swing.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprete per eseguire i blocchi del diagramma a blocchi.
 * Gestisce variabili, input/output, condizioni e loop.
 */
public class FlowchartInterpreter {

    private mxGraph graph;
    private Map<String, Object> variables;
    private StringBuilder output;
    private Object currentCell;
    private Object startCell;
    private Object endCell;
    private boolean isRunning;
    private boolean isPaused;
    private ExecutionListener listener;

    // Stack per gestire i loop
    private Stack<LoopContext> loopStack;

    // Pattern per riconoscere le operazioni
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("(.+?)\\s*=\\s*(.+)");
    private static final Pattern INPUT_PATTERN = Pattern.compile("(?i)(?:I\\s*:|input)\\s*:?\\s*(.+)");
    private static final Pattern OUTPUT_PATTERN = Pattern.compile("(?i)(?:O\\s*:|output)\\s*:?\\s*(.+)");

    public interface ExecutionListener {
        void onExecutionStep(Object cell, Map<String, Object> variables, String output);
        void onExecutionComplete();
        void onExecutionError(String error);
        void onInputRequired(String variableName, InputCallback callback);
    }

    public interface InputCallback {
        void onInputProvided(String value);
    }

    private static class LoopContext {
        Object loopCell;
        Object loopBodyStartCell;

        LoopContext(Object loopCell, Object loopBodyStartCell) {
            this.loopCell = loopCell;
            this.loopBodyStartCell = loopBodyStartCell;
        }
    }

    public FlowchartInterpreter(mxGraph graph, Object startCell, Object endCell) {
        this.graph = graph;
        this.startCell = startCell;
        this.endCell = endCell;
        this.variables = new HashMap<>();
        this.output = new StringBuilder();
        this.loopStack = new Stack<>();
        this.isRunning = false;
        this.isPaused = false;
    }

    public void setExecutionListener(ExecutionListener listener) {
        this.listener = listener;
    }

    public void reset() {
        variables.clear();
        output = new StringBuilder();
        loopStack.clear();
        currentCell = startCell;
        isRunning = false;
        isPaused = false;
    }

    public void start() {
        reset();
        isRunning = true;
        currentCell = startCell;
        executeAll();
    }

    public void step() {
        if (!isRunning) {
            reset();
            isRunning = true;
            currentCell = startCell;
        }

        if (currentCell != null && currentCell != endCell) {
            executeStep();
        } else {
            stop();
        }
    }

    public void stop() {
        isRunning = false;
        isPaused = false;
        if (listener != null) {
            listener.onExecutionComplete();
        }
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
        executeAll();
    }

    private void executeAll() {
        while (isRunning && !isPaused && currentCell != null && currentCell != endCell) {
            executeStep();
        }

        if (currentCell == endCell) {
            stop();
        }
    }

    private void executeStep() {
        if (currentCell == null || currentCell == endCell) {
            stop();
            return;
        }

        try {
            mxCell cell = (mxCell) currentCell;
            String style = cell.getStyle();
            String value = (String) cell.getValue();

            // IMPORTANTE: Salva il blocco che stiamo per eseguire
            Object executingCell = currentCell;

            // Esegui il blocco in base al tipo
            if (FlowchartPanel.START.equals(style)) {
                // Blocco Start - passa al prossimo
                moveToNext(cell);

            } else if (FlowchartPanel.ASSIGNMENT.equals(style)) {
                // Blocco Assignment - esegui assegnamento
                executeAssignment(value);
                moveToNext(cell);

            } else if (FlowchartPanel.INPUT.equals(style)) {
                // Blocco Input - richiedi input all'utente
                executeInput(value);
                // moveToNext viene chiamato in requestInput dopo l'input
                // Non chiamare moveToNext qui perché siamo in pausa

            } else if (FlowchartPanel.OUTPUT.equals(style)) {
                // Blocco Output - visualizza output
                executeOutput(value);
                moveToNext(cell);

            } else if (FlowchartPanel.CONDITIONAL.equals(style)) {
                // Blocco Conditional - valuta condizione
                boolean result = evaluateCondition(value);
                moveToConditionalBranch(cell, result);

            } else if (FlowchartPanel.LOOP.equals(style)) {
                // Blocco Loop - valuta condizione loop
                boolean result = evaluateCondition(value);
                moveToLoopBranch(cell, result);

            } else if (FlowchartPanel.MERGE.equals(style)) {
                // Merge point - passa semplicemente al prossimo
                moveToNext(cell);

            } else {
                // Tipo sconosciuto - passa al prossimo
                moveToNext(cell);
            }

            // Notifica listener DOPO l'esecuzione con il blocco che abbiamo appena eseguito
            // Ora variabili e output sono già aggiornati dall'esecuzione
            if (listener != null && !isPaused) {
                listener.onExecutionStep(executingCell, new HashMap<>(variables), output.toString());
            }

        } catch (Exception e) {
            if (listener != null) {
                listener.onExecutionError("Errore durante l'esecuzione: " + e.getMessage());
            }
            stop();
        }
    }

    private void executeAssignment(String instruction) {
        Matcher matcher = ASSIGNMENT_PATTERN.matcher(instruction);
        if (matcher.matches()) {
            String varName = matcher.group(1).trim();
            String expression = matcher.group(2).trim();

            Object result = evaluateExpression(expression);
            variables.put(varName, result);
        } else {
            // Se non è un assegnamento, prova a valutare come espressione
            evaluateExpression(instruction);
        }
    }

    private void executeInput(String instruction) {
        Matcher inputMatcher = INPUT_PATTERN.matcher(instruction);

        if (inputMatcher.find()) {
            // Input - rimuovi il prefisso "I:" o "Input:" e ottieni i nomi delle variabili
            String varNames = inputMatcher.group(1).trim();
            String[] vars = varNames.split(",");

            for (String varName : vars) {
                varName = varName.trim();
                requestInput(varName);
            }
        } else {
            // Fallback: assume che l'intera stringa sia un nome di variabile
            String varName = instruction.replaceAll("(?i)^I\\s*:\\s*", "").trim();
            requestInput(varName);
        }
    }

    private void executeOutput(String instruction) {
        Matcher outputMatcher = OUTPUT_PATTERN.matcher(instruction);

        if (outputMatcher.find()) {
            // Output - rimuovi il prefisso "O:" o "Output:" e valuta l'espressione
            String expression = outputMatcher.group(1).trim();
            Object result = evaluateExpression(expression);
            output.append(result).append("\n");
        } else {
            // Fallback: rimuovi il prefisso "O:" se presente e valuta
            String expression = instruction.replaceAll("(?i)^O\\s*:\\s*", "").trim();
            Object result = evaluateExpression(expression);
            output.append(result).append("\n");
        }
    }

    private void requestInput(String varName) {
        // Richiedi input all'utente
        boolean wasRunningAutomatically = !isPaused && isRunning;

        // Salva il cell corrente per poterlo avanzare dopo l'input
        Object cellToAdvance = currentCell;

        // IMPORTANTE: Metti in pausa PRIMA di richiedere l'input
        isPaused = true;

        if (listener != null) {
            listener.onInputRequired(varName, value -> {
                try {
                    // Prova a convertire in numero
                    if (value.matches("-?\\d+")) {
                        variables.put(varName, Integer.parseInt(value));
                    } else if (value.matches("-?\\d+\\.\\d+")) {
                        variables.put(varName, Double.parseDouble(value));
                    } else {
                        variables.put(varName, value);
                    }
                } catch (Exception e) {
                    variables.put(varName, value);
                }

                // IMPORTANTE: Avanza al blocco successivo dopo aver ricevuto l'input
                moveToNext((mxCell) cellToAdvance);

                // Notifica il listener con le variabili aggiornate
                if (listener != null) {
                    listener.onExecutionStep(cellToAdvance, new HashMap<>(variables), output.toString());
                }

                // Togliamo la pausa dopo l'input
                isPaused = false;

                // Riprendi solo se eravamo in esecuzione automatica
                if (wasRunningAutomatically) {
                    resume();
                }
                // Se siamo in step-by-step, l'esecuzione è completa per questo step
                // il prossimo click su "Next Step" continuerà dal blocco successivo
            });
        }
    }

    private boolean evaluateCondition(String condition) {
        try {
            // Rimuovi il punto interrogativo se presente
            condition = condition.replaceAll("\\?", "").trim();

            // Pattern per condizioni semplici: var op value
            Pattern pattern = Pattern.compile("(.+?)\\s*([<>=!]+)\\s*(.+)");
            Matcher matcher = pattern.matcher(condition);

            if (matcher.matches()) {
                Object left = evaluateExpression(matcher.group(1).trim());
                String operator = matcher.group(2).trim();
                Object right = evaluateExpression(matcher.group(3).trim());

                return compareValues(left, operator, right);
            }

            // Se non è un confronto, valuta come espressione booleana
            Object result = evaluateExpression(condition);
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof Number) {
                return ((Number) result).doubleValue() != 0;
            }

        } catch (Exception e) {
            if (listener != null) {
                listener.onExecutionError("Errore nella valutazione della condizione: " + condition);
            }
        }

        return false;
    }

    private boolean compareValues(Object left, String operator, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();

            switch (operator) {
                case ">": return l > r;
                case "<": return l < r;
                case ">=": return l >= r;
                case "<=": return l <= r;
                case "==": case "=": return l == r;
                case "!=": return l != r;
            }
        } else {
            String l = String.valueOf(left);
            String r = String.valueOf(right);

            switch (operator) {
                case "==": case "=": return l.equals(r);
                case "!=": return !l.equals(r);
            }
        }

        return false;
    }

    private Object evaluateExpression(String expression) {
        expression = expression.trim();

        // Se è una variabile, restituisci il suo valore
        if (variables.containsKey(expression)) {
            return variables.get(expression);
        }

        // Se è un numero
        try {
            if (expression.matches("-?\\d+")) {
                return Integer.parseInt(expression);
            } else if (expression.matches("-?\\d+\\.\\d+")) {
                return Double.parseDouble(expression);
            }
        } catch (NumberFormatException e) {
            // Non è un numero
        }

        // Se è una stringa letterale (tra virgolette)
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }

        // Prova a valutare espressioni aritmetiche semplici
        try {
            return evaluateArithmeticExpression(expression);
        } catch (Exception e) {
            // Se non riesce, restituisci come stringa
            return expression;
        }
    }

    private Object evaluateArithmeticExpression(String expression) {
        // Sostituisci le variabili con i loro valori
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() instanceof Number) {
                expression = expression.replaceAll("\\b" + entry.getKey() + "\\b",
                    String.valueOf(entry.getValue()));
            }
        }

        // Valuta espressioni semplici come: a + b, a - b, a * b, a / b
        expression = expression.replaceAll("\\s+", "");

        // Gestisci moltiplicazione e divisione
        Pattern mulDivPattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)([*/])(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = mulDivPattern.matcher(expression);
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            String op = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            double result = op.equals("*") ? left * right : left / right;
            expression = expression.replace(matcher.group(), String.valueOf(result));
            matcher = mulDivPattern.matcher(expression);
        }

        // Gestisci addizione e sottrazione
        Pattern addSubPattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)([+\\-])(-?\\d+(?:\\.\\d+)?)");
        matcher = addSubPattern.matcher(expression);
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            String op = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            double result = op.equals("+") ? left + right : left - right;
            expression = expression.replace(matcher.group(), String.valueOf(result));
            matcher = addSubPattern.matcher(expression);
        }

        // Prova a parsare il risultato finale
        try {
            if (expression.contains(".")) {
                return Double.parseDouble(expression);
            } else {
                return Integer.parseInt(expression);
            }
        } catch (NumberFormatException e) {
            return expression;
        }
    }

    private void moveToNext(mxCell cell) {
        Object[] edges = graph.getOutgoingEdges(cell);
        if (edges.length > 0) {
            mxCell edge = (mxCell) edges[0];
            currentCell = edge.getTarget();
        } else {
            currentCell = null;
        }
    }

    private void moveToConditionalBranch(mxCell cell, boolean condition) {
        Object[] edges = graph.getOutgoingEdges(cell);

        // Cerca il branch giusto (True o False)
        for (Object edge : edges) {
            mxCell edgeCell = (mxCell) edge;
            String style = edgeCell.getStyle();
            String label = (String) edgeCell.getValue();

            if (condition) {
                // Cerca branch True (verde o con label "Sì" o "Yes")
                if ("TRUE_BRANCH".equals(style) ||
                    (label != null && (label.contains("Sì") || label.contains("Yes") || label.contains("True")))) {
                    currentCell = edgeCell.getTarget();
                    return;
                }
            } else {
                // Cerca branch False (rosso o con label "No")
                if ("FALSE_BRANCH".equals(style) ||
                    (label != null && (label.contains("No") || label.contains("False")))) {
                    currentCell = edgeCell.getTarget();
                    return;
                }
            }
        }

        // Fallback: prendi il primo edge disponibile
        if (edges.length > 0) {
            currentCell = ((mxCell) edges[0]).getTarget();
        } else {
            currentCell = null;
        }
    }

    private void moveToLoopBranch(mxCell cell, boolean condition) {
        if (condition) {
            // Entra nel corpo del loop
            Object[] edges = graph.getOutgoingEdges(cell);
            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                String style = edgeCell.getStyle();

                if ("TRUE_BRANCH".equals(style)) {
                    // Salva il contesto del loop
                    if (loopStack.isEmpty() || loopStack.peek().loopCell != cell) {
                        loopStack.push(new LoopContext(cell, edgeCell.getTarget()));
                    }
                    currentCell = edgeCell.getTarget();
                    return;
                }
            }
        } else {
            // Esci dal loop
            if (!loopStack.isEmpty() && loopStack.peek().loopCell == cell) {
                loopStack.pop();
            }

            Object[] edges = graph.getOutgoingEdges(cell);
            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                String style = edgeCell.getStyle();

                if ("FALSE_BRANCH".equals(style)) {
                    currentCell = edgeCell.getTarget();
                    return;
                }
            }
        }

        // Fallback
        Object[] edges = graph.getOutgoingEdges(cell);
        if (edges.length > 0) {
            currentCell = ((mxCell) edges[0]).getTarget();
        } else {
            currentCell = null;
        }
    }

    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    public String getOutput() {
        return output.toString();
    }

    public Object getCurrentCell() {
        return currentCell;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }
}
