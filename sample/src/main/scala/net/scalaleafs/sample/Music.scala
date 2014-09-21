package net.scalaleafs.sample

import net.scalaleafs._

case class Album(title : String, artist : String, image : String, var state: Boolean = false)

object AlbumDao {
  def fetchAlbums = {
    println("Fetching albums")
    Album("Songs of Love & Hate", "Leonard Cohen", "http://ecx.images-amazon.com/images/I/51mvXVc%2BbqL._AA115_.jpg") :: 
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") :: 
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Purple Rain", "Prince", "http://ecx.images-amazon.com/images/I/51WSAG1qvML._AA160_.jpg") ::
    Album("Dummy", "Portishead", "http://ecx.images-amazon.com/images/I/41Nze3UW5OL._AA160_.jpg") :: Nil
  }
}

class Music extends Template {


  val search = Var("")
  val albums = search.map(s => AlbumDao.fetchAlbums.filter(album => album.artist.contains(s) || album.title.contains(s)))
  
  def render = 
    ".search-box" #> bind(search) { s =>
      setAttr("value", s) &
      onchange(search.set(_) & Noop)
    } &
    ".clear" #> onclick(search.set("")) &
    "#album-table tbody tr" #> bindAll(albums) { album =>
      ".image" #> setAttr("src", album.image) &
      ".title" #> (album.title + "!") &
      ".artist" #> album.artist &
      ".btn" #>  {
        onclick({(album.state = !album.state); albums.trigger() }& Noop) &
        addClass(if (album.state) "btn-warning" else "btn-default")
      }
    }
}

class AlbumTemplate(album: Placeholder[Album]) extends Template {
  val v = Val(album.get)
  def render = bind(v) { album =>
      ".image" #> setAttr("src", album.image) &
      ".title" #> (album.title + "!") &
      ".artist" #> album.artist &
      ".btn" #>  {
        onclick({(album.state = !album.state); v.trigger() }& Noop) &
        addClass(if (album.state) "btn-warning" else "btn-default")
    } 
  }
}
