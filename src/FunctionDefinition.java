import com.mxgraph.view.mxGraph;
import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta la definizione di una funzione nel flowchart.
 * Ogni funzione ha un proprio diagramma a blocchi con START/END e variabili locali.
 */
public class FunctionDefinition {
    private String name;
    private mxGraph functionGraph;
    private Object startCell;
    private Object endCell;
    private List<String> formalParameters;
    private String returnType;  // "void", "int", "double", "char*", etc.

    public FunctionDefinition(String name) {
        this.name = name;
        this.functionGraph = new mxGraph();
        this.functionGraph.setAllowDanglingEdges(false);
        this.functionGraph.setCellsEditable(false);
        this.functionGraph.setConnectableEdges(false);
        this.formalParameters = new ArrayList<>();
        this.returnType = "void";  // Default to void (procedure)
    }

    public FunctionDefinition(String name, List<String> formalParameters, String returnType) {
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

    public List<String> getFormalParameters() {
        return formalParameters;
    }

    public void setFormalParameters(List<String> formalParameters) {
        this.formalParameters = formalParameters != null ? new ArrayList<>(formalParameters) : new ArrayList<>();
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType != null ? returnType : "void";
    }
}
