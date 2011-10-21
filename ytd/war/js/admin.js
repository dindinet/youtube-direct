/* Copyright (c) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// namespace protection against collision
var admin = admin || {};

jQuery(document).ready(function() {
  var namespace = '';
  var regexResults = /ns=(\w+)/i.exec(window.location.search);
  if (regexResults != null) {
    namespace = regexResults[1];
  }
  
	if (window.isLoggedIn) {
		jQuery('#tabs').tabs();
		admin.init(namespace);
	}
});

admin.init = function(namespace) {
  admin.sub.init(namespace); // from video_submission.js	
  admin.photo.init(namespace); // from photo_submission.js   
  admin.assign.init(namespace); // from assignments.js
  admin.config.init(namespace); //from configuration.js
};

admin.showMessage = function(message, elementToHide, displaySeconds) {
  // Default timeout is 15 sec.
  displaySeconds = displaySeconds || 15;
  return admin.showSomething(message, 'message', elementToHide, displaySeconds);
};

admin.showError = function(error, elementToHide) {
  // Let's console.log() exceptions if it's available and we're on a dev server (not port 443)
  if (window.location.port != 443 && typeof error != 'string' && typeof console != 'undefined' && typeof console.log != 'undefined') {
    for (property in error) {
      console.log(jQuery.sprintf("%s: %s", property, error[property]));
    }
  }
  
  return admin.showSomething(error, 'error', elementToHide, 10);
};

admin.showSomething = function(message, elementClass, elementToHide, displaySeconds) {
  var wrapperElement = jQuery('<p>').addClass('messageListWrapper').prependTo('#messageList');
  var messageElement = jQuery('<span>' + message + '</span>').addClass(elementClass);
  messageElement.prependTo(wrapperElement);
  
  if (elementToHide) {
    jQuery(elementToHide).hide();
  }
  
  if (typeof displaySeconds == 'number') {
    setTimeout(function() {
      messageElement.fadeOut('fast');
    }, displaySeconds * 1000);
  }
  
  return wrapperElement;
};

admin.formatDate = function(date) {
  var year = admin.padZero(date.getFullYear());
  var month = admin.padZero(date.getMonth() + 1);
  var day = admin.padZero(date.getDate());
  var hours = admin.padZero(date.getHours());
  var minutes = admin.padZero(date.getMinutes());
  var seconds = admin.padZero(date.getSeconds());
  
  // Use %s to maintain zero-padding, which the jQuery.sprintf() library can't provide.
  return jQuery.sprintf('%s-%s-%s %s:%s:%s', year, month, day, hours, minutes, seconds);
};

admin.padZero = function(value) {
  value = value + '';
  if (value.length < 2) {
    return '0' + value;
  } else {
    return value;
  }
};

admin.getChannelId = function(callback) {
  if (admin.channelId != null) {
    callback(admin.channelId);
  } else {
    var messageElement = admin.showMessage("Opening communications channel...");
  
    jsonrpc.makeRequest('OPEN_CHANNEL_CONNECTION', {}, function(json) {
      try {
        if (!json.error) {
          admin.showMessage("Communications channel opened.", messageElement);
          
          var channel = new goog.appengine.Channel(json.token);
          admin.socket = channel.open();

          admin.socket.onmessage = function(message) {
            admin.showMessage(message.data);
          };
          
          admin.socket.onclose = function(message) {
            admin.channelId = null;
          };

          admin.socket.onerror = function(error) {
            admin.showError(error.description);
            admin.socket.close();
            admin.channelId = null;
          };
          
          admin.channelId = json.channelId;
          callback(json.channelId);
        } else {
          admin.showError(json.error, messageElement);  
        }
      } catch(exception) {
        admin.showError(exception, messageElement);
      }
    });
  }
}