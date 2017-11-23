# Model-Checker 

[![Build Status](https://img.shields.io/travis/davidstreader/Model-Checker.svg)](https://jenkins.tangentmc.net/job/Model-Checker/)
[![GitHub release](https://img.shields.io/github/release/davidstreader/Model-Checker.svg)](https://github.com/davidstreader/Model-Checker/releases)
[Click for the latest jar build from master](https://jenkins.tangentmc.net/job/Model-Checker)

## Overview

### Main Application

----------------------- 

The Automata Concocter is a web based application that
constructs finite state automata based on text input of the user and was
designed as an educational tool for students studying software engineering. The
AC allows the user to define multiple automata and navigate through diffirent
edges to reach different states within the user defined state machines. The user
can save defined automata as a txt file and can upload a previously saved txt
file with defined state machines.

### Building / Distributing

-----------------------

## Linux
```bash 
$cd modelchecker
$./gradlew build
$cd ..
```
Please be aware that if you have a web authentication proxy you will need to add arguments to gradlew.

## Windows
Ensure that the `JAVA_HOME` environmental variable is set correctly.

Use `git bash` to run the gradle build script, as `npm` is tempremental
```bash
$cd modelchecker
$./gradlew build
```


This will build a jar file `ModelChecker.jar` in the root folder of the
repository


### Web interface

-----------------------

Main web source files can be found in the `app` directory
(Model-Checker/app/).


### Overall Structure

-----------------------

Lexer -> Parser -> Interpreter -> Evaluator -> Graphical display

The lexer produces a list of Tokens from the input code given via the web interface, this list is then passed to the Parser.  
The Parser produces an AST (Abstract syntax tree) which is then used by the interperator to build automata diagrams.
Finally the evaluator first tests the operations (Which are similar to tests), then it moves onto testing equations which is done by generating different automata and the applying 
the user created automata to test if it works over a given set space.
This is then sent to the web interface via websocket.
