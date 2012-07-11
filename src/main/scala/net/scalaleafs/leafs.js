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
    console.log('Callback invoked: /$$CONTEXT/leafs/ajaxCallback/' + callbackId); // DEBUG MODE
    $.getScript('/$$CONTEXT/leafs/ajaxCallback/' + callbackId);
  };
  
  this.postCallback = function(callbackId, data) {
    data['action'] = callbackId;
    console.log('Callback invoked: /$$CONTEXT/leafs/ajaxFormPost/' + callbackId + ' with data:'); // DEBUG MODE
    console.log(data); // DEBUG MODE
    $.ajax({
        url: '/$$CONTEXT/leafs/ajaxFormPost',
        type: "POST",
        data: data,
        dataType: "script"
      });
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
  
  this.FormOptions = {
    None: 0,
    UpdateOnChange: 2,
    UpdateOnKeyPress: 4,
    DoSubmit: 8,
    EnableWhenChanged: 16
  };
  
  this.UpdateMode = {
    ON_SUBMIT: 0,
    ON_CHANGE: 1,
    ON_KEY: 2
  };
  
  this.formInputInit = function(inputId, options, callbackId) {
    var input = $("#" + inputId);
    var inputAndLabel = input;
    var initialValue = input.val();
    var lastValue = initialValue;
    var currentValue = initialValue;
    var formOnChange = null;
    // Is it a form?         DEBUG MODE
    var form = $(input).closest('form');
    if (form.size() > 0) {
      form.addClass("rich-form");
      var label = form.find("label[for='" + input.attr('id') + "']");
      var inputAndLabel = input.add(label);
      
      // Create form data     DEBUG MODE
      var data = form.data("leafs_form_data");
      if (data == null) {
        data = new Object();
        form.data("leafs_form_data", data);
        data["current"] = new Object();
        data["changed"] = new Object();
        data.changesPending = false;
      }
      if (options & leafs.FormOptions.EnableWhenChanged) {
        data.disablingElements = input.add(data.disablingElements); 
        input.attr("disabled", "disabled").addClass("disabled");
      }
      // Form-based onChange maintains form-level changed flag.
      formOnChange = function() {
        if (currentValue != input.val()) {
          data.changesPending = true;
        }
        currentValue = input.val();
        data["current"][callbackId] = currentValue;
        if (initialValue != input.val()) {
          data["changed"][callbackId] = true;
          inputAndLabel.addClass("changed");
        } else {
          delete data["changed"][callbackId];
          inputAndLabel.removeClass("changed");      
        }
        if ($.isEmptyObject(data["changed"])) {
          form.removeClass("changed")
          data.disablingElements.attr("disabled", "disabled").addClass("disabled");
        }
        else {
          form.addClass("changed")
          data.disablingElements.removeAttr("disabled").removeClass("disabled");
        }
      };
      // Register submit handler.
      if (input.attr("type") == "submit") {
        input.on("click", function() {
          if (data.changesPending) {
            data.changesPending = false;
            leafs.postCallback(callbackId, data["current"]);
          }
          return false;
        });
      };
    }
    var onChange = function(doCallback) {
      currentValue = input.value;
      if (initialValue != input.val()) {
        inputAndLabel.addClass("changed");
      } else {
        inputAndLabel.removeClass("changed");      
      }
      if (formOnChange != null && input.attr("type") != "submit") {
        formOnChange();
      }
      if (doCallback) {
        if (lastValue != input.val()) {
          lastValue = input.val();
          leafs.callback(callbackId + "?value=" + lastValue);
        }
      }
    };
  
    // Set initial "empty" or "non-empty" class // DEBUG MODE
    if (input.val() == "") inputAndLabel.removeClass("non-empty").addClass("empty")
    else inputAndLabel.removeClass("empty").addClass("non-empty");
    // Set "focus" class // DEBUG MODE
    input.on("focus", function() { 
      inputAndLabel.addClass("focus"); 
    });
    // Remove "focus" class and "empty" or "non-empty" on blur // DEBUG MODE
    input.on("blur", function() {
      inputAndLabel.removeClass("focus");
      if (input.val() == "") inputAndLabel.removeClass("non-empty").addClass("empty")
      else inputAndLabel.removeClass("empty").addClass("non-empty");
    });
    // Remove label as soon as key is pressed.
    input.on("keydown", function() { 
      inputAndLabel.removeClass("empty").addClass("non-empty"); 
    });
    input.on("change", function() { 
      onChange(options & leafs.FormOptions.UpdateOnChange); 
    });
    input.on("keyup", function() {
      onChange(options & leafs.FormOptions.UpdateOnKeyPress);
    });
    if (input.attr("type") == "button") {
      input.on("click", function() {
        leafs.callback(callbackId);
        return false;
      });
    };
  };
 
  
  this.formSubmit = function(input, callbackId) {
    var data = formData(input)["current"];
    if (data != null) {
      data['action'] = callbackId;
        console.log('Callback invoked: /$$CONTEXT/leafs/ajaxFormPost with data:'); // DEBUG MODE
        console.log(data); // DEBUG MODE
        $.ajax({
            url: '/$$CONTEXT/leafs/ajaxFormPost',
            type: "POST",
            data: data,
            dataType: "script"
          });
    }
    return false;
  };
};

// Override buggy IE implementation of getElementById
if (/msie/i.test (navigator.userAgent)) //only override IE
{
  document.nativeGetElementById = document.getElementById;
  document.getElementById = function(id)
  {
    var elem = document.nativeGetElementById(id);
    if(elem)
    {
      //make sure that it is a valid match on id
      if(elem.attributes['id'].value == id)
      {
        return elem;
      }
      else
      {
        //otherwise find the correct element
        for(var i=1;i<document.all[id].length;i++)
        {
          if(document.all[id][i].attributes['id'].value == id)
          {
            return document.all[id][i];
          }
        }
      }
    }
    return null;
  };
}