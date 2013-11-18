package net.scalaleafs.sample

import net.scalaleafs._

class Frame extends Template {
  
  val url = CurrentUrl

  def render =
    "#main-menu" #> {
      "li:not(.dropdown) a" #> linkHref &
      bind(url.headOption) { head =>
        "li.home" #> addClass("active").when(head == None) &
        "li.calendar" #> addClass("active").when(head == Some("calendar")) &
        "li.calendar2" #> addClass("active").when(head == Some("calendar2")) &
        "li.music" #> addClass("active").when(head == Some("music")) &
        "li.books" #> addClass("active").when(head == Some("books")) 
      }
    } & 
    "#page-container" #> 
      bind(url.headOption) { head =>
          Match(head) {
            case None => new Home
            case Some("calendar") => new Calendar 
            case Some("calendar2") => new Calendar2 
            case Some("music") => new Music
            case Some("books") => new Books
            case Some(other) => <h1>No page here: {other}</h1>
          }
    }

}