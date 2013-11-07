package net.scalaleafs.test

import net.scalaleafs.Template
import net.scalaleafs.Ident
import net.scalaleafs.Var
import net.scalaleafs.implicits._
import net.scalaleafs.Context
import net.scalaleafs.Noop

class BootstrapShowcase extends Template {

  val isActive = Var(false)
  
  def render = 
   "#biggest-buttons" #> {
     bind(isActive) { isActive =>
       "button" #> {
         addClass("active").when(isActive) &
         onclick(this.isActive.set(!isActive)) 
       }
     }
   }
}