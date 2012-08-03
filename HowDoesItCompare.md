---
title: How does it compare?
category: qqqIntroduction
index: 2
layout: default
---

# ...to Lift?

It's no secret that ScalaLeafs was heavily inspired by Liftweb. There are templates, binding, CSS selectors, wiring, closures in sessions. I was really charmed by Lift's approach. In general, however, I found Lift quite large, trying to support every use case, making things quite incomprehesible at times.

But it's the little things I had trouble with. Like the flickering of the initial rendering of a wired page. Or unexpected behaviours when using wiring. Or trying to change the order of post-page javascript commands. Or the order in which fields are updated in a form post. I seemed to hit a lot of exceptions. I found myself writing layers on top of Lift to the point I decided I might just as well write my own framework. How hard can it be? Well, it is hard and a lot of work. But I hope I've succeeded. 

There are a few areas where a different design choices were made. Lift's view-first approach was abandoned for example. Because ScalaLeafs is built from the ground up with things like CSS and wiring in mind, you may find it more concise and consistent.

# ...to Play?

Play has a focus on scalability and the server-side of things. It is quite bare-bone for rich web-applications. Javascript goodness is left to programmer, which seems to be the trend. The sweet spot for ScalaLeafs are projects where there is a single team with mainly scala expertise that still wants to develop neat looking rich web applications fast. I've found myself in this situation more than once.

ScalaLeafs is stateful, closures are kept in sessions, and is inherently more difficult to scale. This doesn't mean to say it isn't. Most of Lift's scalabilty considerations also apply to ScalaLeafs: http://simply.liftweb.net/index-Chapter-20.html

