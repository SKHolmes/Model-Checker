package mc.compiler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.compiler.ast.AbstractSyntaxTree;
import mc.compiler.ast.ProcessNode;
import mc.exceptions.CompilationException;
import mc.process_models.ProcessModel;
import mc.process_models.automata.Automaton;
import mc.process_models.automata.generator.AutomatonGenerator;
import mc.process_models.automata.operations.AutomataOperations;
import mc.webserver.FakeContext;
import mc.webserver.webobjects.Context;
import mc.webserver.webobjects.LogMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Compiler {
    // fields
    private Lexer lexer;
    private Expander expander;
    private ReferenceReplacer replacer;
    private Interpreter interpreter;
    private OperationEvaluator evaluator;
    private EquationEvaluator eqEvaluator;
    private Parser parser;

    public Compiler() throws InterruptedException {
        this.lexer = new Lexer();
        parser = new Parser();
        this.expander = new Expander();
        this.replacer = new ReferenceReplacer();
        this.interpreter = new Interpreter();
        this.eqEvaluator = new EquationEvaluator();
        this.evaluator = new OperationEvaluator();
    }


    public CompilationObject compile(String code, Context context, com.microsoft.z3.Context z3Context, BlockingQueue<Object> messageQueue) throws CompilationException, InterruptedException {
        /**
         * As far as understood the below code is a dummy one for testing purpose that no longer functions.

         if (code.startsWith("random")) {
         messageQueue.add(new LogMessage("Generating random models"));
         boolean alphabet = Boolean.parseBoolean(code.split(",")[1]);
         int nodeCount = Integer.parseInt(code.split(",")[2]);
         int alphabetCount = Integer.parseInt(code.split(",")[3]);
         int maxTransitionCount = Integer.parseInt(code.split(",")[4]);
         Map<String,ProcessModel> models = new HashMap<>();
         int i = 0;
         List<ProcessModel> nodes = new AutomatonGenerator().generateAutomaton("A",new AutomataOperations(),new EquationEvaluator.EquationSettings(alphabet,alphabetCount,nodeCount,maxTransitionCount));
         for (ProcessModel model:nodes) {
         Automaton a = (Automaton) model;
         models.put(a.getId()+(i++),a);
         }
         return new CompilationObject(models,Collections.emptyList(),Collections.emptyList());
         }
         */
        return compile(parser.parse(lexer.tokenise(code), z3Context), code, z3Context, context, messageQueue);
    }
    private CompilationObject compile(AbstractSyntaxTree ast, String code, com.microsoft.z3.Context z3Context, Context context, BlockingQueue<Object> messageQueue) throws CompilationException, InterruptedException {
        HashMap<String,ProcessNode> processNodeMap = new HashMap<>();
        for (ProcessNode node: ast.getProcesses()) {
            processNodeMap.put(node.getIdentifier(), (ProcessNode) node.copy());
        }
        ast = expander.expand(ast, messageQueue, z3Context);
        ast = replacer.replaceReferences(ast, messageQueue);
        Map<String, ProcessModel> processMap = interpreter.interpret(ast, new LocalCompiler(processNodeMap, expander, replacer,messageQueue),messageQueue,z3Context);
        List<OperationResult> results = evaluator.evaluateOperations(ast.getOperations(), processMap, interpreter, code,z3Context);
        EquationEvaluator.EquationReturn eqResults = eqEvaluator.evaluateEquations(ast.getEquations(), code, context,z3Context,messageQueue);
        processMap.putAll(eqResults.getToRender());
        if (!(context instanceof FakeContext)) {

            List<Automaton> toPosition = processMap.values().stream().filter(Automaton.class::isInstance).map(s -> (Automaton)s).filter(s -> s.getNodeCount() <= context.getAutoMaxNode()).collect(Collectors.toList());

            int counter = toPosition.size();
            for (Automaton automaton : toPosition) {
                messageQueue.add(new LogMessage("Performing layout for @|black "+automaton.getId()+"|@, remaining: "+counter--,true,false));
                automaton.position();
            }

        }
        return new CompilationObject(processMap, results, eqResults.getResults());
    }
    @AllArgsConstructor
    public static class LocalCompiler {
        @Getter
        private HashMap<String,ProcessNode> processNodeMap;
        private Expander expander;
        private ReferenceReplacer replacer;
        private BlockingQueue<Object> messageQueue;

        public ProcessNode compile(ProcessNode node, com.microsoft.z3.Context context) throws CompilationException, InterruptedException {
            node = expander.expand(node,messageQueue,context);
            node = replacer.replaceReferences(node,messageQueue);
            return node;
        }
    }
}
