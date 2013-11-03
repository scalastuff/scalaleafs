package net.scalaleafs2.test

import net.scalaleafs2.Template
import net.scalaleafs2.IdentRenderNode
import net.scalaleafs2.Var
import net.scalaleafs2.implicits._
import net.scalaleafs2.Context
import net.scalaleafs2.Noop

class BootstrapShowcase extends Template {

  val isActive = Var(false)
  
  def render = 
   "#biggest-buttons" #> {
     isActive bind { isActive =>
       "button" #> {
         addClass("active", isActive) &
         onclick {
           context => 
             this.isActive.set(!isActive) 
         }
       }
     }
   }
}