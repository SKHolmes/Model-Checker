# Model-Checker [![Build Status](https://jenkins.tangentmc.net/job/Model-Checker/badge/icon)](https://jenkins.tangentmc.net/job/Model-Checker/)
[Currently hosted application](http://modelchecker-swen302.herokuapp.com/).    
NB: URL may change and this link will be useless.   
Streader's Raiders SWEN302 Group Project

## Overview

Automata Concocter is developed using the [Polymer 1.0](https://www.polymer-project.org/1.0/) framework. Polymer uses
predefined elements in a composite pattern to create web based applications in HTML, CSS and Javascript. All elements
used to create this application can be found on the Polymer website in the element catalogue. The Polymer framework
requires Bower to manage third party libraries. Information on Bower and Bower installation can be found at their
website.

Bower Home:		http://bower.io/  
Bower Install:		http://bower.io/#install-bower  
Polymer 1.0 Home:	https://www.polymer-project.org/1.0/  
Polymer 1.0 Catalogue:	https://elements.polymer-project.org/   
Polymer 1.0 Dev Guide:	https://www.polymer-project.org/1.0/docs/devguide/feature-overview.html   

### Building

For windows devices, run install.bat
For nix devices, run install.sh

###Main Application
-----------------------
The Automata Concocter is a web based application that constructs finite state automata based on text input of the
user and was designed as an educational tool for students studying software engineering. The AC allows the user to
define multiple automata and navigate through diffirent edges to reach different states within the user defined state
machines. The user can save defined automata as a txt file and can upload a previously saved txt file with defined
state machines. The AC uses a static predefined grammar found in  (Model-Checker/app/elements/automata-parser/automata-grammar.pegjs)

###Custom Elements
-----------------------
All custom elements are located within there corresponding directories in 'Model-Checker/elements/' and include the index
file and corresponding html file of the same name as the given directory that includes the script utilizied by the element. 

  * **text-editor**
  
    The text-editor custom element is used to describe Automata by the user and has a live compiling option which can
be set
    to on or off in the toolbar. Assuming the live compiling option is off, definitions are retrieved from the text editor 
    using the private getCode function outlined in text-editor.html when the compile button is used. SetCode is used here when
    the user opts to load a txt file of their choosing and updates the corresponding field within this element. If live compilation
    is on, the text given by the user is updated on each key press by firing a text-editor-change event. These events are limited
    to be a second away from each other at least. This value is given as the changeEventDebounceTime defined in text-editor.html.

  * **automata-walker**
   
     This element outlines the walker in its entirety. By updating the Automata dropdown menu on each compilation the user can
    then select a given automata and a corresponding edge based on the current Node. The _walk function is invoked onClick of
    the walk button. The automata-visalization is then updated to highlight the current node the user has navigated through.

  * **automata-visualisation**
   
    This element is responsible for the visualisatising of automata. It interacts directly
    with the DagreD3 library and renders the graphs on screen in the appropriate place in
    the SVG group.  

  * **parser**
  
    This file is automatically generated using our grammar in the automata-grammar.pegjs file. It is generated using the [PEGJS library](http://pegjs.org). Test out how to write grammars [here](http://pegjs.org/online).

  * **console-logger**

    Based on the type of message to be printed (log,warn,error) to the console the corrosponding functions are called within 
    console-logger.html passing a String containing the message. These methods are invoked when interpreting the given
    definitions by the user.

###Styles
-----------------------

Main application css files can be found in the styles directory (Model-Checker/app/styles/). Styling in relation to custom elements can be altered from a custom elements corosponding html file. 

####Where to from here...

So you're a SWEN302 student and you want to further enchance the Automata Concocter... These are some tasks which we did no have the time to complete:   
- [ ] Hiding.
- [ ] Remove unimplemented grammar errors.
- [ ] Parallelisation.
- [ ] Fix Walker drop-down-box bug. Get ECS dept. to update Chrome! Currently: 40.0.2214.91
