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
      $.getScript('/$$AJAX_CALLBACK_PATH/' + window.id + '/' + setLocationCallback + "?value=" + url);    
    } else {
      if (url.substring(0, 4) == "pop:") {
        url = url.substring(4);
      }
      window.location.href = url;
    }
  } 
  
  this.callback = function(callbackId) {
    console.log('Callback invoked: /$$AJAX_CALLBACK_PATH/' + window.id + '/' + callbackId); // DEBUG MODE
    $.getScript('/$$AJAX_CALLBACK_PATH/' + window.id + '/' + callbackId);
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
    window.onunload = function() {
        $.ajax({
      url: '/$$AJAX_CALLBACK_PATH/' + window.id + '/' + callbackId,
      dataType: 'script',
      async: false
      });
    window.onunload = function() {};
    }; 
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

function initCal() {
	
	var date = new Date();
	var d = date.getDate();
	var m = date.getMonth();
	var y = date.getFullYear();
	
	var calendar = $('#calendar').fullCalendar({
		header: {
			left: 'prev,next today',
			center: 'title',
			right: 'month,agendaWeek,agendaDay'
		},
		selectable: true,
		selectHelper: true,
		select: function(start, end, allDay) {
			var title = prompt('Event Title:');
			if (title) {
				calendar.fullCalendar('renderEvent',
					{
						title: title,
						start: start,
						end: end,
						allDay: allDay
					},
					true // make the event "stick"
				);
			}
			calendar.fullCalendar('unselect');
		},
		editable: true,
		events: [
			{
				title: 'All Day Event',
				start: new Date(y, m, 1)
			},
			{
				title: 'Long Event',
				start: new Date(y, m, d-5),
				end: new Date(y, m, d-2)
			},
			{
				id: 999,
				title: 'Repeating Event',
				start: new Date(y, m, d-3, 16, 0),
				allDay: false
			},
			{
				id: 999,
				title: 'Repeating Event',
				start: new Date(y, m, d+4, 16, 0),
				allDay: false
			},
			{
				title: 'Meeting',
				start: new Date(y, m, d, 10, 30),
				allDay: false
			},
			{
				title: 'Lunch',
				start: new Date(y, m, d, 12, 0),
				end: new Date(y, m, d, 14, 0),
				allDay: false
			},
			{
				title: 'Birthday Party',
				start: new Date(y, m, d+1, 19, 0),
				end: new Date(y, m, d+1, 22, 30),
				allDay: false
			},
			{
				title: 'Click for Google',
				start: new Date(y, m, 28),
				end: new Date(y, m, 29),
				url: 'http://google.com/'
			}
		]
	});
	
};

