'use strict'

class PetriNet{

	constructor(){
		this._type = 'petrinet';
		this._placeMap = {};
		this._placeCount = 0;
		this._transitionMap = {};
		this._transitionCount = 0;
		this._rootId = undefined;
		this._nextPlaceId = 0;
		this._nextTransitionId = 0;
	}

	/**
	 * Returns the fact that this data structure represents a petri net.
	 *
	 * @return {string} - petrinet
	 */
	get type(){
		return this._type;
	}

	/**
	 * Returns the root place for this petri net.
	 *
	 * @return {place} - the root
	 */
	get root(){
		return this._placeMap[this._rootId];
	}

	/**
	 * Sets the specified place as the new root of this petri net.
	 * If the specified place is not in this petri net then an
	 * exception is thrown.
	 *
	 * @param {place} place - the new root id
	 * @return {place} - the new root place
	 */
	set root(place){
		if(place === this.root){
			return place;
		}

		// find new root
		for(let i in this._placeMap){
			if(place === this._placeMap[i]){
				this._rootId = parseInt(i, 10);
				return this._placeMap[i];
			}
		}

		// root was not found, throw exception
	}

	/**
	 * Returns the unique identifier for the root place of this petri net.
	 *
	 * @return {int} - the root id
	 */
	get rootId(){
		return this._rootId;
	}

	/**
	 * Returns an array of the current places in this petri net. The root place
	 * is the first element in the array.
	 *
	 * @return {place[]} - the array of places
	 */
	get places(){
		let places = [this.root];
		
		// add the remaining places
		for(let i in this._placeMap){
			// only add place if it is not the root
			if(parseInt(i, 10) !== this._rootId){
				places.push(this._placeMap[i]);
			}
		}

		return places;
	}

	/**
	 * 
	 */
	getPlace(id){
		if(this._placeMap[id] !== undefined){
			return this._placeMap[id];
		}

		// place not found, throw exception
	}

	/**
	 * Adds a new place to this petri net and returns it.
	 *
	 * @return {place} - the constructed place
	 */
	addPlace(){
		let id = this._nextPlaceId++;
		let place = new PetriNet.Place(id);
		this._placeMap[id] = place;
		this._placeCount++;
		return place;
	}

	/**
	 * Returns the number of places currently in this petri net.
	 *
	 * @return {int} - count of places
	 */
	get placeCount(){
		return this._placeCount;
	}

	/**
	 * Returns an array of the current transitions in this petri net.
	 *
	 * @return {transition[]} - the array of transitions
	 */
	get transitions(){
		let transitions = [];
		
		// add the remaining places
		for(let i in this._transitionMap){
			transitions.push(this._transitionMap[i]);
		}

		return transitions;
	}

	getTransition(id){
		if(this._transitionsMap[id] !== undefined){
			return this._transitionsMap[id];
		}

		// transition not found, throw exception
	}

	/**
	 * Adds a new transition to this petri net with the specified label from the specified place.
	 * Constructs a new place that the new transition will transition to afterwards
	 * and returns that new place.
	 *
	 * @param {string} label - label that represents the transition
	 * @param {place} from - the place this transition will transition from
	 * @return {place} - the place that this transition will transition to
	 */
	addTransition(label, from){
		let id = this._nextTransitionId++;
		let transition = new PetriNet.Transition(id, label);
		this._transitionMap[id] = transition;

		from.addTransitionFromMe(transition);
		transition.addPlaceToMe(transition);
		let to = this.addPlace();
		to.addTransitionToMe(transition);
		transition.addPlaceFromMe(to);

		return to;
	}

	/**
	 * Returns the number of transitions currently in this petri net.
	 *
	 * @return {int} - count of transitions
	 */
	get transitionCount(){
		return this._transitionCount;
	}

	/**
	 * Merges the places in the specified array into a single
	 * place. The first element of the array is the place which the
	 * remaining elements will be merged with.
	 *
	 * @param {place[]} places - the array of places
	 */
	mergePlaces(places){
		let place = places[0];

		// merge remaining places to place
		for(let i = 1; i < places.length; i++){
			let current = places[i];
			let transitions = current.transitionsFromMe;

			for(let j = 0; j < transitions.length; j++){
				delete transitions[j]._placesToMe[current.id];
				transitions[j].addPlaceToMe(place);
				place.addTransitionFromMe(transitions[j]);
			}

			transitions = current.transitionsToMe;
			for(let j = 0; j < transitions.length; j++){
				delete transitions[j]._placesFromMe[current.id];
				transitions[j].addPlaceFromMe(place);
				place.addTransitionFromMe(transitions[j]);
			}

			delete this._placeMap[current.id];
			this._placeCount--;
		}
	}

	addPetriNet(net, place){
		// add nodes to this petri net
		let places = net.places;
		for(let i = 0; i < places.length; i++){
			let id = this._nextPlaceId++;
			places[i].id = id;
			this._placeMap[id] = places[i];
			this._placeCount++;
		}

		// add transitions to this petri net
		let transitions = net.transitions;
		for(let i = 0; i < transitions.length; i++){
			let id = this._nextTransitionId++;
			transitions[i].id = id;
			this._transitionMap[id] = transitions[i];
			this._transitionCount++;
		}

		// merge on the specified place
		this.mergePlaces([place, net.root]);
	}
}

PetriNet.Place = class {

	constructor(id){
		this._id = id;
		this._transitionsToMe = [];
		this._transitionsFromMe = []; 
		this._metaData = {};
	}

	/**
	 * Returns the unique identifier for this place.
	 *
	 * @return {int} - place id
	 */
	get id(){
		return this._id;
	}

	/**
	 * Sets the unique identifier of this place to the specified id
	 *
	 * @param {int} id - the new id value
	 */
	 set id(id){
	 	this._id = id;
	 }

	/**
	 * Returns an array of the ids of the transitions from this place.
	 *
	 * @return {int[]} - array of the ids for transitions from this place
	 */
	get transitionsFromMe(){
		return this._transitionsFromMe;
	}

	/**
	 * Adds the specified transition id to the the array of transition ids
	 * from this place. Only adds the id if the id is not already in the array
	 * of transitions.
	 *
	 * @param {id} transition - id of the transition to add
	 * @return {boolean} - true if the id was added, otherwise returns false
	 */
	addTransitionFromMe(id){
		// check if the id is alread in the array of transitions
		for(var i = 0; i < this._transitionsFromMe; i++){
			if(id = this._transitionsFromMe[i]){
				return false;
			}
		}

		// add id to array of transitions
		this._transitionsFromMe.push(id);
		return true;
	}

	/**
	 * Returns an array of the ids of the transitions that can move to this place.
	 *
	 * @return {int[]} - array of the ids for transitions to this place
	 */
	get transitionsToMe(){
		return this._transitionsToMe;
	}

	/**
	 * Adds the specified transition id to the the array of transition ids
	 * to this place. Only adds the id if the id is not already in the array
	 * of transition ids.
	 *
	 * @param {id} transition - id of the transition to add
	 * @return {boolean} - true if the id was added, otherwise returns false
	 */
	addTransitionToMe(id){
		// check if the id is alread in the array of transitions
		for(var i = 0; i < this._transitionsToMe; i++){
			if(id = this._transitionsToMe[i]){
				return false;
			}
		}

		// add id to array of transitions
		this._transitionsToMe.push(id);
		return true;		
	}

	/**
	 * Returns the meta data for this place associated with the specified
	 * key. If no meta data is available for this key then undefined is
	 * returned.
	 *
	 * @param {string} key - the key to get meta data for
	 * @return {?} - the value associated to the specified key
	 */
	getMetaData(key){
		return this._metaData[key];
	}

	/**
	 * Adds the specified key-value pair to the meta data of this place.
	 * If the key already exists in the meta data then that data is overridden.
	 *
	 * @param {string} key - the key to add
	 * @param {?} value - the value to add
	 */
	addMetaData(key, value){
		this._metaData[key] = value;
	}

	/**
	 * Deletes the meta data associated with this key from the meta data
	 * of this place.
	 *
	 * @param {string} key - the key to delete data for
	 */
	deleteMetaData(key){
		delete this._metaData[key];
	}

}

PetriNet.Transition = class {

	constructor(id, label){
		this._id = id;
		this._label = label;
		this._placesToMe = [];
		this._placesFromMe = []; 
		this._metaData = {};
	}

	/**
	 * Returns the unique identifier for this transition.
	 *
	 * @return {int} - place id
	 */
	get id(){
		return this._id;
	}

	/**
	 * Sets the unique identifier of this transition to the specified id
	 *
	 * @param {int} id - the new id value
	 */
	 set id(id){
	 	this._id = id;
	 }

	/**
	 * Returns the label associated with this transition.
	 *
	 * @return {string} label - the label for this transition
	 */
	get label(){
		return this._label;
	}

	/**
	 * Sets the label associated with this transition to the specified
	 * label.
	 *
	 * @param {string} label - the new label
	 * @return {string} - the new label
	 */
	set label(label){
		this._label = label;
		return this._label;
	}

	/**
	 * Returns an array of the ids for places from this transition.
	 *
	 * @return {int[]} - place from this place
	 */
	get placesFromMe(){
		return this._placesFromMe;
	}

	/**
	 * Adds the specified id of the place to the array of places that move from this 
	 * transition. Only adds the id if it is not already located in the array of 
	 * place ids. 
	 *
	 * @param {place} place - id of the place to add
	 * @return {boolean} - returns true if the id is added, otherwise returns false
	 */
	addPlaceFromMe(id){
		// check if the id is alread in the array of places
		for(var i = 0; i < this._placesFromMe; i++){
			if(id = this._placesFromMe[i]){
				return false;
			}
		}

		// add id to array of transitions
		this._placesFromMe.push(id);
		return true;
	}

	/**
	 * Returns an array of the places that can move to this transition.
	 *
	 * @return {int[]} - places to this transition
	 */
	get placesToMe(){
		return this._placesToMe;
	}

	/**
	 * Adds the specified id of the place to the array of places that move to this 
	 * transition. Only adds the id if it is not already located in the array of 
	 * place ids. 
	 *
	 * @param {int} id - id of the place to add
	 * @return {boolean} - returns true if the id is added, otherwise returns false
	 */
	addPlaceToMe(id){
		// check if the id is alread in the array of places
		for(var i = 0; i < this._placesToMe; i++){
			if(id = this._placesToMe[i]){
				return false;
			}
		}

		// add id to array of transitions
		this._placesToMe.push(id);
		return true;
	}
}

/**
 * Represents an exception that can be raised while processing a petri net.
 */
PetriNet.Exception = class {

	/**
	 * Constructs a new PetriNetException with the specified message.
	 *
	 * @oaram {string} message - the error message
	 */
	constructor(message){
		this.message = "PetriNetException: " + message;
	}
}