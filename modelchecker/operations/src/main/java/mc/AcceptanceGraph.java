package mc;

import lombok.Getter;
import lombok.Setter;
import mc.exceptions.CompilationException;
import mc.processmodels.automata.Automaton;
import mc.processmodels.automata.AutomatonEdge;
import mc.processmodels.automata.AutomatonNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dstr on 24/01/18.
 * An acceptance graph should be a dfa plus a Map from nodes to a set of acceptance sets
 */
public class AcceptanceGraph {

  @Getter
  @Setter
  private Automaton a;
  @Getter
  @Setter
  private Map<AutomatonNode, List<Set<String>>> node2AcceptanceSets =
    new HashMap<AutomatonNode, List<Set<String>>>();


  /**
   * Construct an Acceptance Graph from a nfa
   *
   * @param id  the id of the resulting automaton
   * @param nfa an automata taken in by the function
   * @return acceptance graph - dfa + node to set of acceptance sets Map
   * @throws CompilationException when the function fails
   */

  public AcceptanceGraph(String id, Automaton nfa, boolean cong)
    throws CompilationException {
    //Automaton nfa = nfain.copy();  // must copy
    //System.out.println("Starting accept");
    Map<AutomatonNode, List<Set<String>>> dfaNode2ASet =
      new HashMap<AutomatonNode, List<Set<String>>>();
    //The acceptance sets should contain "STOP" "ROOT"
    // nfa nodes to its acceptance set
    Map<AutomatonNode, Set<String>> nfaNode2A = new HashMap<AutomatonNode, Set<String>>();
    Automaton dfa = new Automaton(id, !Automaton.CONSTRUCT_ROOT);
    nfaNode2A = build_nfanode2ASet(nfa,cong);
    //System.out.println("built nfaNode2A");

    //maps one dfa node to a set of nfa nodes (nfaNode2A maps nfa node to acceptance set)
    Map<AutomatonNode, Set<AutomatonNode>> dfa2nfaSet = new HashMap<>(); //dfa 2 nfa
    //Map<String, AutomatonNode> nodeMap = new HashMap<>();
    Set<AutomatonNode> visited = new HashSet<>(); //set of dfa nodes

    Stack<AutomatonNode> dfaNodes = new Stack<>();
//set up root node
    //Set<AutomatonNode> dfaRoot =constructClosure(nfa.getRoot(), dfa2nfaSet); //add to dfa2nfaSet
    //Set<AutomatonNode> dfaRoot =constructClosure(nfa.getRoot());
    for (AutomatonNode nfaRoot : nfa.getRoot()) { // more than one root from "+"
      AutomatonNode drt = dfa.addNode();
      drt.setStartNode(true);
      dfa.addRoot(drt);
      Set<AutomatonNode> nr = new HashSet<AutomatonNode>();
      nr.add(nfaRoot);  // one dfa root maps to a set of nfa roots
      dfa2nfaSet.put(drt, nr);
    }// note this dfa has more than one root node!
    for (AutomatonNode dfaRoot : dfa.getRoot()) {
      dfaNodes.push(dfaRoot);
      dfa2nfaSet.put(dfaRoot, nfa.getRoot());
//System.out.println("pushed");
    }


    Set<String> alphabet = nfa.getAlphabet();
    //alphabet.remove(Constant.HIDDEN);
    int cnt = 0;
    boolean processedRoot = false;
    while (!dfaNodes.isEmpty() && cnt < 30) {
      AutomatonNode poped = dfaNodes.pop();
      visited.add(poped);

      List<Set<String>> acceptance = new LinkedList<>();
      for (AutomatonNode n : dfa2nfaSet.get(poped)) {// get the nfa nodes making this dfa node
//System.out.println("nfa node "+n.myString());
        if (!acceptance.contains(nfaNode2A.get(n))) {
          acceptance.add(nfaNode2A.get(n));
        }

      }

      //System.out.println(poped.getId()+" -> "+ acceptance.toString());
      dfaNode2ASet.put(poped, acceptance);
      //System.out.println("Adding "+ poped.myString()+" "+ acceptance.toString());
      for (String action : alphabet) {
        Set<AutomatonNode> nextStates = constructStateSet(dfa2nfaSet.get(poped), action);
        AutomatonNode nextdfa = null;
        if (nextStates.isEmpty()) {
          continue;
        }
        for (AutomatonNode dfand : dfa2nfaSet.keySet()) {
          if (dfa2nfaSet.get(dfand).equals(nextStates)) {
            nextdfa = dfand;
            break;
          }
        }
// only build new dfa node if it has not already been built
        if (nextdfa == null) {

          nextdfa = dfa.addNode();
          // set dfa node to STOP in one nfa node is STOP
          boolean allSTOP = true;
          for (AutomatonNode nd : nextStates) {
            if (nd.isERROR()) {
              allSTOP = false;
              poped.setErrorNode(true);
              break;
            }
            if (!nd.isSTOP()) {
              allSTOP = false;
            }
          }
          if (allSTOP) poped.setStopNode(true);
          dfa2nfaSet.put(nextdfa, nextStates);
          //Set<String> nextSt =constructClosure(nfa.getRoot(), dfa2nfaSet); //add to dfa2nfaSet
          dfaNodes.push(nextdfa);
          //fringe.push(nextStates);  // states to be processed
        }

        cnt++;
        dfa.addEdge(action, poped, nextdfa, null, true, false);
      }
    }

    dfa.getNodes().stream()
      .filter(nodex -> nodex.getOutgoingEdges().isEmpty())
      .forEach(nodey -> nodey.setStopNode(true));
    //System.out.println("So far "+dfa.myString());
    //printnode2AcceptanceSets(dfaNode2ASet, nfaNode2A);
    Map<AutomatonNode, List<Set<String>>> dfaNode2ASetNew = activeActionCorrection(dfaNode2ASet);
    //System.out.println("Next");
    //printnode2AcceptanceSets(dfaNode2ASetNew, nfaNode2A);
// refactor the acceptance sets adding [a^] where needed

    this.setA(dfa);
    this.setNode2AcceptanceSets(dfaNode2ASetNew);

    //System.out.println("Ending AcceptanceGraph Constructor " + this.toString());
  }


  // [{a,b^},{c^}] ==> [{a,b^},{c^},{b^}]
  private Map<AutomatonNode, List<Set<String>>> activeActionCorrection(Map<AutomatonNode, List<Set<String>>> nd2Ac) {
    Map<AutomatonNode, List<Set<String>>> out = new HashMap<>();
    for (AutomatonNode nd : nd2Ac.keySet()) {
      List<Set<String>> asout = new ArrayList<>();
      for (Set<String> as : nd2Ac.get(nd)) {
        if (as.size() > 1) {
          for (String a : as) {
            if (a.endsWith(Constant.ACTIVE)) {
              Set<String> s = new HashSet<String>();
              s.add(a);
              asout.add(s);
            }
          }
        }
        asout.add(as);
      }
      out.put(nd, asout);
    }
    return out;
  }


  /**
   * @param nodes  set of nfa nodes of curret dfa node
   * @param action an action of events leaving set of nfa nodes
   * @return set of next nfa nodes
   */
  private Set<AutomatonNode> constructStateSet(Set<AutomatonNode> nodes, String action) {
    //Set<String> states = new HashSet<>();
    Set<AutomatonNode> nextNodes = new TreeSet<>();
    //Set<String> visited = new HashSet<>();
    //System.out.println("starting constructStateSet");

    Stack<AutomatonNode> fringe = new Stack<>();
    nodes.forEach(fringe::push);

    while (!fringe.isEmpty()) {
      AutomatonNode current = fringe.pop();
//System.out.println("1 "+ current.getId());
      List<AutomatonEdge> edges = current.getOutgoingEdges();
      for (AutomatonEdge edge : edges) {
        //System.out.println("2** "+edge.toString());
        //System.out.println("2   "+ edge.getLabel()+" "+edge.getTo().getId());
        if (action.equals(edge.getLabel())) {
          //states.add(edge.getTo().getId());
          //System.out.println("3    "+edge.getTo().getId());
          nextNodes.add(edge.getTo());
          //System.out.println("("+edge.getLabel()+" "+ edge.getTo().getId());
        }
      }
    }


    /*System.out.print("ending constructStateSet size ");
    nextNodes.stream().forEach(x-> System.out.print(x.getId()+" "));
    System.out.println("End constructState"); */
    return nextNodes;
  }

  /**
   * @param nodes      list of nfa nodes to make one dfa node
   * @param identifier
   * @return the dfa nodeid
   * Side effect build the acceptance set and store in node2AcceptanceSets
   */
  private String constructNodeId(Set<AutomatonNode> nodes, String identifier) {
    String id = identifier + constructLabel(nodes);

    return id;
  }

  /**
   * @param nodes set of nodes n1,n2
   * @return a string of the set of nodes "{n1,n1}"
   */
  private String constructLabel(Set<AutomatonNode> nodes) {
    Set<String> labelSet = new HashSet<>();
    for (AutomatonNode node : nodes) {
      labelSet.add(Integer.toString(node.getLabelNumber()));
    }

    List<String> labels = new ArrayList<>(labelSet);
    Collections.sort(labels);

    StringBuilder builder = new StringBuilder();
    builder.append("{");
    for (int i = 0; i < labels.size(); i++) {
      builder.append(labels.get(i));
      if (i < labels.size() - 1) {
        builder.append(",");
      }
    }
    builder.append("}");

    return builder.toString();
  }

  /**
   * @param a automaton
   *          This computes the map nfanode2ASet
   *          note needs to respect Start and STOP if congruance
   *          Builds the ready set or  Acceptance Set
   */
  private Map<AutomatonNode, Set<String>> build_nfanode2ASet(Automaton a,boolean cong) {
    //System.out.println("build_nfanode2ASet");
    Map<AutomatonNode, Set<String>> nfanode2ASet = new HashMap<AutomatonNode, Set<String>>();
    for (AutomatonNode n : a.getNodes()) {
      Set<String> as = n.getOutgoingEdges().stream().
        distinct().
        map(AutomatonEdge::getLabel).
        collect(Collectors.toSet());
      if (cong) {
        if (n.isSTOP()) {
          as.add(Constant.STOP);
        }
        if (n.isStartNode()) {
          as.add(Constant.Start);
        }
      }
      nfanode2ASet.put(n, as);
      //System.out.println("nfa ready "+n.getId()+" -> "+as);
    }
    //System.out.println("build_nfanode2ASet");
    return nfanode2ASet;
  }

  private void printnode2AcceptanceSets(
    Map<AutomatonNode, List<Set<String>>> node2AcceptanceSets,
    Map<AutomatonNode, Set<String>> nfanode2ASet) {
    System.out.println("nfa Sets");
    for (AutomatonNode n : nfanode2ASet.keySet()) {
      System.out.println(" "+n.getId()+" "+nfanode2ASet.get(n).toString() );
    }

 System.out.println("Acceptance Sets");
  for (AutomatonNode nd : node2AcceptanceSets.keySet()) {
   System.out.println(" "+nd.getId()+"  "+node2AcceptanceSets.get(nd));
  }

  }

  /**
   * Colors the nodes of an Acceptance graph
   *
   * @param cmap defines the colour in terms of a set of acceptance sets
   *             first time this should be an empty map
   *             colouring subsequent Acceptance graph should reuse the old cmap
   * @param equ  true compute equality false compute refinement
   *             returns the last color used (for coloring more than one ag
   */
  public Integer colorNodes(Map<Integer, List<Set<String>>> cmap,
                            Map<AutomatonNode, List<Set<String>>> n2as,
                            Integer color,
                            boolean equ) {
    //Map<AutomatonNode, List<Set<String>> > n2as = new TreeMap<>();
    //System.out.println(this.toString());
    //System.out.println("*****Color Start**** "+color);
    //System.out.println("n2as "+n2as.toString());
    boolean found = false;
    for (AutomatonNode nd : this.getA().getNodes()) {
      List<Set<String>> acept = n2as.get(nd);
      //System.out.println("node "+nd.myString()+ " accept "+acept.toString());
      found = false;
      //System.out.println(cmap.toString());
      for (int i : cmap.keySet()) {
        //    //System.out.println("reading cmap  "+i+" ");
        //    //System.out.println("reading cmap  "+cmap.get(i));

        if (equ == true && AcceptanceSetEquality(cmap.get(i), acept)) {
          //System.out.println("found "+ cmap.get(i).toString());
          nd.setColour(i);
          found = true;
          break;
        } else if (equ == false && AcceptanceGraph.AcceptanceSubSet(acept, cmap.get(i))) {
          //System.out.println("subset found "+ cmap.get(i).toString());
          nd.setColour(i);
          found = true;
          break;
        }
      }
      if (!found) {
        color = color + 1;
        nd.setColour(color);
        //System.out.println("New cmap "+acept.toString()+" "+ color);
        cmap.put(color, acept);
      }
      //System.out.println("node " +nd.getId()+" has color "+ nd.getColour());
    }
   //System.out.println("ColorNodes end col = "+color);
    return color;
  }

  private String node2ac_toString() {
    String outString = "";
    for (AutomatonNode nd : node2AcceptanceSets.keySet()) {
      outString = outString + ("\n " + nd.getId() + "  " + node2AcceptanceSets.get(nd));
    }
    return outString;
  }


  private void printnode2AcceptanceSets() {
  System.out.println("Acceptance Sets");
  for (AutomatonNode nd : node2AcceptanceSets.keySet()) {
   System.out.println(" "+nd.getId()+"  "+node2AcceptanceSets.get(nd));
  }
  }

  public static boolean AcceptanceSubSet(List<Set<String>> a1, List<Set<String>> a2) {
    //System.out.println("AcceptanceSubSet "+ a1+" Sub "+a2);
    boolean ok = false;
    for (Set<String> as1 : a1) {
      ok = false;
      for (Set<String> as2 : a2) {

        if (as1.containsAll(as2)) {
          ok = true;
          //System.out.println("as1 "+as1+" ->>- as2 "+as2);
          break;
        }
        //System.out.println("as1 "+as1+" ->NOT>- as2 "+as2);
      }  // if one true then inner loop true
      if (ok == false) { break; } //if one inner false then outer false
    }  //outer only true if all inner loops true
    //System.out.println("a1 "+a1+" ->>- a2 "+a2+"  returns "+ok);
    return ok;
  }

  public static boolean AcceptanceSetEquality(List<Set<String>> a1, List<Set<String>> a2) {
    boolean b = AcceptanceSubSet(a1, a2) && AcceptanceSubSet(a2, a1);
    //System.out.println("Equ ?  "+ a1.toString() + "  "+ a2.toString() +" = "+b);
    return b;
  }

  /*
    Constructor
   */
  AcceptanceGraph(Automaton ain, Map<AutomatonNode, List<Set<String>>> n2as) {
    a = ain;
    node2AcceptanceSets = n2as;
  }

  public String toString() {
    if (this.getA() == null) {
      System.out.println("AcceptanceGraph aut = null");
    }
    if (this.getNode2AcceptanceSets() == null) {
      System.out.println("AcceptanceGraph n2ac = null");
    }
    return "**Acceptance Graph  \n  " + this.getA().myString() + " " +
      this.node2ac_toString() + "\n**End Acceptance Graph ";

  }
}