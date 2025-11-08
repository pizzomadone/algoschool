import com.mxgraph.view.mxGraph;

/**
 * Rappresenta la definizione di una funzione nel flowchart.
 * Ogni funzione ha un proprio diagramma a blocchi con START/END e variabili locali.
 */
public class FunctionDefinition {
    private String name;
    private mxGraph functionGraph;
    private Object startCell;
    private Object endCell;

    public FunctionDefinition(String name) {
        this.name = name;
        this.functionGraph = new mxGraph();
        this.functionGraph.setAllowDanglingEdges(false);
        this.functionGraph.setCellsEditable(false);
        this.functionGraph.setConnectableEdges(false);
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
}
