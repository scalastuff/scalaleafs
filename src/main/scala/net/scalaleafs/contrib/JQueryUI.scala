package net.scalaleafs.contrib
import net.scalaleafs._
import scala.xml.Elem
import scala.xml.NodeSeq
import java.util.UUID

object JQueryUI {

  private def bindEvent(id : String, event : String, cmd : JSCmd) : JSCmd =
  bindEvent(id, event, cmd.toString)
    
  private def bindEvent(id : String, event : String, cmd : String) : JSCmd = 
    JSCmd(id + ".off(" + event + ").on(" + event + ", function(e) { " + cmd + "})")


  def searchBox(text : Var[String], defaultText : String = "") = new ElemWithIdTransformation {
    def apply(elem : Elem, id : String) : NodeSeq = {
      val inputId = id + "-input"
      R.addPostRequestJs {
        bindEvent(inputId, "focus",  "if (this.value == '" + defaultText + "') { this.value = ''; $(this.parentNode).removeClass('ui-search-box-default') }") &
        bindEvent(inputId, "blur",  "if (this.value == '') { $(this.parentNode).addClass('ui-search-box-default'); this.value = '" + defaultText + "' } ") 
      }
      text.onChange { value =>
        R.addPostRequestJs {
          if (text == "") JSCmd("$('#" + inputId + "').val('" + defaultText + "').parent().addClass('ui-search-box-default')")
          else JSCmd("$('#" + inputId + "').val('" + text + "').parent().removeClass('ui-search-box-default')")
        }
      }
      // calculate initial text and default for smoother rendering
      val (initialText, initialDefault) = text.get match {
        case "" => (defaultText, " ui-search-box-default")
        case s => (s, "")
      }
      
      val clearText = R.callback(_ => text.set(""))
      <span class={"ui-search-box ui-corner-all" + initialDefault}>
        <a class="ui-search-box-clear ui-icon" onclick={R.callback(_ => text.set("")) & JsReturnFalse}> </a>
        <span class="ui-search-box-icon"> </span>
        <input id={inputId} type="text" class="ui-search-box-input" onblur={R.callback(s => text.set("s")) & JsReturnFalse} value={initialText} />
    </span>
    }
  } 
}