import java.util.HashMap;
import java.util.Map;

/**
 * Rappresenta il contesto di esecuzione di una funzione chiamata.
 * Gestisce le variabili locali, i parametri e il punto di ritorno.
 */
public class FunctionContext {
    private String functionName;
    private Map<String, Object> localVariables;
    private Object returnPoint;
    private String returnVariableName;

    /**
     * Crea un nuovo contesto di funzione.
     *
     * @param functionName Nome della funzione
     * @param returnPoint Il blocco a cui tornare dopo la chiamata
     * @param returnVariableName La variabile dove salvare il risultato (null per procedure)
     */
    public FunctionContext(String functionName, Object returnPoint, String returnVariableName) {
        this.functionName = functionName;
        this.returnPoint = returnPoint;
        this.returnVariableName = returnVariableName;
        this.localVariables = new HashMap<>();
    }

    public String getFunctionName() {
        return functionName;
    }

    public Map<String, Object> getLocalVariables() {
        return localVariables;
    }

    public Object getReturnPoint() {
        return returnPoint;
    }

    public String getReturnVariableName() {
        return returnVariableName;
    }

    /**
     * Imposta il valore di una variabile locale.
     */
    public void setLocalVariable(String name, Object value) {
        localVariables.put(name, value);
    }

    /**
     * Ottiene il valore di una variabile locale.
     */
    public Object getLocalVariable(String name) {
        return localVariables.get(name);
    }

    /**
     * Verifica se una variabile locale esiste.
     */
    public boolean hasLocalVariable(String name) {
        return localVariables.containsKey(name);
    }
}
