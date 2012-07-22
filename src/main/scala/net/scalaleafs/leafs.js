/*
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
window.onpopstate = function(event) {
  // Ignore state that wasn't put by leafs (like initial pop state) // DEBUG MODE
  if (event.state != null) {
    console.log('Pop state: ' + event.state) // DEBUG MODE
    setUrl("pop:" + window.location);
  }
}  

var leafs = new function() {

  this.callback = function(callbackId) {
    console.log('Callback invoked: /$$AJAX_CALLBACK_PATH/leafs/ajaxCallback/' + callbackId); // DEBUG MODE
    $.getScript('/$$AJAX_CALLBACK_PATH/' + callbackId);
  };
  
  this.postCallback = function(callbackId, data) {
    data['action'] = callbackId;
    console.log('Callback invoked: /$$AJAX_FORMPOST_PATH with data:'); // DEBUG MODE
    console.log(data); // DEBUG MODE
    $.ajax({
        url: '/$$AJAX_FORMPOST_PATH',
        type: "POST",
        data: data,
        dataType: "script"
      });
  }
  
  this.loadJavascript(name) {
	  $.getScript(name)
  }
  this.onPageLoad = function(f) {
  $(document).ready(f)  
  };
  
  this.addClass = function(elt, cls) {
  $(elt).addClass(cls)
  };
  
  this.removeClass = function(elt, cls) {
    $(elt).removeClass(cls)
  };
  
  this.replaceHtml = function(id, html) {
    $('#' + id).replaceWith(html);
  };

  this.removeNextSiblings = function(id, idbase) {
    $('#' + id).nextAll("[id^='" + idbase + "']").remove();
  };
};
