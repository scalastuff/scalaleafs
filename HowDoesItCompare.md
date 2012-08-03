---
title: How does it compare?
category: Introduction
index: 2
layout: default
---

# ...to Play?


Play has a focus on scalability and the server-side of things, it is quite bare-bone for rich web-applications. Javascript goodness is left to programmer, which seems to be the trend. The sweet spot for ScalaLeafs are projects where there is a single team with mainly scala expertise that still wants to develop neat looking rich web applications fast. I've found myself in this situation more than once.

ScalaLeafs is stateful, closures are kept in sessions, and is inherently more difficult to scale. However, not everyone is building facebook-scale applications (let's be real). I fully concur with David Pollack's view on this topic: http://simply.liftweb.net/index-Chapter-20.html

# ...to Lift?

It's no secret that ScalaLeafs was heavily inspired by Liftweb. There is Templates, binding, CSS selectors, wiring, closures in sessions. Sometimes the same terminology is used, sometimes not. In general, I found Lift to be quite large, but some little things I really needed were not supported. Things like CSS selectors, wiring look similar but work differently. Because there are so many ways to do things there is little guidance, which can be problematic if you're working in a team.  Because ScalaLeafs is built from the ground up with CSS and wiring in mind, you may find it more concise and consistent.
