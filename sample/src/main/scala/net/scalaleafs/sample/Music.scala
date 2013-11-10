package net.scalaleafs.sample

import net.scalaleafs._

case class Album(title : String, artist : String, image : String)

object AlbumDao {
  def fetchAlbums = 
    Album("Songs of Love & Hate", "Leonard Cohen", "http://ecx.images-amazon.com/images/I/51mvXVc%2BbqL._AA115_.jpg") :: 
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") :: Nil
}

class Music extends Template {
  
  val search = Var("")
  val albums = search.map(s => AlbumDao.fetchAlbums.filter(album => album.artist.contains(s) || album.title.contains(s)))
  
  def render = 
    ".search-box" #> bind(search) { s =>
      setAttr("value", s) &
      onchange(search.set)
    } &
    ".clear" #> onclick(search.set("")) &
    "#album-table tbody tr" #> bindAll(albums) { album =>
      ".image" #> setAttr("src", album.image) &
      ".title" #> album.title &
      ".artist" #> album.artist 
    }
}