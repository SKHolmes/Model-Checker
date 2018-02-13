package mc.operations.impl;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import mc.Constant;
import mc.exceptions.CompilationException;
import mc.processmodels.petrinet.Petrinet;
import mc.processmodels.petrinet.components.PetriNetEdge;
import mc.processmodels.petrinet.components.PetriNetPlace;
import mc.processmodels.petrinet.components.PetriNetTransition;

public class PetrinetParallelFunction {

  private static Set<String> unsynchedActions;
  private static Set<String> synchronisedActions;
  private static Map<Petrinet, Map<PetriNetPlace, PetriNetPlace>> petriPlaceMap;
  private static Map<Petrinet, Map<PetriNetTransition, PetriNetTransition>> petriTransMap;

  public static Petrinet compose(Petrinet p1, Petrinet p2) {
    clear();

    for(String eId : p1.getEdges().keySet()) {
      Set<String> owners = p1.getEdges().get(eId).getOwners();
      if(owners.contains(Petrinet.DEFAULT_OWNER)) {
        owners.clear();
      }

      owners.add(p1.getId());
    }

    for(String eId : p2.getEdges().keySet()) {
      Set<String> owners = p2.getEdges().get(eId).getOwners();
      if(owners.contains(Petrinet.DEFAULT_OWNER)) {
        owners.clear();
      }

      owners.add(p2.getId());
    }


    setupActions(p1, p2);
    Petrinet composition = new Petrinet(p1.getId() + "||" + p2.getId(), false);
    composition.getOwners().clear();
    composition.getOwners().addAll(p1.getOwners());
    composition.getOwners().addAll(p2.getOwners());

    addPetrinet(composition,p1).forEach(composition::addRoot);
    addPetrinet(composition,p2).forEach(composition::addRoot);

    setupSynchronisedActions(p1, p2, composition);
    return composition;
  }

  private static void setupActions(Petrinet p1, Petrinet p2) {
    Set<String> actions1 = p1.getAlphabet().keySet();
    Set<String> actions2 = p2.getAlphabet().keySet();
    actions1.forEach(a -> setupAction(a, actions2));
    actions2.forEach(a -> setupAction(a, actions1));
  }

  private static void setupAction(String action, Set<String> otherPetrinetActions) {
    if (action.equals(Constant.HIDDEN) || action.equals(Constant.DEADLOCK)) {
      unsynchedActions.add(action);

      // broadcasting actions are always unsynched
    } else if (action.endsWith("!")) {
      if (containsReceiverOf(action, otherPetrinetActions)) {
        synchronisedActions.add(action);
      }
      if (containsBroadcasterOf(action, otherPetrinetActions)) {
        synchronisedActions.add(action);
      } else {
        unsynchedActions.add(action);
      }
    } else if (action.endsWith("?")) {
      if (!containsBroadcasterOf(action, otherPetrinetActions)) {
        if (containsReceiverOf(action, otherPetrinetActions)) {
          synchronisedActions.add(action);
        }

        unsynchedActions.add(action);
      }
    } else if (otherPetrinetActions.contains(action)) {
      synchronisedActions.add(action);
    } else {
      unsynchedActions.add(action);
    }
  }

  @SneakyThrows(value = {CompilationException.class})
  private static void setupSynchronisedActions(Petrinet p1, Petrinet p2, Petrinet comp) {
    for (String action : synchronisedActions) {

      Set<PetriNetTransition> p1Pair = new HashSet<>(p1.getAlphabet().get(action)).stream()
          .map(t -> petriTransMap.get(p1).get(t)).collect(Collectors.toSet());

      Set<PetriNetTransition> p2Pair = new HashSet<>(p2.getAlphabet().get(action)).stream()
          .map(t -> petriTransMap.get(p2).get(t)).collect(Collectors.toSet());

      for (PetriNetTransition t1 : p1Pair) {
        for (PetriNetTransition t2 : p2Pair) {


          Set<PetriNetEdge> outgoingEdges = new HashSet<>();
          outgoingEdges.addAll(t1.getOutgoing());
          outgoingEdges.addAll(t2.getOutgoing());

          Set<PetriNetEdge> incomingEdges = new HashSet<>();
          incomingEdges.addAll(t1.getIncoming());
          incomingEdges.addAll(t2.getIncoming());

          PetriNetTransition newTrans = comp.addTransition(action);


          for(PetriNetEdge outE : outgoingEdges) {
            comp.addEdge((PetriNetPlace) outE.getTo(), newTrans, outE.getOwners());
          }

          for(PetriNetEdge inE : incomingEdges) {
            comp.addEdge(newTrans, (PetriNetPlace) inE.getFrom(), inE.getOwners());
          }


        }
      }

      for (PetriNetTransition oldTrans : Iterables.concat(p1Pair, p2Pair)) {
        comp.removeTransititon(oldTrans);
      }

    }
  }

  private static boolean containsReceiverOf(String broadcaster, Collection<String> otherPetrinet) {
    for (String reciever : otherPetrinet) {
      if (reciever.endsWith("?")) {
        if (reciever.substring(0, reciever.length() - 1).equals(broadcaster.substring(0, broadcaster.length() - 1))) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean containsBroadcasterOf(String broadcaster, Set<String> otherPetrinet) {
    String broadcastAction = broadcaster.substring(0, broadcaster.length() - 1);
    for (String receiver : otherPetrinet) {
      if (receiver.endsWith("!")) {
        String action = receiver.substring(0, receiver.length() - 1);
        if (action.equals(broadcastAction)) {
          return true;
        }
      }
    }

    return false;
  }

  private static void clear() {
    unsynchedActions = new HashSet<>();
    synchronisedActions = new HashSet<>();
    petriPlaceMap = new HashMap<>();
    petriTransMap = new HashMap<>();
  }

  @SneakyThrows(value = {CompilationException.class})
  public static Set<PetriNetPlace> addPetrinet(Petrinet addTo, Petrinet petriToAdd) {


    Set<PetriNetPlace> roots = new HashSet<>();
    Map<PetriNetPlace, PetriNetPlace> placeMap = new HashMap<>();
    Map<PetriNetTransition, PetriNetTransition> transitionMap = new HashMap<>();

    for (PetriNetPlace place : petriToAdd.getPlaces().values()) {
      PetriNetPlace newPlace = addTo.addPlace();
      newPlace.copyProperties(place);

      if (place.isStart()) {
        newPlace.setStart(false);
        roots.add(newPlace);
      }

      placeMap.put(place, newPlace);
    }

    for (PetriNetTransition transition : petriToAdd.getTransitions().values()) {
      PetriNetTransition newTransition = addTo.addTransition(transition.getLabel());
      transitionMap.put(transition, newTransition);
    }

    for (PetriNetEdge edge : petriToAdd.getEdges().values()) {

      if (edge.getFrom() instanceof PetriNetPlace) {
        addTo.addEdge(transitionMap.get(edge.getTo()), placeMap.get(edge.getFrom()), edge.getOwners());
      } else {
        addTo.addEdge(placeMap.get(edge.getTo()), transitionMap.get(edge.getFrom()), edge.getOwners());
      }
    }

    petriTransMap.put(petriToAdd, transitionMap);
    petriPlaceMap.put(petriToAdd, placeMap);
    return roots;
  }
}

