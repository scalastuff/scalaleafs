package net.scalaleafs.test

import net.scalaleafs._

object MainMenu extends Enumeration {
  val home = Value("home")
}

object CurrentSecurityContext extends RequestVar[Option[SecurityContext]](None)

class Frame(window: Window) extends Template {

  val url = CurrentUrl

  val securityContext = CurrentSecurityContext

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