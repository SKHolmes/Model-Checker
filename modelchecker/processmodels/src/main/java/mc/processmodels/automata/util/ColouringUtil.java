package mc.processmodels.automata.util;

import com.google.common.collect.Multimap;

import java.util.*;
import java.util.stream.Collectors;

import lombok.ToString;
import mc.Constant;
import mc.processmodels.automata.AutomatonEdge;
import mc.processmodels.automata.AutomatonNode;

public class ColouringUtil {


  private static final int BASE_COLOUR = -1;
  private static final int STOP_COLOUR = 0;
  private static final int ERROR_COLOUR = -5;
  private static final int ROOT_COLOUR = -2;
  private static final int ROOT_STOP_COLOUR = -3;
  private static final int ROOT_ERROR_COLOUR = -4;
  private int nextColourId = 1;
  //private Map<AutomatonNode,Integer> oldColours = new TreeMap<AutomatonNode,Integer>();

  /**
   * computs a bisimulation coloring. Starting from an initial coloring
   * Bisimulation starts with all nodes the same color
   * Failure equivalence uses acceptance set equality
   * Failure refinement uses acceptance subset
   *  @param nodes
   *
   */
  public Map<Integer, ColourPi> doColouring
  (List<AutomatonNode> nodes) throws Error {
    //System.out.println("doColouring START");
    Map<String, Integer> nextCol = new TreeMap<String, Integer>();
    Map<Integer, ColourPi> col2pi = new TreeMap<>();
   // Map<ColourPi, Integer> cpi = new TreeMap<ColourPi, Integer>();
    // Repeatedly
    int test = 0;
    boolean go = true;
    while (go) {
      if (test > 10) {
        System.out.println("ERROR COLOURING automaton");
        Throwable t = new Throwable();
        t.printStackTrace();
        throw (new Error("Automata Too Big"));
      }
      test++;
      // build fresh pi each iteration
      //    for each node nd
      for (AutomatonNode nd : nodes) {
        List<ColourComponent> ndp = new ArrayList<ColourComponent>(buildpi(nd).pi);
        ColourPi nodeColPi =  new ColourPi(ndp);

        if (nodeColPi.isinPi(col2pi)) {
          //        if nd_col = PI(nd_colPI)
          nextCol.put(nd.getId(), new Integer (nodeColPi.getCol(col2pi)));
          System.out.println("found "+nodeColPi.myString()+"->"+nextCol.get(nd.getId()));
          //System.out.println("Q2 cpi " + this.cpiMapToString(cpi));
          continue;

        } else {
            //         newCol in nd and in PI
          System.out.println("Not found " + nodeColPi.myString());
            nextCol.put(nd.getId(), getNextColourId());
      //System.out.println("NOT found "+ nodeColPi.myString()+" so Adding to next "+nd.getId()+" -> "+nextCol.get(nd.getId()));
            col2pi.put(nextCol.get(nd.getId()), nodeColPi);
           Integer n = new Integer(nextCol.get(nd.getId()));
          //System.out.println("add "+ nodeColPi.myString()+" -> "+ n);
          //System.out.println("Q3c cpi " + this.col2piToString(col2pi));
          col2pi.put(n,nodeColPi);
          //System.out.println("Q3d cpi " + this.col2piToString(col2pi));
          System.out.println(" added cpi "+n+" -> "+ nodeColPi.myString());
          continue;
          }
        }

      //   apply the new colours to the nodes
      //System.out.println("REcolor Nodes");
      for (AutomatonNode nd : nodes) {
        nd.setColour(nextCol.get(nd.getId()));
        System.out.println(nd.getId()+" -> "+nd.getColour());
      }

      System.out.println("before termination check\n" + this.col2piToString(col2pi));
      //if two nodes have  same old colour  but differnt new color pi then keep going
      go = false;
      Map<Integer, ColourPi> reversepi = new TreeMap<Integer, ColourPi>();

        for (AutomatonNode nd : nodes) {
          if (reversepi.containsKey(nd.getColour())) {
          if (!reversepi.get(nd.getColour()).equals(buildpi(nd))) {
            go = true;
            System.out.println("Keep Going "+reversepi.get(nd.getColour()).myString()+" != "+CCSString(buildpi(nd).pi));
            break;
          }
        } else {
          //System.out.println(nd.getId()+" -> "+ nd.getColour());
          reversepi.put(nd.getColour(), buildpi(nd));
          System.out.println("Termination Check Add "+nd.getColour()+"->"+ reversepi.get(nd.getColour()).myString());
        }
      }
      System.out.println("**** "+go);

    }

    System.out.println("Colouring\n" + this.col2piToString(col2pi));
    return col2pi;
  }

  private ColourPi buildpi(AutomatonNode nd) {
    ArrayList<ColourComponent> ccs = new ArrayList<ColourComponent>();
    if (nd.isStartNode()) {
      ccs.add(new ColourComponent(Constant.Start, ROOT_COLOUR));
    }
      if (nd.isSTOP()) {
        ccs.add(new ColourComponent(Constant.STOP, STOP_COLOUR));
      } else if (nd.isERROR()) {
        ccs.add(new ColourComponent(Constant.ERROR, ERROR_COLOUR));
      }


    for (AutomatonEdge ed : nd.getOutgoingEdges()) {
      if (ed.getFrom().equals(nd)) {
        ColourComponent cc = new ColourComponent(ed.getLabel(), ed.getTo().getColour());
        boolean add = true;
        for (ColourComponent c : ccs) { //Make a Set not a list
          if (c.action.equals(cc.action) && c.to == cc.to) {
            add = false;
            break;
          }
        }
        if (add) ccs.add(cc);
      }
    }

    Collections.sort(ccs);
    //System.out.println("Sorted ndpi "+ CCSString(ccs));
    return new ColourPi(ccs);
  }


  public Map<AutomatonNode, Integer> performInitialColouring(List<AutomatonNode> nodes) {
    //System.out.println("performInitialColouring");

    Map<AutomatonNode, Integer> initialColour = new HashMap<AutomatonNode, Integer>();
    for (AutomatonNode node : nodes) {
      // check if the current node is a terminal and or Start
      node.setColour(BASE_COLOUR);
      if (node.isTerminal()) {
        String terminal = node.getTerminal();
        if (terminal.equals("STOP")) {
          if (node.isStartNode()) {
            node.setColour(ROOT_STOP_COLOUR);
          } else {
            node.setColour(STOP_COLOUR);
          }
        }  //ERROR documentory only
      } else if (node.isStartNode()) {
        node.setColour(ROOT_COLOUR);
      }
    }
    for (AutomatonNode node : nodes) {
      initialColour.put(node, node.getColour());
//System.out.println("initialCol "+node.getId()+"->"+node.getColour());
    }
    return initialColour;
  }


  /*
     uses the color held on the automaton nodes
     Because the initial colouring need not be the total relation
     We need to check that the two nodes are initial colour equal
     ONLY USED IN Failure equ
   */
  public List<ColourComponent> constructColouring(AutomatonNode node) {
    Set<ColourComponent> colouringSet = new HashSet<>();
    //colouringSet.add(new ColourComponent(node.getColour(), "****"));
    node.getOutgoingEdges()
      .forEach(edge -> {
        boolean add = true;
        ColourComponent newColC = new ColourComponent(edge.getLabel(), edge.getTo().getColour());
        for (ColourComponent cc : colouringSet) {
          if (cc.equals(newColC)) {
            add = false;
          }
        }
        if (add) {
          colouringSet.add(newColC);
          //System.out.println("Adding From "+ node.getId()+ " To "+ edge.getTo().getId()+
          //                     " col "+edge.getTo().getColour() );
        }
      });
    if (node.isStartNode()) {
      colouringSet.add(new ColourComponent(Constant.Start, ROOT_COLOUR));
    }
    if (node.isSTOP()) {
      colouringSet.add(new ColourComponent(Constant.STOP, STOP_COLOUR));
    } else if (node.isERROR()) {
      colouringSet.add(new ColourComponent(Constant.ERROR, ERROR_COLOUR));
    }

    List<ColourComponent> colouring = new ArrayList<>(colouringSet);
    //System.out.println("CC "+node.myString());
    //System.out.println("CC "+node.getId()+" "+ colouring.stream(). map(x->x.myString()).reduce("",(x,y)->x+" "+y));
    return colouring;
  }

  private int getNextColourId() {
    return nextColourId++;
  }

  public boolean colorComponentEquality(List<ColourComponent> c1, List<ColourComponent> c2) {

    if (c1.size() != c2.size()) {
      return false;
    }
    for (int ix = 0; ix < c1.size(); ix++) {
      if (c1.get(ix).to != c2.get(ix).to) {
        return false;
      }
      if (!c1.get(ix).action.equals(c2.get(ix).action)) {
        return false;
      }
    }
    return true;
  }

  private Map<AutomatonNode, Integer> setOldColours(Multimap<Integer, AutomatonNode> nodeColours) {
    Map<AutomatonNode, Integer> oCols = new TreeMap<AutomatonNode, Integer>();
    for (Integer k : nodeColours.asMap().keySet()) {
      for (AutomatonNode n : nodeColours.get(k)) {
        oCols.put(n, k);
      }
    }
    return oCols;
  }

  private boolean colEquality(Map<AutomatonNode, Integer> old, Map<AutomatonNode, Integer> now) {

    boolean b = true;
    for (AutomatonNode n : now.keySet()) {
      if (!now.containsKey(n) || old.get(n) != now.get(n)) {
        return false;
      }
    }
    return b;
  }


  //ColourComponent for an event is the triple <FromColour, label, ToColour>
  @ToString
  //@AllArgsConstructor
  public static class ColourComponent implements Comparable<ColourComponent> {
     public int to;
    public String action;

    public ColourComponent(String actionin, int toin) {
      to = toin;
      action = actionin;
    }


    public static int compareTo(ColourComponent c1, ColourComponent c2) {
      return c1.compareTo(c2);
    }

    public int compareTo(ColourComponent col) {
      //System.out.print("PINGO");

        if (to < col.to) return -1;
        if (to > col.to) return +1;
        if (to == col.to) {
          return (action.compareTo(col.action));
        } else {
          return 0;
        } // unreachable
    }


    public String myString() {
      return action + " " + to;
    }

@Override
    public boolean equals(Object cin) {
      if (!(cin instanceof ColourComponent)) return false;
       ColourComponent col = (ColourComponent)cin;
      boolean ok = action.equals(col.action) && to == col.to;
      //System.out.println("colcomp eq "+ ok);
      return ok;
    }
  }

  public static class ColourPi implements Comparable<ColourPi> {

    public boolean isinPi(Map<Integer,ColourPi> colpiMap) {
      //ColourPi  are sorted sets
      for(ColourPi cpi:colpiMap.values()){
        if (this.pi.size() != cpi.pi.size()) continue;
        boolean found = true;

        for(int i = 0; i <this.pi.size();i++) {
          if (! (this.pi.get(i).to == cpi.pi.get(i).to)   ||
              ! (this.pi.get(i).action.equals(cpi.pi.get(i).action))) {
            found = false;
            break;
          }
        }
        if (found) return true;
      }
      return false;
    }

    public Integer getCol (Map<Integer,ColourPi> colpiMap) {
      //ColourPi  are sorted sets
      for(Integer j:colpiMap.keySet()){
        if (this.pi.size() != colpiMap.get(j).pi.size()) continue;
        boolean found = true;
        ColourPi cpi = colpiMap.get(j);
        for(int i = 0; i <this.pi.size();i++) {
          if (! (this.pi.get(i).to == cpi.pi.get(i).to)   ||
            ! (this.pi.get(i).action.equals(cpi.pi.get(i).action))) {
            found = false;
            break;
          }
        }
        if (found) return j;
      }
      return -1;  //DATA ERROR
    }
    public List<ColourComponent> pi = new ArrayList<>();

    public static int compareTo(ColourPi c1, ColourPi c2) {
      return c1.compareTo(c2);
    }

    public int compareTo(ColourPi col) {
      //System.out.println(this.myString()+" ? "+col.myString());
      if (pi.size()!= col.pi.size()) {
        //System.out.println(pi.size()+" !? "+ col.pi.size());
        return 1;
      }
      int i = 0;
      for(ColourComponent cc : pi) {
        if (i>= col.pi.size()) return 1;
        if (cc.compareTo(col.pi.get(i))==1) {
          return 1;
        } else if (cc.compareTo(col.pi.get(i))==-1) {
          return -1;
        } else {
          i++;
          continue;
        }
      }
      //System.out.println("compareTo 0");
      return 0;
    }
    @Override
    public boolean equals(Object oin){
      if ( ! (oin instanceof ColourPi)) return false;
      ColourPi piin = (ColourPi) oin;
      int i = 0;
      //System.out.println(this.myString()+  "=?=" + piin.myString());
      if (pi.size()!= piin.pi.size()) {
        //System.out.println(pi.size()+" != "+ piin.pi.size());
        return false;
      }

      for(ColourComponent cc: pi){
        //System.out.printf("i "+i);
        if (! piin.pi.get(i).action.equals(cc.action)) {
          //System.out.println(piin.pi.get(i).action + "!= "+ cc.action);
          return false;}
        if ( (piin.pi.get(i).to != cc.to)) {
          //System.out.println(piin.pi.get(i).to +" != "+ cc.to);
          return false;}
        i++;
      }
      return true;
    }

    public boolean subset(ColourPi piin){
      int i = 0;
      //System.out.println(this.myString()+  "<=?" + piin.myString());
      if (pi.containsAll(piin.pi)) return true;
      else return false;
    }

    public ColourPi(List<ColourComponent> p){
      List<ColourComponent> pp = p.stream().distinct().collect(Collectors.toList());
      Collections.sort(pp);
      pi =pp;
    }

    public String myString(){
      StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (ColourComponent cc : this.pi) {
          sb.append(cc.myString()+", ");
        }
       sb.append(")");
      return sb.toString();
    }
  }

    public String CCSString(List<ColourComponent> ccs) {
    String s = "{ ";
    for (ColourComponent cc : ccs) {
      s = s +  " " + cc.action + " " + cc.to + " ";
    }
    return s + " }";
  }

  public String cpiMapToString(Map< ColourPi, Integer> colpi) {

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (ColourPi cp : colpi.keySet()) {
      if (! first)   sb.append("\n"); else first = false;
      sb.append(cp.myString() + "->");
      sb.append(colpi.get(cp));

    }
    return sb.toString();
  }

  public String col2piToString(Map<Integer, ColourPi> colpi) {

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Integer i : colpi.keySet()) {
      if (! first)   sb.append("\n"); else first = false;
      sb.append(i + "->");
      sb.append(colpi.get(i).myString());
    }
    return sb.toString();
  }
}
