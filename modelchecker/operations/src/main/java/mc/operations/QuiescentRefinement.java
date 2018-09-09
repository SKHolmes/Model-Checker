package mc.operations;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.microsoft.z3.Context;
import mc.Constant;
import mc.exceptions.CompilationException;
import mc.operations.functions.AbstractionFunction;
import mc.plugins.IOperationInfixFunction;
import mc.processmodels.ProcessModel;
import mc.processmodels.automata.Automaton;
import mc.processmodels.automata.AutomatonNode;

public class QuiescentRefinement implements IOperationInfixFunction {
  /**
   * A method of tracking the function.
   *
   * @return The Human-Readable form of the function name
   */
  @Override
  public String getFunctionName() {
    return "QuiescentRefinement";
  }

  /**
   * The form which the function will appear when composed in the text.
   *
   * @return the textual notation of the infix function
   */
  @Override
  public String getNotation() {
    return "<q";
  }
  @Override
  public String getOperationType(){return "automata";}
  @Override
  public Collection<String> getValidFlags(){
  return ImmutableSet.of(Constant.UNFAIR, Constant.FAIR, Constant.CONGURENT);
  }
  /**
   * Evaluate the function.
   *
   * @param alpha
   * @param processModels automaton in the function (e.g. {@code A} in {@code A ~ B})
   * @return the resulting automaton of the operation
   */
  @Override
  public boolean evaluate(Set<String> alpha, Set<String> flags, Context context, Collection<ProcessModel> processModels) throws CompilationException {
    System.out.println("QUIESCENT "+alpha);

    //ProcessModel[] pms =  processModels.toArray();
    Automaton a1 = ((Automaton) processModels.toArray()[0]).copy();
    Automaton a2 = ((Automaton) processModels.toArray()[1]).copy();
    TraceRefinement teo = new TraceRefinement();
    AbstractionFunction abs = new AbstractionFunction();
    TraceWork tw = new TraceWork();
    //tw.evaluate(flags,processModels, TraceType.QuiescentTrace);
    addQuiescentAndListeningLoops(alpha,a1);
    addQuiescentAndListeningLoops(alpha,a2);
    System.out.println("Q a1 "+a1.myString());
    System.out.println("Q a2 "+a2.myString());
    a1 = abs.GaloisBCabs(a1.getId(),flags,context,a1);
    a2 = abs.GaloisBCabs(a2.getId(),flags,context,a2);
    ArrayList<ProcessModel> pms = new ArrayList<>();;
    pms.add(a1);
    pms.add(a2);
    return  teo.evaluate(alpha,flags,context,pms);
  }

  private void addQuiescentAndListeningLoops(Set<String> alphbet, Automaton a) throws CompilationException {
    System.out.println("addQuiescentAndListeningLoops");
    for(AutomatonNode nd : a.getNodes()){
      Set<String> notListening = nd.readySet().stream().filter(x->!x.endsWith("?")).collect(Collectors.toSet());
      nd.setQuiescent(notListening.size()==0);
      for(String lab: alphbet) {
        if (!nd.readySet().contains(lab)) {
          a.addEdge(lab,nd,nd,nd.getGuard(),false,false);
        }
      }
    }
  }

}

