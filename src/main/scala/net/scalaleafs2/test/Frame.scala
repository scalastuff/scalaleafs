package net.scalaleafs2.test

import net.scalaleafs2._

object MainMenu extends Enumeration {
  val home = Value("home")
}

class Frame(window: Window) extends Template {

  val url = window.url

  val securityContext = SecurityContextVar
  
  println("URL: " + url.head.get(null))

  def render =
    "#main-menu" #> { 
      bind(url.head) { head =>
        "li:not(.dropdown) a" #> linkHref &
        "li.home" #> addClass("active").when(head == "home") &
        "li.showcase" #> addClass("active").when(head == "showcase")
      }
    } & 
    "#page-container" #> 
      bind(url.head) { head =>
          Match(head) {
            case "showcase" => new BootstrapShowcase
            case s => <h1>mkElem("div.ruud"){head.get}</h1>
          }
    }
}