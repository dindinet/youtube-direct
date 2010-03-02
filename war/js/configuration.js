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
admin.config = admin.config || {};

// A list of parameters that map 1:1 between the JSON parameter names and the form ids.
admin.config.BASIC_PARAMS = ['developerKey', 'clientId', 'defaultTag', 'linkBackText',
                             'moderationMode', 'newSubmissionAddress', 'brandingMode',
                             'loginInstruction', 'postSubmitMessage', 'fromAddress',
                             'approvalEmailText', 'rejectionEmailText', 'privateKeyBytes', 
                             'maxPhotoSizeMb'];

admin.config.init = function() {
  var saveButton = jQuery('#saveButton');     
  
  admin.config.getAdminConfig(function(data) {
  	jQuery.each(admin.config.BASIC_PARAMS, function(i, param) {
  		jQuery('#' + param).val(data[param]);
  	});
    
    if (data.moderationEmail) {
      jQuery('#moderationEmail').attr('checked', true);
      admin.config.toggleDiv(jQuery('#moderationEmailTextDiv'), true);
    } else {
      jQuery('#moderationEmail').attr('checked', false);
      admin.config.toggleDiv(jQuery('#moderationEmailTextDiv'), false);
    }
    
    if (data.youTubeAuthSubToken && data.youTubeUsername) {
      jQuery('#youTubeUsername').html(jQuery.sprintf(
      				'Authenticated as <a href="http://youtube.com/%s">%s</a>', data.youTubeUsername,
      				data.youTubeUsername));
      jQuery('#authenticateButton').val("Re-Authenticate");
    }
    
    if (data.photoSubmissionEnabled) {
    	jQuery('#photoSubmissionEnabled').attr('checked', true);
    	admin.config.toggleDiv(jQuery('#maxPhotoSizeMbDiv'), true);
    } else {
    	jQuery('#photoSubmissionEnabled').attr('checked', false);
    	admin.config.toggleDiv(jQuery('#maxPhotoSizeMbDiv'), false);
    }
  });
  
  saveButton.click(function() {
    admin.config.updateAdminConfig();
  });
  
  jQuery('#authenticateButton').click(function() {
    // Hardcode http:// rather than allowing for https:// to ensure that we can get by with
    // registering http://APP.appspot.com/ as the prefix for AuthSub requests in the Google
    // Manage Your Domain pages.
    var nextUrl = jQuery.sprintf('http://%s/admin/PersistAuthSubToken', window.location.host);
    window.location = jQuery.sprintf('https://www.google.com/accounts/AuthSubRequest?next=%s&scope=http%3A%2F%2Fgdata.youtube.com&session=1&secure=0', nextUrl);
  });
  
  jQuery('#moderationEmail').click(function() {
    admin.config.toggleDiv(jQuery('#moderationEmailTextDiv'));
  });
  
  jQuery('#photoSubmissionEnabled').click(function() {
    admin.config.toggleDiv(jQuery('#maxPhotoSizeMbDiv'));
  });
};

admin.config.toggleDiv = function(divObject, isVisible) {
  if (isVisible == null) {
    isVisible = divObject.css("display") == "none" ? true : false;
  }
  
  var displayStyle = isVisible ? 'block' : 'none';
  
  divObject.css("display", displayStyle);
}

admin.config.getAdminConfig = function(callback) {
  var messageElement = admin.showMessage('Loading configuration...');
  
  var command = 'GET_ADMIN_CONFIG';
  var params = {};
  
  var jsonRpcCallback = function(jsonStr) {
    try {
      var json = JSON.parse(jsonStr);
      if (!json.error) {
        admin.showMessage('Configuration loaded.', messageElement);
        callback(json.result);
      } else {
        admin.showError(json.error, messageElement);
      }
    } catch(exception) {
    	admin.showError('Request failed: ' + exception, messageElement);
    }
  } 
  
  jsonrpc.makeRequest(command, params, jsonRpcCallback);  
};

admin.config.updateAdminConfig = function() {
  var messageElement = admin.showMessage("Saving configuration...");

  var params = {};
	jQuery.each(admin.config.BASIC_PARAMS, function(i, param) {
		params[param] = jQuery('#' + param).val();
	});
  
	params.moderationEmail = jQuery('#moderationEmail').attr('checked');
	params.photoSubmissionEnabled = jQuery('#photoSubmissionEnabled').attr('checked');

  var command = 'UPDATE_ADMIN_CONFIG';

  var jsonRpcCallback = function(jsonStr) {
    try {
      var json = JSON.parse(jsonStr);
      if (!json.error) {
        admin.showMessage("Configuration saved.", messageElement);
      } else {
        admin.showError(json.error, messageElement);
      }
    } catch(exception) {
    	admin.showError('Request failed: ' + exception, messageElement);
    }
  } 

  jsonrpc.makeRequest(command, params, jsonRpcCallback);
};