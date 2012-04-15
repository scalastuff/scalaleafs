
// 
function callback(uid) {
  console.log('Callback invoked: /ajaxCallback/' + uid); // DEBUG
  $.getScript('/ajaxCallback/' + uid);
};

// Used to detect initial (useless) popstate.
// If history.state exists, assume browser isn't going to fire initial popstate.
var popped = ('state' in window.history), initialURL = location.href

window.onpopstate = function(event) {
  // Ignore initial pop state that some browsers fire on page load
  var initialPop = !popped && location.href == initialURL
  popped = true
  if ( initialPop ) return;
  console.log('Pop state: ' + window.location) // DEBUG
  setUrl("pop:" + window.location);
}	