<!--
Copyright 2009 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

  <fieldset>
    <legend>YouTube API Settings</legend>
    <div class="clear">
      <label class="configureLabel">YouTube Account:</label>
      <span id="youTubeUsername">Unauthenticated</span>
      <input id="authenticateButton" type="button" value="Authenticate"/>
    </div>
    <div class="clear">
      <label for="developerKey" class="configureLabel">YouTube Developer Key:</label>
      <input class="configureInput" id="developerKey" type="text"/>
      <img class="tip" src="/questionmark.png" title="Register at http://code.google.com/apis/youtube/dashboard/gwt"></img>
    </div>
    <div class="clear">
      <label for="privateKeyBytes" class="configureLabel">Private Key Certificate: </label>
      <textarea cols="50" rows="10" id="privateKeyBytes"></textarea>
      <img class="tip" src="/questionmark.png" title="Optional private key certificate data. Unless you're using secure AuthSub and have uploaded a security certificate to https://www.google.com/accounts/ManageDomains, leave blank."></img>
    </div>
  </fieldset>
  <fieldset>
    <legend>Video Branding Settings</legend>
    <div class="clear">
      <label for="brandingMode" class="configureLabel">Branding Mode:</label>
      <select class="configureInput" id="brandingMode">
        <option value="0" selected="selected">ON</option>
        <option value="1">OFF</option>
      </select>
      <img class="tip" src="/questionmark.png" title="ON: Add branding to approved submissions. OFF: No branding."></img>
    </div>
    <div class="clear">
      <label for="defaultTag" class="configureLabel">Default YouTube Tag:</label>
      <input class="configureInput" id="defaultTag" type="text"/>
      <img class="tip" src="/questionmark.png" title="User-visible YouTube tag added to all approved submissions, e.g. 'yourcompany'"></img>
    </div>
    <div class="clear">
      <label for="linkBackText" class="configureLabel">Link Back Text:</label>
      <input class="configureInput" id="linkBackText" type="text"/>
      <img class="tip" src="/questionmark.png" title="e.g. 'Submitted in response to article ARTICLE_URL.' If branding is on, automatically added to approved videos' YouTube descriptions. ARTICLE_URL = url of page hosting submission."></img>
    </div>
  </fieldset>
  <fieldset>
    <legend>Moderation / Submission Settings</legend>
    <div class="clear">
      <label for="moderationMode" class="configureLabel">Moderation Mode:</label>
      <select class="configureInput" id="moderationMode">
        <option value="0" selected="selected">ON</option>
        <option value="1">OFF</option>
      </select>
      <img class="tip" src="/questionmark.png" title="ON: Submissions need to be moderated. OFF: All submissions are auto-approved."></img>
    </div>
    <div class="clear">
      <label for="submissionMode" class="configureLabel">Submission Mode:</label>
      <select class="configureInput" id="submissionMode">
        <option value="0" selected="selected">NEW OR EXISTING</option>
        <option value="1">NEW ONLY</option>
      </select>
      <img class="tip" src="/questionmark.png" title="NEW OR EXISTING: Accept new and existing videos. NEW ONLY: Only accept new uploads."></img>
    </div>
    <div class="clear">
      <label for="newSubmissionAddress" class="configureLabel">Notification Address:</label>
      <input class="configureInput" id="newSubmissionAddress" type="text"/>
      <img class="tip" src="/questionmark.png" title="Email will be sent to this address whenever a video is submitted. Address must be an AppEngine admin."></img>
    </div>
    <div class="clear">
      <label for="moderationEmail" class="configureLabel">Moderation Email:</label>
      <input type="checkbox" id="moderationEmail">Email User Upon Moderation</input>
      <img class="tip" src="/questionmark.png" title="If checked, user who submitted video will receive mail when their video is approved/rejected."></img>
    </div>
    <div id="moderationEmailTextDiv">
      <div class="clear">
        <label for="fromAddress" class="configureLabel">Sender Email Address: </label>
        <input class="configureInput" id="fromAddress" type="text"/>
        <img class="tip" src="/questionmark.png" title="Address to use as sender of moderation emails. Address must be an AppEngine admin."></img>
      </div>
      <div class="clear">
        <label for="approvalEmailText" class="configureLabel">Approval Email Text: </label>
        <textarea cols="50" rows="10" id="approvalEmailText"></textarea>
        <img class="tip" src="/questionmark.png" title="Body of approval emails. ARTICLE_URL = url of page hosting submission. YOUTUBE_URL = url of video on YouTube."></img>
      </div>
      <div class="clear">
        <label for="rejectionEmailText" class="configureLabel">Rejection Email Text: </label>
        <textarea cols="50" rows="10" id="rejectionEmailText"></textarea>
        <img class="tip" src="/questionmark.png" title="Body of rejection emails. ARTICLE_URL = url of page hosting submission. YOUTUBE_URL = url of video on YouTube."></img>
      </div>
    </div>
    <div class="clear">
      You <b>must</b> <a target="_blank" href="http://code.google.com/appengine/docs/billing.html">enable billing</a> on your production App Engine instance <b>before</b> you enable photo submissions. YouTube Direct will not work otherwise.
    </div>
    <div class="clear">
      <label for="photoSubmissionEnabled" class="configureLabel">Photo Submissions:</label>
      <input type="checkbox" id="photoSubmissionEnabled">Enabled</input>
      <img class="tip" src="/questionmark.png" title="If checked, users will be able to submit photos as well as videos."></img>
    </div>
    <div id="photoSubmissionConfigDiv">
      <div class="clear">
        <label for="maxPhotoSizeMb" class="configureLabel">Max. Photo Size (MB):</label>
        <select class="configureInput" id="maxPhotoSizeMb">
          <option value="1">1</option>
          <option value="3">3</option>
          <option value="5" selected="selected">5</option>
          <option value="10">10</option>
          <option value="20">20</option>
        </select>
        <img class="tip" src="/questionmark.png" title="Photos uploaded that are larger than this number of megabytes will be ignored."></img>
      </div>
      <div class="clear">
        <label for="recaptchaPrivateKey" class="configureLabel">ReCaptcha Private Key: </label>
        <input class="configureInput" id="recaptchaPrivateKey" type="text"/>
        <img class="tip" src="/questionmark.png" title="Private key registered for an *.appspot.com domain at http://recaptcha.net"></img>
      </div>
      <div class="clear">
        <label for="recaptchaPublicKey" class="configureLabel">ReCaptcha Public Key: </label>
        <input class="configureInput" id="recaptchaPublicKey" type="text"/>
        <img class="tip" src="/questionmark.png" title="Public key registered for an *.appspot.com domain at http://recaptcha.net"></img>
      </div>
    </div>
  </fieldset>
  <fieldset>
    <legend>Submissions Widget Settings</legend>
    <div class="clear">
      <label for="loginInstruction" class="configureLabel">Initial Message:</label>
      <textarea cols="50" rows="10" id="loginInstruction"></textarea>
      <img class="tip" src="/questionmark.png" title="Presented to user on the initial submission screen. HTML is okay."></img>
    </div>
    <div class="clear">
      <label for="postSubmitMessage" class="configureLabel">Post-Submit Message:</label>
      <textarea cols="50" rows="10" id="postSubmitMessage"></textarea>
      <img class="tip" src="/questionmark.png" title="Presented to user after submitting a video. HTML is okay."></img>
    </div>
  </fieldset>
  <div class="clear">
    <input id="saveButton" type="button" value="Save Changes"/>
  </div>