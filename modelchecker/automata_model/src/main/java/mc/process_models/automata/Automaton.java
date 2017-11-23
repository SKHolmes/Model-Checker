package mc.process_models.automata;

import com.microsoft.z3.Context;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import mc.compiler.Guard;
import mc.exceptions.CompilationException;
import mc.process_models.ProcessModel;
import mc.process_models.ProcessModelObject;
import mc.util.Location;
import mc.util.expr.Expression;

import java.util.*;
import java.util.stream.Collectors;

public class Automaton extends ProcessModelObject implements ProcessModel {

    public static final boolean CONSTRUCT_ROOT = true;

    private AutomatonNode root;
    private Map<String, AutomatonNode> nodeMap;
    private Map<String, AutomatonEdge> edgeMap;
    private Map<String, List<AutomatonEdge>> alphabet;

    private int nodeId;
    private int edgeId;

    public Automaton(String id) {
        super(id, "automata");
        setupAutomaton();

        // setup the root for this automaton
        this.root = addNode();
        root.addMetaData("startNode", true);
    }

    public Automaton(String id, boolean constructRoot) {
        super(id, "automata");
        setupAutomaton();

        // only construct a root node if specified to do so
        if (constructRoot) {
            this.root = addNode();
            root.addMetaData("startNode", true);
        }
    }
    private void setupAutomaton() {
        this.nodeMap = new HashMap<>();
        this.edgeMap = new HashMap<>();
        this.alphabet = new HashMap<>();

        this.nodeId = 0;
        this.edgeId = 0;
    }

    public AutomatonNode getRoot() {
        return root;
    }
    public void position() {
        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        try {
            Map<String, mxCell> graphNodes = new HashMap<>();
            for (AutomatonNode node : getNodes()) {
                mxCell n = (mxCell) graph.insertVertex(parent, node.getId(), node.getId(), 0, 0, 0, 0);
                graphNodes.put(node.getId(), n);
                if (node.hasMetaData("startNode")) {
                    graph.getModel().setRoot(n);
                }
            }
            for (AutomatonEdge edge : getEdges()) {
                Object to = graphNodes.get(edge.getTo().getId());
                Object from = graphNodes.get(edge.getFrom().getId());
                graph.insertEdge(parent, edge.getId(), edge.getLabel(), from, to);
            }
            mxGraphLayout layout;
            try {
                layout = new mxHierarchicalLayout(graph) {

                    @Override
                    public List<Object> findRoots(Object parent, Set<Object> vertices)
                    {
                        List<Object> list = new ArrayList<>();
                        list.add(graphNodes.get(getRoot().getId()));
                        return list;
                    }
                };
                layout.execute(parent);
            } catch (Exception ex) {
                layout = new mxOrthogonalLayout(graph);
                layout.execute(parent);
            }
            for (Map.Entry<String, mxCell> id : graphNodes.entrySet()) {
                getNode(id.getKey()).addMetaData("pos",id.getValue().getGeometry().getPoint());
            }
        } catch (CompilationException e) {
            e.printStackTrace();
        }
    }
    public void setRoot(AutomatonNode root) throws CompilationException {
        // check the the new root is defined
        if (root == null) {
            throw new CompilationException(getClass(),"Unable to set the root node to null",(Location)getMetaData().get("location"));
        }

        // check that the new root is part of this automaton
        if (!nodeMap.containsKey(root.getId())) {
            throw new CompilationException(getClass(),"Unable to set the root node to "+root.getId()+", as the root is not a part of this automaton",(Location)getMetaData().get("location"));
        }

        this.root = root;
    }

    public String getRootId() {
        return root.getId();
    }

    public List<AutomatonNode> getNodes() {
        return nodeMap.entrySet().stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public AutomatonNode getNode(String id) throws CompilationException {
        if (nodeMap.containsKey(id)) {
            return nodeMap.get(id);
        }

        throw new CompilationException(getClass(),"Unable to get the node "+id+" as it does not exist.",(Location)getMetaData().get("location"));
    }

    public AutomatonNode addNode() {
        String id = getNextNodeId();
        while(nodeMap.containsKey(id)){
            id = getNextNodeId();
        }

        return addNode(id);
    }

    public AutomatonNode addNode(String id) {
        AutomatonNode node = new AutomatonNode(id);
        nodeMap.put(id, node);
        return node;
    }

    public boolean removeNode(AutomatonNode node) {
        // check that the specified node is part of this automaton
        if (!nodeMap.containsKey(node.getId())) {
            return false;
        }
        // remove the edges that reference the specified node
        List<AutomatonEdge> edges = node.getIncomingEdges();
        edges.addAll(node.getOutgoingEdges());
        edges.stream()
            .map(ProcessModelObject::getId)
            .forEach(id -> edgeMap.remove(id));
        nodeMap.remove(node.getId());
        return true;
    }

    public AutomatonNode combineNodes(AutomatonNode node1, AutomatonNode node2, Context context) throws CompilationException, InterruptedException {
        if(!nodeMap.containsKey(node1.getId())){
            throw new CompilationException(getClass(), node1.getId() + " was not found in the automaton " + getId(), (Location)getMetaData("location"));
        }
        if(!nodeMap.containsKey(node2.getId())){
            throw new CompilationException(getClass(),node2.getId() + " was not found in the automaton "+ getId(), (Location)getMetaData("location"));
        }
        AutomatonNode node = addNode();

        for (AutomatonEdge edge1 : node1.getIncomingEdges()) {
            for (AutomatonEdge edge2: node2.getIncomingEdges()) {
                processGuards(edge1,edge2,context);
            }
        }

        for (AutomatonEdge edge1 : node1.getOutgoingEdges()) {
            for (AutomatonEdge edge2: node2.getOutgoingEdges()) {
                processGuards(edge1,edge2,context);
            }
        }
        // add the incoming and outgoing edges from both nodes to the combined nodes
        processIncomingEdges(node, node1);
        processIncomingEdges(node, node2);
        processOutgoingEdges(node, node1);
        processOutgoingEdges(node, node2);
        // create a union of the metadata from both nodes
        for(String key : node1.getMetaDataKeys()){
            node.addMetaData(key, node1.getMetaData(key));
        }
        for(String key : node2.getMetaDataKeys()){
            node.addMetaData(key, node2.getMetaData(key));
        }
        node.removeMetaData("variables");
        if (node1.hasMetaData("variables") && node2.hasMetaData("variables")) {
            Map<?, ?> vars1 = (Map) node1.getMetaData("variables");
            Map<?, ?> vars2 = (Map) node2.getMetaData("variables");
            if (Objects.equals(vars1, vars2)) {
                node.addMetaData("variables", vars1);
            }
        }

        if(node1.hasMetaData("startNode") || node2.hasMetaData("startNode")){
            setRoot(node);
            node.addMetaData("startNode",true);
        }
        removeNode(node1);
        removeNode(node2);
        return node;
    }
    private void processGuards(AutomatonEdge edge1, AutomatonEdge edge2, Context context) throws CompilationException, InterruptedException {
        if (edge1.getLabel().equals(edge2.getLabel()) && edge1.hasMetaData("guard") && edge2.hasMetaData("guard")) {
            Guard guard1 = (Guard) edge1.getMetaData("guard");
            Guard guard2 = (Guard) edge2.getMetaData("guard");
            if (guard1 == null || guard2 == null || guard1.getGuard() == null || guard2.getGuard() == null) return;
            //Since assignment should be the same (same colour) we can just copy most data from either guard.
            Guard combined = guard1.copy();
            //By putting both equations equal to eachother, if we have multiple or operations, then if one matches then it will be solveable.
            if (!guard1.getVariables().isEmpty() && !Expression.equate(guard1,guard2, context))
                //We could take either path
                combined.setGuard(context.mkOr(guard1.getGuard(), guard2.getGuard()));
            else
                combined.setGuard(guard1.getGuard());
            edge1.addMetaData("guard",combined);
            edge2.addMetaData("guard",combined);
        }
    }
    private void processIncomingEdges(AutomatonNode node, AutomatonNode oldNode) {
        List<AutomatonEdge> edges = oldNode.getIncomingEdges();
        for (AutomatonEdge edge : edges) {
            node.addIncomingEdge(edge);
            edge.setTo(node);
            oldNode.removeIncomingEdge(edge);
        }
    }

    private void processOutgoingEdges(AutomatonNode node, AutomatonNode oldNode) {
        List<AutomatonEdge> edges = oldNode.getOutgoingEdges();
        for (AutomatonEdge edge : edges) {
            node.addOutgoingEdge(edge);
            edge.setFrom(node);
            oldNode.removeOutgoingEdge(edge);
        }
    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    public List<AutomatonEdge> getEdges() {
        return edgeMap.entrySet().stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public AutomatonEdge getEdge(String id) throws CompilationException {
        if (edgeMap.containsKey(id)) {
            return edgeMap.get(id);
        }

        throw new CompilationException(getClass(),"Edge "+id+" was not found in the automaton "+getId(),(Location)getMetaData().get("location"));
    }

    public AutomatonEdge addEdge(String label, AutomatonNode from, AutomatonNode to, Map<String,Object> metaData) throws CompilationException {
        String id = getNextEdgeId();
        return addEdge(id, label, from, to, metaData);
    }

    public AutomatonEdge addEdge(String id, String label, AutomatonNode from, AutomatonNode to, Map<String,Object> metaData) throws CompilationException {
        // check that the nodes have been defined
        if (from == null) {
            throw new CompilationException(getClass(),"Unable to add the specified edge as the source was null.",(Location)getMetaData().get("location"));
        }

        if (to == null) {
            throw new CompilationException(getClass(),"Unable to add the specified edge as the destination was null.",(Location)getMetaData().get("location"));
        }
        // check that the nodes are part of this automaton
        if (!nodeMap.containsKey(from.getId())) {
            throw new CompilationException(getClass(),"Unable to add the specified edge as "+from.getId()+" is not a part of this automaton. \nPlease make sure you aren't linking directly to a parallel composed process!",(Location)getMetaData().get("location"));
        }

        if (!nodeMap.containsKey(to.getId())) {
            throw new CompilationException(getClass(),"Unable to add the specified edge as "+to.getId()+" is not a part of this automaton.  \nPlease make sure you aren't linking directly to a parallel composed process!",(Location)getMetaData().get("location"));
        }

        // check if there is already an identical edge between the specified nodes
        List<AutomatonEdge> edges = from.getOutgoingEdges().stream()
            .filter(edge -> edge.getLabel().equals(label) && edge.getTo().getId().equals(to.getId()))
            .collect(Collectors.toList());

        if(edges.size() > 0){
            for (AutomatonEdge edge : edges) {
                if (edge.getMetaData("guard") == null && !metaData.containsKey("guard")) {
                    return edge;
                }
                Guard guard = (Guard) edge.getMetaData("guard");
                Guard guard2 = (Guard) metaData.get("guard");
                if(guard != null && guard2 != null) {
                    if (Objects.equals(guard,guard2)) return edge;
                }
            }
        }

        AutomatonEdge edge = new AutomatonEdge(id, label, from, to);
        edge.getMetaData().putAll(metaData);

        // add edge reference to the incoming and outgoing nodes
        from.addOutgoingEdge(edge);
        to.addIncomingEdge(edge);

        // add edge to the edge and alphabet maps
        if (!alphabet.containsKey(label)) {
            alphabet.put(label, new ArrayList<>());
        }
        alphabet.get(label).add(edge);
        edgeMap.put(id, edge);

        return edge;
    }

    public boolean removeEdge(AutomatonEdge edge) {
        // check that the specified edge is part of this automaton
        if (!edgeMap.containsKey(edge.getId())) {
            return false;
        }

        // remove references to this edge
        edge.getFrom().removeOutgoingEdge(edge);
        edge.getTo().removeIncomingEdge(edge);

        edgeMap.remove(edge.getId());
        return true;
    }

    public boolean removeEdge(String id) {
        if (!edgeMap.containsKey(id)) {
            return false;
        }

        return removeEdge(edgeMap.get(id));
    }

    public void relabelEdges(String oldLabel, String newLabel) {
        if (alphabet.containsKey(oldLabel)) {
            List<AutomatonEdge> edges = alphabet.get(oldLabel);
            edges.forEach(edge -> edge.setLabel(newLabel));
            if (alphabet.containsKey(newLabel)) {
                edges.addAll(alphabet.get(newLabel));
            }
            alphabet.put(newLabel, edges);
            alphabet.remove(oldLabel);
        }
    }

    public int getEdgeCount() {
        return edgeMap.size();
    }

    public Set<String> getAlphabet() {
        return alphabet.keySet();
    }

    public AutomatonNode addAutomaton(Automaton automaton) throws CompilationException {
        AutomatonNode root = null;
        for(AutomatonNode node : automaton.getNodes()){
            AutomatonNode newNode = addNode(node.getId());
            for(String key : node.getMetaDataKeys()){
                if(key.equals("startNode")){
                    root = newNode;
                    if(this.root != null){
                        continue;
                    }

                    this.root = newNode;
                }

                newNode.addMetaData(key, node.getMetaData(key));
            }
        }

        for (AutomatonEdge edge : automaton.getEdges()) {
            AutomatonNode from = getNode(edge.getFrom().getId());
            AutomatonNode to = getNode(edge.getTo().getId());
            AutomatonEdge newEdge = addEdge(edge.getId(), edge.getLabel(), from, to, edge.getMetaData());
            for (String key : edge.getMetaDataKeys()) {
                newEdge.addMetaData(key, edge.getMetaData(key));
            }
        }
        if (root == null) {
            throw new CompilationException(getClass(),"There was no root found while trying to add an automaton");
        }
        return root;
    }


    public String getNextNodeId() {
        return getId() + ".n" + nodeId++;
    }

    public String getNextEdgeId() {
        return getId() + ".e" + edgeId++;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("automaton:{\n");
        builder.append("\tnodes:{\n");
        for (AutomatonNode node : nodeMap.values()) {
            builder.append("\t\t").append(node.getId());
            if (node == root) {
                builder.append("(root)");
            }
            builder.append("\n");
        }
        builder.append("\t}\n\tedges:{\n");
        for (AutomatonEdge edge : edgeMap.values()) {
            builder.append("\t\t").append(edge.getFrom().getId()).append(" -").append(edge.getLabel()).append("> ").append(edge.getTo().getId()).append("\n");
        }
        builder.append("\t}\n}");

        return builder.toString();
    }

    public Automaton copy() throws CompilationException {
        Automaton copy = new Automaton(getId(), !CONSTRUCT_ROOT);
        copy.nodeId = nodeId;
        copy.edgeId = edgeId;
        List<AutomatonNode> nodes = getNodes();
        for(AutomatonNode node : nodes){
            AutomatonNode newNode = copy.addNode(node.getId());
            for(String key : node.getMetaDataKeys()){
                newNode.addMetaData(key, node.getMetaData(key));
                if(key.equals("startNode")){
                    copy.setRoot(newNode);
                }
            }
        }

        List<AutomatonEdge> edges = getEdges();
        for(AutomatonEdge edge : edges){
            AutomatonNode from = copy.getNode(edge.getFrom().getId());
            AutomatonNode to = copy.getNode(edge.getTo().getId());
            copy.addEdge(edge.getId(), edge.getLabel(), from, to, edge.getMetaData());
        }

        for(String key : getMetaDataKeys()){
            copy.addMetaData(key, getMetaData(key));
        }

        return copy;
    }
}
