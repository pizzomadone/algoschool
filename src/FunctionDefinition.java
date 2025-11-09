import com.mxgraph.view.mxGraph;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta la definizione di una funzione nel flowchart.
 * Ogni funzione ha un proprio diagramma a blocchi con START/END e variabili locali.
 */
public class FunctionDefinition {

    /**
     * Classe interna per rappresentare un parametro formale con nome e tipo
     */
    public static class Parameter {
        private String name;
        private String type;  // "int", "double", "string"

        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return type + " " + name;
        }
    }

    private String name;
    private mxGraph functionGraph;
    private Object startCell;
    private Object endCell;
    private List<Parameter> formalParameters;
    private String returnType;  // "void", "int", "double", "string"

    public FunctionDefinition(String name) {
        this.name = name;
        this.functionGraph = new mxGraph();
        this.functionGraph.setAllowDanglingEdges(false);
        this.functionGraph.setCellsEditable(false);
        this.functionGraph.setConnectableEdges(false);
        this.formalParameters = new ArrayList<>();
        this.returnType = "void";  // Default to void (procedure)
    }

    public FunctionDefinition(String name, List<Parameter> formalParameters, String returnType) {
        this(name);
        if (formalParameters != null) {
            this.formalParameters = new ArrayList<>(formalParameters);
        }
        this.returnType = returnType != null ? returnType : "void";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public mxGraph getFunctionGraph() {
        return functionGraph;
    }

    public void setFunctionGraph(mxGraph functionGraph) {
        this.functionGraph = functionGraph;
    }

    public Object getStartCell() {
        return startCell;
    }

    public void setStartCell(Object startCell) {
        this.startCell = startCell;
    }

    public Object getEndCell() {
        return endCell;
    }

    public void setEndCell(Object endCell) {
        this.endCell = endCell;
    }

    public List<Parameter> getFormalParameters() {
        return formalParameters;
    }

    public void setFormalParameters(List<Parameter> formalParameters) {
        this.formalParameters = formalParameters != null ? new ArrayList<>(formalParameters) : new ArrayList<>();
    }

    public void addParameter(String name, String type) {
        this.formalParameters.add(new Parameter(name, type));
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType != null ? returnType : "void";
    }
}
