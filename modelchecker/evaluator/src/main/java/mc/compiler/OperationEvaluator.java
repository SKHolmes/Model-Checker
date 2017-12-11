package mc.compiler;

import com.microsoft.z3.Context;
import mc.compiler.ast.*;
import mc.exceptions.CompilationException;
import mc.plugins.IOperationInfixFunction;
import mc.process_models.ProcessModel;
import mc.process_models.automata.Automaton;
import mc.process_models.automata.operations.AutomataOperations;
import mc.util.Location;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by sheriddavi on 27/01/17.
 */
public class OperationEvaluator {

    private int operationId;

    private AutomataOperations automataOperations;
    private static Map<String, Class<? extends IOperationInfixFunction>> operationsMap = new HashMap<>();

    public OperationEvaluator(){
        this.automataOperations = new AutomataOperations();
    }

    public List<OperationResult> evaluateOperations(List<OperationNode> operations,
                                                    Map<String, ProcessModel> processMap, Interpreter interpreter,
                                                    String code, Context context)
            throws CompilationException, InterruptedException {
        reset();
        List<OperationResult> results = new ArrayList<>();
        for(OperationNode operation : operations){

            String firstId  = findIdent(operation.getFirstProcess(),  code);
            String secondId = findIdent(operation.getSecondProcess(), code);

            List<String> firstIds = collectIdentifiers(operation.getFirstProcess());
            List<String> secondIds = collectIdentifiers(operation.getSecondProcess());

            List<Automaton> automata = new ArrayList<>();
            List<String> missing = new ArrayList<>(firstIds);

            missing.addAll(secondIds);
            missing.removeAll(processMap.keySet());

            if (!missing.isEmpty()) {
                throw new CompilationException(OperationEvaluator.class, "Identifier " + missing.get(0) + " not found!", operation.getLocation());
            }

            automata.add((Automaton) interpreter.interpret("automata", operation.getFirstProcess(), getNextOperationId(), processMap,context));
            automata.add((Automaton) interpreter.interpret("automata", operation.getSecondProcess(), getNextOperationId(), processMap,context));


            boolean result = instantiate(operationsMap.get(operation.getOperation().toLowerCase())).evaluate(automata);

            if (operation.isNegated()) {
                result = !result;
            }
            results.add(new OperationResult(operation.getFirstProcess(),operation.getSecondProcess(), firstId, secondId, operation.getOperation(), operation.isNegated(), result,""));
        }
        return results;
    }

    static List<String> collectIdentifiers(ASTNode process) {
        List<String> ids = new ArrayList<>();
        collectIdentifiers(process,ids);
        return ids;
    }
    private static void collectIdentifiers(ASTNode process, List<String> ids) {
        if (process instanceof IdentifierNode) ids.add(((IdentifierNode) process).getIdentifier());
        if (process instanceof ChoiceNode) {
            collectIdentifiers(((ChoiceNode) process).getFirstProcess(), ids);
            collectIdentifiers(((ChoiceNode) process).getSecondProcess(), ids);
        }
        if (process instanceof CompositeNode) {
            collectIdentifiers(((CompositeNode) process).getFirstProcess(), ids);
            collectIdentifiers(((CompositeNode) process).getSecondProcess(), ids);
        }
        if (process instanceof FunctionNode) collectIdentifiers(((FunctionNode) process).getProcess(), ids);
        if (process instanceof IfStatementNode){
            collectIdentifiers(((IfStatementNode) process).getTrueBranch(), ids);
            if (((IfStatementNode) process).hasFalseBranch())
                collectIdentifiers(((IfStatementNode) process).getFalseBranch(), ids);
        }
        if (process instanceof SequenceNode) collectIdentifiers(((SequenceNode) process).getTo(), ids);
    }

    static String findIdent(ASTNode firstProcess, String code) {
        Location loc = firstProcess.getLocation();
        String[] lines = code.split("\\n");
        lines = Arrays.copyOfRange(lines,loc.getLineStart()-1,loc.getLineEnd());
        if (loc.getLineEnd() != loc.getLineStart()) {
            lines[0] = lines[0].substring(loc.getColStart() - 1);
            lines[lines.length - 1] = lines[lines.length - 1].substring(0, loc.getColEnd() - 2);
        } else {
            lines[0] = lines[0].substring(loc.getColStart(), loc.getColEnd());
        }
        return String.join("",lines);
    }

    private String getNextOperationId(){
        return "op" + operationId++;
    }

    private void reset(){
        operationId = 0;
    }


    public static void addOperations(Class<? extends IOperationInfixFunction> clazz){
        String name = instantiate(clazz).getFunctionName();
        Logger.getLogger(OperationEvaluator.class.getSimpleName()).info("LOADED " + name + " FUNCTION PLUGIN");
        operationsMap.put(name.toLowerCase(),clazz);
    }

    public static <V> V instantiate(Class<V> clazz){
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            Logger.getLogger(OperationEvaluator.class.getSimpleName())
                    .log(Level.WARNING,clazz.getSimpleName() + " was unable to be added as an operation");
        }
        return null;
    }
}
