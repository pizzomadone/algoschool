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
    private volatile boolean isSteppingMode;  // Aggiunto per tracciare la modalità step-by-step (volatile per thread-safety)
    private volatile boolean isExecutingStep;  // Flag per prevenire esecuzioni multiple simultanee
    private ExecutionListener listener;

    // Stack per gestire i loop
    private Stack<LoopContext> loopStack;

    // Function management
    private Stack<FunctionContext> callStack;
    private FlowchartPanel flowchartPanel;  // Reference to access function definitions

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
        this(graph, startCell, endCell, null);
    }

    public FlowchartInterpreter(mxGraph graph, Object startCell, Object endCell, FlowchartPanel flowchartPanel) {
        this.graph = graph;
        this.startCell = startCell;
        this.endCell = endCell;
        this.flowchartPanel = flowchartPanel;
        this.variables = new HashMap<>();
        this.output = new StringBuilder();
        this.loopStack = new Stack<>();
        this.callStack = new Stack<>();
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
        isSteppingMode = false;
        isExecutingStep = false;
    }

    public void start() {
        reset();
        isRunning = true;
        isSteppingMode = false;  // Esecuzione automatica
        currentCell = startCell;
        executeAll();
    }

    public synchronized void step() {
        // Previeni esecuzioni multiple simultanee
        if (isExecutingStep) {
            return;
        }

        isExecutingStep = true;

        try {
            if (!isRunning) {
                reset();
                isRunning = true;
                isSteppingMode = true;  // Modalità step-by-step
                currentCell = startCell;
            }

            if (currentCell != null && currentCell != endCell) {
                executeStep();
            } else {
                stop();
            }
        } finally {
            isExecutingStep = false;
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
        isSteppingMode = false;  // Quando si riprende, si passa a esecuzione automatica
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
                output.append("▶ IF: Evaluating condition '").append(value).append("'\n");
                boolean result = evaluateCondition(value);
                output.append("  → Condition is ").append(result ? "TRUE" : "FALSE");
                output.append(result ? " (taking YES branch)\n" : " (taking NO branch)\n");
                moveToConditionalBranch(cell, result);

            } else if (FlowchartPanel.LOOP.equals(style)) {
                // Blocco Loop - valuta condizione loop
                System.out.println("\n▶ Executing LOOP block: " + value);
                System.out.println("Current variables: " + variables);
                output.append("▶ WHILE LOOP: Evaluating condition '").append(value).append("'\n");
                boolean result = evaluateCondition(value);
                System.out.println("Condition result: " + result);
                output.append("  → Condition is ").append(result ? "TRUE" : "FALSE");
                output.append(result ? " (entering loop body)\n" : " (exiting loop)\n");
                moveToLoopBranch(cell, result);

            } else if (FlowchartPanel.FOR_LOOP.equals(style)) {
                // Blocco For Loop - formato: init; condition; increment
                System.out.println("\n▶ Executing FOR LOOP block: " + value);
                System.out.println("Current variables: " + variables);
                output.append("▶ FOR LOOP: Processing '").append(value).append("'\n");

                // Parse the for loop: init; condition; increment
                String[] parts = value.split(";");
                if (parts.length == 3) {
                    String init = parts[0].trim();
                    String condition = parts[1].trim();
                    String increment = parts[2].trim();

                    // Check if this is the first time we enter the for loop
                    if (loopStack.isEmpty() || loopStack.peek().loopCell != cell) {
                        // First entry: execute initialization
                        output.append("  → Initialization: ").append(init).append("\n");
                        executeAssignment(init);
                        output.append("  → Evaluating condition: ").append(condition).append("\n");
                        boolean result = evaluateCondition(condition);
                        output.append("  → Condition is ").append(result ? "TRUE" : "FALSE");
                        output.append(result ? " (entering loop body)\n" : " (exiting loop)\n");
                        moveToLoopBranch(cell, result);
                    } else {
                        // Re-entering: execute increment, then check condition
                        output.append("  → Increment: ").append(increment).append("\n");
                        executeAssignment(increment);
                        output.append("  → Evaluating condition: ").append(condition).append("\n");
                        boolean result = evaluateCondition(condition);
                        output.append("  → Condition is ").append(result ? "TRUE" : "FALSE");
                        output.append(result ? " (continuing loop)\n" : " (exiting loop)\n");
                        moveToLoopBranch(cell, result);
                    }
                } else {
                    // Malformed for loop - treat as simple condition
                    boolean result = evaluateCondition(value);
                    output.append("  → Condition is ").append(result ? "TRUE" : "FALSE");
                    output.append(result ? " (entering loop body)\n" : " (exiting loop)\n");
                    moveToLoopBranch(cell, result);
                }

            } else if (FlowchartPanel.DO_WHILE.equals(style)) {
                // Blocco Do-While - valuta condizione dopo il corpo
                System.out.println("\n▶ Executing DO-WHILE block: " + value);
                System.out.println("Current variables: " + variables);
                output.append("▶ DO-WHILE: Evaluating condition '").append(value).append("'\n");
                boolean result = evaluateCondition(value);
                System.out.println("Condition result: " + result);
                output.append("  → Condition is ").append(result ? "TRUE" : "FALSE");
                output.append(result ? " (repeating loop body)\n" : " (exiting loop)\n");
                moveToLoopBranch(cell, result);

            } else if (FlowchartPanel.FUNCTION_CALL.equals(style)) {
                // Blocco Function Call - chiama una funzione
                executeFunctionCallBlock(value);
                moveToNext(cell);

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

            output.append("▶ ASSIGNMENT: Evaluating '").append(expression).append("'\n");
            Object result = evaluateExpression(expression);
            setVariable(varName, result);
            output.append("  → Variable '").append(varName).append("' = ").append(result).append("\n");
        } else {
            // Se non è un assegnamento, prova a valutare come espressione
            evaluateExpression(instruction);
        }
    }

    private void executeInput(String instruction) {
        // Con il nuovo formato, il testo contiene solo i nomi delle variabili
        // (senza "I:" perché ora è visualizzato fuori dal blocco)
        instruction = instruction.trim();

        // Rimuovi eventuali prefissi "I:" o "Input:" se presenti (per compatibilità)
        Matcher inputMatcher = INPUT_PATTERN.matcher(instruction);
        String varNames;

        if (inputMatcher.find()) {
            varNames = inputMatcher.group(1).trim();
        } else {
            varNames = instruction.replaceAll("(?i)^I\\s*:\\s*", "").trim();
        }

        // Gestisci multiple variabili separate da virgola
        String[] vars = varNames.split(",");

        for (String varName : vars) {
            varName = varName.trim();
            if (!varName.isEmpty()) {
                output.append("▶ INPUT: Requesting value for variable '").append(varName).append("'\n");
                requestInput(varName);
            }
        }
    }

    private void executeOutput(String instruction) {
        // Con il nuovo formato, il testo contiene solo l'espressione o stringa
        // (senza "O:" perché ora è visualizzato fuori dal blocco)
        instruction = instruction.trim();

        // Rimuovi eventuali prefissi "O:" o "Output:" se presenti (per compatibilità)
        Matcher outputMatcher = OUTPUT_PATTERN.matcher(instruction);
        String expression;

        if (outputMatcher.find()) {
            expression = outputMatcher.group(1).trim();
        } else {
            expression = instruction.replaceAll("(?i)^O\\s*:\\s*", "").trim();
        }

        // Valuta l'espressione
        // Se è una stringa tra virgolette, evaluateExpression la restituirà senza virgolette
        // Se è una variabile o un'espressione, la valuterà
        output.append("▶ OUTPUT: ");
        Object result = evaluateExpression(expression);
        output.append(result).append("\n");
    }

    private void requestInput(String varName) {
        // Richiedi input all'utente

        // Salva il cell corrente per poterlo avanzare dopo l'input
        Object cellToAdvance = currentCell;

        // IMPORTANTE: Metti in pausa PRIMA di richiedere l'input
        isPaused = true;

        if (listener != null) {
            listener.onInputRequired(varName, value -> {
                try {
                    // Prova a convertire in numero
                    if (value.matches("-?\\d+")) {
                        setVariable(varName, Integer.parseInt(value));
                        output.append("  → User entered: ").append(value).append(" (stored as Integer)\n");
                    } else if (value.matches("-?\\d+\\.\\d+")) {
                        setVariable(varName, Double.parseDouble(value));
                        output.append("  → User entered: ").append(value).append(" (stored as Double)\n");
                    } else {
                        setVariable(varName, value);
                        output.append("  → User entered: \"").append(value).append("\" (stored as String)\n");
                    }
                } catch (Exception e) {
                    setVariable(varName, value);
                    output.append("  → User entered: \"").append(value).append("\"\n");
                }

                // IMPORTANTE: Avanza al blocco successivo dopo aver ricevuto l'input
                moveToNext((mxCell) cellToAdvance);

                // Notifica il listener con le variabili aggiornate
                if (listener != null) {
                    listener.onExecutionStep(cellToAdvance, new HashMap<>(variables), output.toString());
                }

                // Togliamo la pausa dopo l'input
                isPaused = false;

                // Riprendi solo se NON siamo in modalità step-by-step
                if (!isSteppingMode) {
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

            // Gestisci operatori logici AND (&&, AND, &)
            if (condition.matches(".*\\s+(AND|&&|&)\\s+.*")) {
                String[] parts = condition.split("\\s+(AND|&&|&)\\s+", 2);
                if (parts.length == 2) {
                    return evaluateCondition(parts[0].trim()) && evaluateCondition(parts[1].trim());
                }
            }

            // Gestisci operatori logici OR (||, OR, |)
            if (condition.matches(".*\\s+(OR|\\|\\||\\|)\\s+.*")) {
                String[] parts = condition.split("\\s+(OR|\\|\\||\\|)\\s+", 2);
                if (parts.length == 2) {
                    return evaluateCondition(parts[0].trim()) || evaluateCondition(parts[1].trim());
                }
            }

            // Gestisci operatore logico NOT (!, NOT)
            if (condition.matches("^(NOT|!)\\s+.*")) {
                String subCondition = condition.replaceFirst("^(NOT|!)\\s+", "").trim();
                return !evaluateCondition(subCondition);
            }

            // Gestisci parentesi per raggruppamento
            if (condition.startsWith("(") && condition.endsWith(")")) {
                return evaluateCondition(condition.substring(1, condition.length() - 1).trim());
            }

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
                listener.onExecutionError("Errore nella valutazione della condizione: " + condition + " - " + e.getMessage());
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

        // Se è un numero, valutalo prima di controllare le variabili
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

        // Controlla se è una chiamata di funzione: nomeFunzione(arg1, arg2, ...)
        if (expression.matches("\\w+\\s*\\(.+\\)")) {
            return evaluateFunctionCall(expression);
        }

        // Prova a valutare espressioni aritmetiche semplici (potrebbero contenere variabili)
        try {
            Object arithmeticResult = evaluateArithmeticExpression(expression);
            // Se il risultato è diverso dall'espressione originale, è stata valutata con successo
            if (arithmeticResult != null && !arithmeticResult.toString().equals(expression)) {
                return arithmeticResult;
            }
        } catch (Exception e) {
            // Non è un'espressione aritmetica valida
        }

        // Se è una variabile, restituisci il suo valore
        if (hasVariable(expression)) {
            return getVariable(expression);
        }

        // Se arriviamo qui, è una variabile non dichiarata o espressione non valida
        throw new RuntimeException("Variable '" + expression + "' is not defined");
    }

    private Object evaluateArithmeticExpression(String expression) {
        // Sostituisci le variabili con i loro valori
        Map<String, Object> allVars = getAllVariables();
        for (Map.Entry<String, Object> entry : allVars.entrySet()) {
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
        Object[] edges = graph.getOutgoingEdges(cell);

        // Debug: stampa info sugli archi
        System.out.println("\n=== DEBUG LOOP BRANCH ===");
        System.out.println("Loop cell: " + cell.getValue());
        System.out.println("Condition evaluated to: " + condition);
        System.out.println("Number of outgoing edges: " + edges.length);

        for (int i = 0; i < edges.length; i++) {
            mxCell edgeCell = (mxCell) edges[i];
            String style = edgeCell.getStyle();
            String label = (String) edgeCell.getValue();
            Object target = edgeCell.getTarget();
            String targetValue = target != null ? String.valueOf(((mxCell)target).getValue()) : "null";

            System.out.println("Edge " + i + ":");
            System.out.println("  Label: " + label);
            System.out.println("  Style: " + style);
            System.out.println("  Target: " + targetValue);
        }

        if (condition) {
            // Entra nel corpo del loop - cerca TRUE_BRANCH
            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                String style = edgeCell.getStyle();
                String label = (String) edgeCell.getValue();

                // Controlla sia lo stile che l'etichetta per maggiore robustezza
                boolean isTrueBranch = (style != null && style.contains("TRUE_BRANCH")) ||
                                      (label != null && (label.equals("Yes") || label.equals("Sì") || label.equals("Si")));

                if (isTrueBranch) {
                    System.out.println("→ Following TRUE branch to: " + ((mxCell)edgeCell.getTarget()).getValue());

                    // Salva il contesto del loop
                    if (loopStack.isEmpty() || loopStack.peek().loopCell != cell) {
                        loopStack.push(new LoopContext(cell, edgeCell.getTarget()));
                        System.out.println("→ Pushed loop context to stack");
                    }
                    currentCell = edgeCell.getTarget();
                    return;
                }
            }
            System.out.println("⚠ WARNING: TRUE branch not found! Using fallback.");
        } else {
            // Esci dal loop - cerca FALSE_BRANCH
            if (!loopStack.isEmpty() && loopStack.peek().loopCell == cell) {
                loopStack.pop();
                System.out.println("→ Popped loop context from stack");
            }

            for (Object edge : edges) {
                mxCell edgeCell = (mxCell) edge;
                String style = edgeCell.getStyle();
                String label = (String) edgeCell.getValue();

                // Controlla sia lo stile che l'etichetta per maggiore robustezza
                boolean isFalseBranch = (style != null && style.contains("FALSE_BRANCH")) ||
                                       (label != null && label.equals("No"));

                if (isFalseBranch) {
                    System.out.println("→ Following FALSE branch to: " + ((mxCell)edgeCell.getTarget()).getValue());
                    currentCell = edgeCell.getTarget();
                    return;
                }
            }
            System.out.println("⚠ WARNING: FALSE branch not found! Using fallback.");
        }

        // Fallback: prendi il primo edge disponibile
        System.out.println("→ Using fallback: taking first edge");
        if (edges.length > 0) {
            currentCell = ((mxCell) edges[0]).getTarget();
            System.out.println("→ Fallback target: " + ((mxCell)currentCell).getValue());
        } else {
            currentCell = null;
            System.out.println("→ No edges available!");
        }
        System.out.println("=========================\n");
    }

    public Map<String, Object> getVariables() {
        return getAllVariables();
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

    // ===== FUNCTION CALL MANAGEMENT =====

    /**
     * Evaluates a function call expression and returns the result.
     * Format: functionName(arg1, arg2, ...)
     */
    private Object evaluateFunctionCall(String expression) {
        // Parse function name and arguments
        int openParen = expression.indexOf('(');
        int closeParen = expression.lastIndexOf(')');

        if (openParen == -1 || closeParen == -1) {
            throw new RuntimeException("Invalid function call syntax: " + expression);
        }

        String functionName = expression.substring(0, openParen).trim();
        String argsStr = expression.substring(openParen + 1, closeParen).trim();

        // Parse arguments
        List<Object> argValues = new ArrayList<>();
        if (!argsStr.isEmpty()) {
            String[] args = argsStr.split(",");
            for (String arg : args) {
                argValues.add(evaluateExpression(arg.trim()));
            }
        }

        // Execute the function call
        return executeFunctionCall(functionName, argValues);
    }

    /**
     * Executes a function call.
     */
    private Object executeFunctionCall(String functionName, List<Object> argValues) {
        if (flowchartPanel == null) {
            throw new RuntimeException("FlowchartPanel reference not set, cannot call functions");
        }

        // Get function definition
        FunctionDefinition funcDef = flowchartPanel.getFunction(functionName);
        if (funcDef == null) {
            throw new RuntimeException("Function '" + functionName + "' not found");
        }

        // Get formal parameters from FunctionDefinition
        List<FunctionDefinition.Parameter> formalParams = funcDef.getFormalParameters();
        if (formalParams == null) {
            formalParams = new ArrayList<>();
        }

        mxGraph funcGraph = funcDef.getFunctionGraph();
        Object funcStart = funcDef.getStartCell();

        // Validate argument count
        if (argValues.size() != formalParams.size()) {
            throw new RuntimeException("Function '" + functionName + "' expects " +
                formalParams.size() + " parameters but got " + argValues.size());
        }

        // Create function context
        FunctionContext context = new FunctionContext(functionName, currentCell, null);

        // Set parameter values in local variables
        for (int i = 0; i < formalParams.size(); i++) {
            String paramName = formalParams.get(i).getName();
            context.setLocalVariable(paramName, argValues.get(i));
        }

        // Push context onto call stack
        callStack.push(context);

        // Switch to function graph
        mxGraph previousGraph = graph;
        graph = funcGraph;

        // Execute function starting from its start cell
        Object funcStartNext = getNextCell(funcStart);
        currentCell = funcStartNext;

        output.append("▶ CALLING FUNCTION: ").append(functionName).append("(");
        for (int i = 0; i < argValues.size(); i++) {
            if (i > 0) output.append(", ");
            output.append(formalParams.get(i).getName()).append("=").append(argValues.get(i));
        }
        output.append(")\n");

        // Execute function body until END
        while (currentCell != null && currentCell != funcDef.getEndCell()) {
            // Execute current step
            executeStep();

            // Check if we've reached the end or encountered an error
            if (!isRunning || currentCell == null) {
                break;
            }
        }

        // Pop context and restore graph
        FunctionContext returnedContext = callStack.pop();
        graph = previousGraph;

        // Get return value from the specified return variable name
        Object returnValue = 0;  // Default for void functions
        String returnVarName = funcDef.getReturnVariableName();
        String returnType = funcDef.getReturnType();

        if (returnVarName != null && !returnVarName.isEmpty() && !"void".equals(returnType)) {
            // Read the return variable value
            returnValue = returnedContext.getLocalVariable(returnVarName);
            if (returnValue == null) {
                returnValue = 0;  // Default if not set
            }
        }

        if (returnType != null && !"void".equals(returnType)) {
            output.append("▶ FUNCTION ").append(functionName).append(" RETURNED: ").append(returnValue).append("\n");
        } else {
            output.append("▶ FUNCTION ").append(functionName).append(" COMPLETED\n");
        }

        return returnValue;
    }

    /**
     * Gets the next cell from the current cell.
     */
    private Object getNextCell(Object cell) {
        mxCell mxCell = (mxCell) cell;
        Object[] edges = graph.getOutgoingEdges(mxCell);
        if (edges != null && edges.length > 0) {
            return ((mxCell) edges[0]).getTarget();
        }
        return null;
    }

    /**
     * Executes a FUNCTION_CALL block.
     * The value can be:
     * - functionName(args) - for void functions
     * - result = functionName(args) - for functions with return value
     */
    private void executeFunctionCallBlock(String value) {
        String cleanValue = value.trim();

        // Check if it's an assignment with function call
        if (cleanValue.contains("=")) {
            // Format: result = functionName(args)
            String[] parts = cleanValue.split("=", 2);
            String varName = parts[0].trim();
            String funcCallExpr = parts[1].trim();

            // Evaluate function call
            Object result = evaluateExpression(funcCallExpr);

            // Store result
            setVariable(varName, result);
            output.append("▶ ").append(varName).append(" = ").append(result).append("\n");
        } else {
            // Format: functionName(args) - void function or result not used
            Object result = evaluateExpression(cleanValue);
            // Result is discarded for void functions
        }
    }

    /**
     * Gets a variable value, checking local scope first, then global.
     */
    private Object getVariable(String name) {
        // Check local scope first
        if (!callStack.isEmpty()) {
            FunctionContext context = callStack.peek();
            if (context.hasLocalVariable(name)) {
                return context.getLocalVariable(name);
            }
        }

        // Check global scope
        return variables.get(name);
    }

    /**
     * Checks if a variable exists in the current scope (local or global).
     */
    private boolean hasVariable(String name) {
        // Check local scope first
        if (!callStack.isEmpty()) {
            FunctionContext context = callStack.peek();
            if (context.hasLocalVariable(name)) {
                return true;
            }
        }

        // Check global scope
        return variables.containsKey(name);
    }

    /**
     * Sets a variable value in the current scope.
     */
    private void setVariable(String name, Object value) {
        // If we're in a function, set in local scope
        if (!callStack.isEmpty()) {
            FunctionContext context = callStack.peek();
            context.setLocalVariable(name, value);
        } else {
            // Set in global scope
            variables.put(name, value);
        }
    }

    /**
     * Gets all visible variables (local + global).
     */
    private Map<String, Object> getAllVariables() {
        Map<String, Object> allVars = new HashMap<>(variables); // Start with global

        // Override with local variables if in a function
        if (!callStack.isEmpty()) {
            FunctionContext context = callStack.peek();
            allVars.putAll(context.getLocalVariables());
        }

        return allVars;
    }
}
