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
    leafs.setLocation("pop:" + window.location);
  }
}  

var leafs = new function() {
  
  this.setLocation = function(url) {
    var setLocationCallback = window.setLocationCallback;
    if (setLocationCallback != null) {
      console.log('Set url invoked: /$$AJAX_CALLBACK_PATH/' + setLocationCallback + "?value=" + url); // DEBUG MODE
      $.getScript('/$$AJAX_CALLBACK_PATH/' + setLocationCallback + "?value=" + url);    
    } else {
      if (url.substring(0, 4) == "pop:") {
        url = url.substring(4);
      }
      window.location.href = url;
    }
  } 
  
  this.callback = function(callbackId) {
    console.log('Callback invoked: /$$AJAX_CALLBACK_PATH/' + callbackId + '/' + window.id); // DEBUG MODE
    $.getScript('/$$AJAX_CALLBACK_PATH/' + callbackId + '/' + window.id);
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
  
  this.loadJavascript = function(name) {
	  console.log('Loading script: ' + name); // DEBUG MODE
	  $.ajax({
	    url: name,
	    dataType: 'script',
	    async: false
	    });
  };
  
  this.onPageLoad = function(f) {
    $(document).ready(f)  
  };
  
  this.onPageUnload = function(callbackId) {
    console.log("UNLOAD:" + window.onunload);
    window.onunload = function() {
        $.ajax({
      url: '/$$AJAX_CALLBACK_PATH/' + callbackId + '/' + window.id,
      dataType: 'script',
      async: false
      });
    window.onunload = function() {};
    }; 
    console.log("UNLOAD:" + window.onunload);
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
