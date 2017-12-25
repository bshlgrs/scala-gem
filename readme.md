# graphical equation manipulator, Scala edition

I had the idea for this software in 2013, but I wasn't a good enough programmer to make it. You can see my first attempt 
on YouTube [here](https://www.youtube.com/watch?v=16eiGLrX248) 
(featuring an Australian accent much stronger than the one I have after living in the US for a few years). Matt Alger then
[rewrote some of it in Coffeescript](https://github.com/MatthewJA/Graphical-Equation-Manipulator).

I'm going to do it in Scala, because type systems make it much easier for me to think about what I'm doing.

Features to add:

## [x] Phase 1: build out more backend

- Add numbers to the workspace
  - each has an optional variable it's connected to; these must be unique
- Workspace should expose a list of possible actions:
  - equality-declaration
  - expression-creation
  - expression-rewriting
  - number-creating
  - number-connecting
  - all deletions
- Equations should know more information about themselves, such as
  - name of equation
  - name for every variable
  - dimensions of every variable
- Logic to assign subscripts to every variable to remove ambiguity
- Logic to represent:
  - Equations as strings. This involves giving them a new field
  - Expressions as strings
- Computer algebra system
  
## [x] Phase 2: build shitty frontend

Just make logic for displaying everything and also a button for every allowed action 

## [ ] Phase 3: make it less shitty

- less shitty GUI
  - similar to last time should work
    - I think I should just give up on using React
  
## later

- magic triangle GUI element
- make parsers for equations and dimensions and stuff
