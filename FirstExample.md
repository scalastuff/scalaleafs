---
layout: default
title: First Example
---
# First example

    <div>
      <span id="elt1"/>
      <span id="elt2"/>
      <span id="elt3"/>
    </div>

    class FirstExample extends Template {
      def bind = 
        "#elt1" #> Hi there
        "#elt2" #> <h3>Hi There!</h3>
        "#elt3" #> AddClass("selected")
    }