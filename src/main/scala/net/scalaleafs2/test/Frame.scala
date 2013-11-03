package net.scalaleafs2.test

import net.scalaleafs2._

object MainMenu extends Enumeration {
  val home = Value("home")
}

class Frame(window: Window) extends Template {

  val url = window.url

  println("URL: " + url.head.get(null))

  def render =
    "#main-menu" #> {
      url.head.bind { head =>
        "li:not(.dropdown) a" #> linkHref &
        Match(head) {
          case "home" => "#main-menu > li.home" #> addClass("active")
          case "showcase" => "#main-menu > li.showcase" #> addClass("active")
            
          case s => IdentRenderNode
        }
      }
    } & 
    "#page-container" #> 
      url.head.bind { head =>
        replaceContent {
          Match(head) {
            case "showcase" => new BootstrapShowcase
            case s => <h1>mkElem("div.ruud"){head.get}</h1>
          }
        }
    }
}