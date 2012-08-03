---
title: How does it compare?
category: Introduction
index: 2
layout: default
---

# ...to Lift?

It's no secret that ScalaLeafs was heavily inspired by Liftweb. There are templates, binding, CSS selectors, wiring, closures in sessions. I was really charmed by Lift's approach. However Lift is quite large, trying to support every use case. But it's the little things I found myself in trouble with. Like the flickering of the initial rendering of a wired page. Or unexpected behaviours when using wiring. Or trying to change the order of post-page javascript commands. Or the order in which fields are updated in a form post. I seemed to hit a lot of exceptions. I found myself writing layers on top of Lift to the point I decided I might just as well write my own framework. How hard can it be? Well, it is hard and a lot of work. But I hope I've succeeded. Lift's view-first approach was abandoned. Because ScalaLeafs is built from the ground up with, for example, CSS and wiring in mind, you may find it more concise and consistent.

# ...to Play?

Play has a focus on scalability and the server-side of things. It is quite bare-bone for rich web-applications. Javascript goodness is left to programmer, which seems to be the trend. The sweet spot for ScalaLeafs are projects where there is a single team with mainly scala expertise that still wants to develop neat looking rich web applications fast. I've found myself in this situation more than once.

ScalaLeafs is stateful, closures are kept in sessions, and is inherently more difficult to scale. However, not everyone is building facebook-scale applications. Most of Lift's scalabilty considerations also apply to ScalaLeafs: http://simply.liftweb.net/index-Chapter-20.html

