package net.liftweb.widgets.tree

import scala.xml._
import net.liftweb.util.Helpers._
import net.liftweb.util.{Can, Full, Empty}
import net.liftweb.http.S._
import net.liftweb.http.LiftRules
import net.liftweb.http.{LiftResponse, JsonResponse}
import net.liftweb.http.js._
import net.liftweb.http.js.jquery._
import JsCmds._
import JE._
import JqJsCmds._
import JqJE._

object TreeView {
  
  def apply(id: String, jsObj: JsObj) = new TreeView().onLoad(id, jsObj)
  def apply(id: String, jsObj: JsObj, loadTree : () => List[Tree], loadNode: (String) => List[Tree]) = 
    new TreeView().onLoadAsync(id, jsObj, loadTree, loadNode)
  
  /**
   * Call this function typically in boot
   */
  def init() {
    import net.liftweb.http.ResourceServer
    ResourceServer.allow({
      case "tree" :: _ => true
    })
    
  }
}

class TreeView {
  
  /**
   * Makes a static tree out of the <ul><li> lists. The tree is buid when page loads.
   * 
   * @param id - the id of the empty <ul> element that will be populated with the tree
   * @param jsObj - the JSON object passed to the treeview function
   * 
   */
  def onLoad(id: String, jsObj: JsObj) : NodeSeq = {
    <head>
      <link rel="stylesheet" href="/classpath/tree/jquery.treeview.css" type="text/css"/>
      <script type="text/javascript" src="/classpath/tree/jquery.treeview.js"/>
       <script type="text/javascript" charset="utf-8">{
         OnLoad(JqId(id) >> new JsExp with JQueryRight {
           def toJsCmd = "treeview(" + jsObj.toJsCmd + ")"
         }) toJsCmd
       }
       </script>
    </head>

  }
  
  /**
   * Makes the tree to be loaded when the page loads
   * 
   * @param id - the id of the empty <ul> element that will be populated with the tree
   * @param jsObj - the JSON object passed to the treeview function
   * @param loadTree - the function that will be called when the entire tree will be dynamically loaded
   * @param loadNode - the function that will be called when a tree node (other then root) will be retrieved via Ajax
   * 
   */
  def onLoadAsync(id: String, jsObj: JsObj, loadTree : () => List[Tree], loadNode: (String) => List[Tree]): NodeSeq = {
     <head>
       <link rel="stylesheet" href="/classpath/tree/jquery.treeview.css" type="text/css"/>
       <script type="text/javascript" src="/classpath/tree/jquery.treeview.js"/>
       <script type="text/javascript" src="/classpath/tree/jquery.treeview.async.js"/>
       <script type="text/javascript" charset="utf-8">{
         OnLoad(makeTreeView(id, jsObj, loadTree, loadNode)) toJsCmd
       }
       </script>
     </head>
   
  }
  
  
  /**
   * @param id - the id of the empty <ul> element that will be populated with the tree
   * @param jsObj - the JSON object passed to the treeview function
   * @param loadTree - the function that will be called when the entire tree will be dynamically loaded
   * @param loadNode - the function that will be called when a tree node (other then root) will be retrieved via Ajax
   * 
   * @return JsExp - the Java Script expression that calls the treeview function on the element denominated by the id
   * 
   */
  def makeTreeView(id: String, jsObj: JsObj, loadTree : () => List[Tree], loadNode: (String) => List[Tree]): JsExp = {
     val treeFunc : () => LiftResponse = {
     case _ => request match {
       case Full(req) => req.params.get("root") match {
         case Some("source" :: _) => JsonResponse(JsRaw(Tree.toJSON(loadTree())))
         case Some(nodeId :: _) => JsonResponse(JsRaw(Tree.toJSON(loadNode(nodeId))))
         case _ => JsonResponse(JsRaw("[]"))
       }
       case _ => JsonResponse(JsRaw("[]"))
       }
     }

     val key = mapFunc(NFuncHolder(treeFunc))
     
     val url = encodeURL(contextPath +
			 "/"+LiftRules.ajaxPath)+"?"+key+"=_"
 
     val obj: JsObj = JsObj(("url" -> Str(url)) :: jsObj.props:_*)
     
     JqId(id) >> new JsExp with JQueryRight {
       def toJsCmd = "treeview(" + obj.toJsCmd + ")"
     }
  }
  
}

object Tree {
  def apply(text:String) = new Tree(text, Empty, Empty, false, false, Empty)
  def apply(text:String, id: String, hasChildren: Boolean) = new Tree(text, Full(id), Empty, false, true, Empty)
  def apply(text:String, classes: String) = new Tree(text, Empty, Full(classes), false, false, Empty)
  def apply(text:String, children: List[Tree]) = new Tree(text, Empty, Empty, false, false, Full(children))
  def apply(text:String, classes: String, children: List[Tree]) = new Tree(text, Empty, Full(classes), false, false, Full(children))
  def apply(text:String, classes: String, expanded: Boolean, hasChildren: Boolean, children: List[Tree]) = 
    new Tree(text, Empty, Full(classes), expanded, hasChildren, Full(children))
  def apply(text:String, id: String, classes: String, expanded: Boolean, hasChildren: Boolean, children: List[Tree]) = 
    new Tree(text, Full(id), Full(classes), expanded, hasChildren, Full(children))

  def toJSON(nodes: List[Tree]): String = nodes.map(_ toJSON).mkString("[", ", ", "]")
}

/**
 * Server side representation of a node of the tree widget
 */
case class Tree(text:String, id: Can[String], classes: Can[String], expanded: Boolean, hasChildren: Boolean, children: Can[List[Tree]]) {
  
  def toJSON: String = {
    
      "{ \"text\": \"" + text + "\"" +
        id.map(id => ", \"id\": \"" + id + "\"").openOr("") + 
        classes.map(cls => ", \"classes\": \"" + cls + "\"").openOr("") + 
        (hasChildren match { case true => ", \"hasChildren\": true" case _ =>  ""}) +  
        (expanded match { case true => ", \"expanded\": true" case _ =>  ""}) +  
        children.map(childs=> ", \"children\": " + childs.map(_ toJSON).mkString("[", ", ", "]")).openOr("") +
        " }"
  }

}

