'use strict';

function interpretAutomaton(process, processesMap, context){
  const processStack = [];
  const referenceMap = {};
  let override = false;

  const ident = process.ident.ident;
  const automaton = new Automaton(ident);
  const root = automaton.addNode();
  automaton.root = root.id;
  root.metaData.startNode = true;
  const interrupt = process.interrupt;
  interpretNode(process.process, automaton, root)
  if (process.interrupt) {
    interpretNode(process.interrupt.process, automaton, undefined);
  }
  if(process.hiding !== undefined){
    processHiding(automaton, process.hiding);
  }

  labelAutomaton(automaton);
  processesMap[ident] = automaton;

  function interpretSubAutomaton(subProcess, automaton){
    // setup the sub automaton
    const subAutomaton = new Automaton(automaton.id);
    subAutomaton.nodeId = automaton.nodeId;
    subAutomaton.edgeId = automaton.edgeId;

    // setup the sub root
    const subRoot = subAutomaton.addNode();
    subAutomaton.root = subRoot.id;
    subRoot.metaData.startNode = true;

    // interpret the sub process
    interpretNode(subProcess, subAutomaton, subRoot);

    // check if the process was overriden
    if(override){
      override = false;
      return;
    }

    // update the main automaton
    automaton.nodeId = subAutomaton.nodeId;
    automaton.edgeId = subAutomaton.edgeId;

    processStack.push(subAutomaton);
  }

  function interpretNode(astNode, automaton, currentNode){
    processReferencePointer(astNode, currentNode);
    switch(astNode.type){
      case 'sequence':
        interpretSequence(astNode, automaton, currentNode);
        break;
      case 'choice':
        interpretChoice(astNode, automaton, currentNode);
        break;
      case 'composite':
        interpretComposite(astNode, automaton, currentNode);
        break;
      case 'function':
        interpretFunction(astNode, automaton, currentNode);
        break;
      case 'identifier':
        interpretIdentifier(astNode, automaton, currentNode);
        break;
      case 'terminal':
        currentNode.metaData.isTerminal = astNode.terminal;
        break;
      default:
        break;
    }
    if(astNode.label !== undefined){
      processLabelling(automaton, astNode.label.action);
    }

    if(astNode.relabel !== undefined){
      processRelabelling(automaton, astNode.relabel.set);
    }
  }

  function interpretSequence(astNode, automaton, currentNode) {
    const next = (astNode.to.type === 'reference') ? referenceMap[astNode.to.reference] : automaton.addNode();
    const id = automaton.nextEdgeId;
    const metadata = {};
    if (astNode.guard !== undefined) {
      metadata.guard = astNode.guard;
      metadata.next = astNode.next;
      metadata.variables = astNode.variables;
    }
    if (astNode.from.receiver) metadata.receiver = true;
    if (astNode.from.broadcaster) metadata.broadcaster = true;
    if (typeof astNode.from.action !== 'string') astNode.from.action = astNode.from.action.label;
    //Not interrupt, currentNode is defined
    if (currentNode) {
      automaton.addEdge(id, astNode.from.action, currentNode, next, metadata);
    } else {
      automaton.nodes.forEach(node => {
        if (node.metaData.isPartOfInterrupt || node == next) return;
        node.metaData.isPartOfInterrupt = true;
        const id = automaton.nextEdgeId;
        automaton.addEdge(id, astNode.from.action, node, next, {interrupt: process.interrupt});
      });
    }
    if(astNode.to.type !== 'reference'){
      interpretNode(astNode.to, automaton, next);
    }
  }

  function interpretChoice(astNode, automaton, currentNode){
    interpretNode(astNode.process1, automaton, currentNode);
    interpretNode(astNode.process2, automaton, currentNode);
  }

  function interpretComposite(astNode, automaton, currentNode){
    interpretSubAutomaton(astNode.process1, automaton);
    interpretSubAutomaton(astNode.process2, automaton);

    const automaton2 = processStack.pop();
    const automaton1 = processStack.pop();
    const composedAutomaton = parallelComposition(automaton.id + '.comp', automaton1, automaton2);

    combineAutomata(automaton, composedAutomaton, currentNode);
  }

  function interpretFunction(astNode, automaton, currentNode){
    interpretSubAutomaton(astNode.process, automaton);

    let processedAutomaton = processStack.pop();
    switch(astNode.func){
      case 'abs':
        processedAutomaton = abstraction(processedAutomaton, context.isFairAbstraction);
        break;
      case 'simp':
        processedAutomaton = bisimulation(processedAutomaton);
        break;
      case 'safe':
        processedAutomaton = tokenRule(processedAutomaton, 'unreachableStates');
        processStack.push(processedAutomaton);
        override = true;
        return;
      case 'automata':
        processedAutomaton = tokenRule(processedAutomaton, 'toAutomaton');
        break
      default:
        break;
    }

    combineAutomata(automaton, processedAutomaton, currentNode);
  }

  function interpretIdentifier(astNode, automaton, currentNode){
    const reference = processesMap[astNode.ident].clone;
    // check if the reference is not an automata
    if(reference.type !== 'automata'){
      processStack.push(reference);
      override = true;
      return;
    }

    combineAutomata(automaton, reference, currentNode)
  }

  function combineAutomata(automaton, toAdd, currentNode){
    const root = toAdd.root;
    toAdd.root = undefined;
    delete root.metaData.startNode;
    automaton.addAutomaton(toAdd);
    automaton.combineNodes(currentNode, root);
  }

  function processReferencePointer(astNode, currentNode){
    if(astNode.reference !== undefined){
      referenceMap[astNode.reference] = currentNode;
    }
  }

  function processHiding(automaton, hidingSet){
    const alphabet = automaton.alphabet;
    const set = {};

    for(let i = 0; i < hidingSet.set.length; i++){
      set[hidingSet.set[i]] = true;
    }

    for(let label in alphabet){
      if(set[label] !== undefined && hidingSet.type === 'includes'){
        automaton.relabelEdges(label, TAU);
      }
      else if(set[label] === undefined && hidingSet.type === 'excludes'){
        automaton.relabelEdges(label, TAU);
      }
    }
  }

  function processLabelling(automaton, label){
    const alphabet = automaton.alphabet;
    for(let action in alphabet){
      automaton.relabelEdges(action, label + '.' + action);
    }
  }

  function processRelabelling(automaton, relabelSet){
    for(let i = 0; i < relabelSet.length; i++){
      const newLabel = relabelSet[i].newLabel.action;
      const oldLabel = relabelSet[i].oldLabel.action;
      automaton.relabelEdges(oldLabel, newLabel);
    }
  }

  function labelAutomaton(automaton){
    let label = 0;
    const visited = {};
    const fringe = [automaton.root];
    let index = 0;
    while(index < fringe.length){
      const current = fringe[index++];
      if(visited[current.id]){
        continue;
      }

      visited[current.id] = true;
      current.metaData.label = label++;

      const neighbours = current.outgoingEdges.map(id => automaton.getEdge(id)).map(e => automaton.getNode(e.to));
      for(let i = 0; i < neighbours.length; i++){
        const neighbour = neighbours[i];
        if(!visited[neighbour.id]){
          fringe.push(neighbour);
        }
      }
    }
  }
}
