package mc.compiler.parser;

import mc.compiler.ast.FunctionNode;
import mc.compiler.ast.ProcessNode;
import mc.compiler.ast.SequenceNode;
import mc.compiler.ast.TerminalNode;
import mc.exceptions.CompilationException;
import org.junit.Test;

import static org.junit.Assert.fail;

public class FunctionTests extends ParserTests {

    public FunctionTests() throws InterruptedException {
    }

    @Test
	public void correctAbsTest() throws CompilationException, InterruptedException {
		String input = "processes Test = abs(a -> STOP).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode expected = new FunctionNode("abs", sequence, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctSimpTest() throws CompilationException, InterruptedException {
		String input = "processes Test = simp(a -> STOP).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode expected = new FunctionNode("simp", sequence, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctSafeTest() throws CompilationException, InterruptedException {
		String input = "processes Test = safe(a -> STOP).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode expected = new FunctionNode("safe", sequence, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctPruneTest() throws CompilationException, InterruptedException {
		String input = "processes Test = prune(a -> STOP).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode expected = new FunctionNode("prune", sequence, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctNestedTest_1() throws CompilationException, InterruptedException {
		String input = "processes Test = simp(abs(a -> STOP)).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode function = new FunctionNode("abs", sequence, null);
		FunctionNode expected = new FunctionNode("simp", function, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctNestedTest_2() throws CompilationException, InterruptedException {
		String input = "processes Test = simp(abs(prune(a -> STOP))).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode function1 = new FunctionNode("prune", sequence, null);
		FunctionNode function2 = new FunctionNode("abs", function1, null);
		FunctionNode expected = new FunctionNode("simp", function2, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctNestedTest_3() throws CompilationException, InterruptedException {
		String input = "processes Test = safe(simp(abs(prune(a -> STOP)))).\npetrinet Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode function1 = new FunctionNode("prune", sequence, null);
		FunctionNode function2 = new FunctionNode("abs", function1, null);
		FunctionNode function3 = new FunctionNode("simp", function2, null);
		FunctionNode expected = new FunctionNode("safe", function3, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctAutomataCastTest() throws CompilationException, InterruptedException {
		String input = "processes Test = automata(a -> STOP).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode expected = new FunctionNode("automata", sequence, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctPetriNetCastTest() throws CompilationException, InterruptedException {
		String input = "processes Test = petrinet(a -> STOP).\npetrinet Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode expected = new FunctionNode("petrinet", sequence, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctMixedCastTest_1() throws CompilationException, InterruptedException {
		String input = "processes Test = TokenRule(A2P(a -> STOP)).\nautomata Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode cast = new FunctionNode("petrinet", sequence, null);
		FunctionNode expected = new FunctionNode("automata", cast, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}

	@Test
	public void correctMixedCastTest_2() throws CompilationException, InterruptedException {
		String input = "processes Test = petrinet(automata(a -> STOP)).\npetrinet Test.";
		ProcessNode node = constructProcessNode(input);
		SequenceNode sequence = constructSequenceNode(new String[]{"a"}, new TerminalNode("STOP", null));
		FunctionNode cast = new FunctionNode("automata", sequence, null);
		FunctionNode expected = new FunctionNode("petrinet", cast, null);
		if(!expected.equals(node.getProcess())){
			fail("expecting function nodes to be equivalent");
		}
	}
}
