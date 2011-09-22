<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="com.google.inject.Guice"%>
<%@ page import="com.google.inject.Injector"%>
<%@ page import="com.google.inject.AbstractModule"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="com.google.ytd.embed.Authenticator"%>
<%@ page import="com.google.ytd.embed.UserSessionManager"%>
<%@ page import="com.google.ytd.dao.UserAuthTokenDao"%>
<%@ page import="com.google.ytd.dao.UserAuthTokenDaoImpl"%>
<%@ page import="com.google.ytd.dao.AdminConfigDao"%>
<%@ page import="com.google.ytd.dao.AdminConfigDaoImpl"%>
<%@ page import="com.google.ytd.dao.AssignmentDao"%>
<%@ page import="com.google.ytd.dao.AssignmentDaoImpl"%>
<%@ page import="com.google.ytd.dao.DataChunkDao"%>
<%@ page import="com.google.ytd.dao.DataChunkDaoImpl"%>
<%@ page import="com.google.ytd.util.Util"%>
<%@ page import="com.google.ytd.model.Assignment"%>
<%@ page import="com.google.ytd.model.AdminConfig"%>
<%@ page import="java.net.URLDecoder"%>
<%@ page import="javax.jdo.PersistenceManagerFactory"%>
<%@ page import="javax.servlet.http.HttpServletRequest"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>


<%	
	final HttpServletRequest req = request;
	final HttpServletResponse resp = response;

	Injector injector = Guice.createInjector(
	    new AbstractModule() {
	      protected void configure() {
		      bind(PersistenceManagerFactory.class).toInstance(
		          (PersistenceManagerFactory) getServletContext().getAttribute("pmf"));
		      bind(HttpServletRequest.class).toInstance(req);
		      bind(HttpServletResponse.class).toInstance(resp);
		      bind(BlobstoreService.class).toInstance(BlobstoreServiceFactory.getBlobstoreService());
	        bind(AdminConfigDao.class).to(AdminConfigDaoImpl.class);
	        bind(AssignmentDao.class).to(AssignmentDaoImpl.class);
		      bind(UserAuthTokenDao.class).to(UserAuthTokenDaoImpl.class);
		      bind(DataChunkDao.class).to(DataChunkDaoImpl.class);
	      }
	    });
	
	AssignmentDao assignmentDao = injector.getInstance(AssignmentDao.class);
	AdminConfigDao adminConfigDao = injector.getInstance(AdminConfigDao.class);
	AdminConfig adminConfig = adminConfigDao.getAdminConfig();
	Util util = injector.getInstance(Util.class);
	UserSessionManager userSessionManager = injector.getInstance(UserSessionManager.class);
	Authenticator authenticator = injector.getInstance(Authenticator.class);
	BlobstoreService blobstoreService = injector.getInstance(BlobstoreService.class);
	
	String assignmentId = request.getParameter("assignmentId");
	String potentiallyEmptyVideoSelectElement = "";
	if (util.isNullOrEmpty(assignmentId) || assignmentId.equals("undefined")) {
	 assignmentId = "undefined";
	 StringBuffer videoOptions = new StringBuffer();

	 for (Assignment assignment : assignmentDao.getActiveVideoAssignments()) {
	   videoOptions.append("<option value='" + assignment.getId().toString() + "'>" + assignment.getDescription() + "</option>");
	 }
	 
	 potentiallyEmptyVideoSelectElement = String.format("<label for='assignmentId'>Choose a Topic: </label><select id='assignmentId' name='assignmentId'>%s</select><br><br>", videoOptions.toString());
	}
	
	// This will default to true if the assignmentId parameter is missing or set to "undefined".
	boolean photosEnabledForAssignment = assignmentDao.isAssignmentPhotoEnabled(assignmentId);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" 
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
<meta HTTP-EQUIV="CACHE-CONTROL" CONTENT="NO-CACHE">
<title>YouTube Direct</title>

<link type="text/css" href="/css/ext/themes/redmond/jquery-ui-1.8.16.custom.css" rel="stylesheet" />
<link type="text/css" href="/css/embed.css" rel="stylesheet" />

<script type="text/javascript" src="/js/ext/jquery-1.6.2.min.js"></script>
<script type="text/javascript" src="/js/ext/json2.js"></script>
<script type="text/javascript" src="/js/ext/jquery-ui-1.8.16.custom.min.js"></script>
<script type="text/javascript" src="/js/jsonrpc.js"></script>
<script type="text/javascript" src="/js/embed.js"></script>

<script type="text/javascript" src="//maps.google.com/maps/api/js?sensor=false"></script>

</head>

<body>

<span id="youTubeLogo"><img src="/images/icon.png"/></span>

<div align="center">
<div id="main">
<%
	if (authenticator.isLoggedIn()) {		
%> <span id="youTubeName"><%= authenticator.getUserSession().getMetaData("youTubeName") %></span>
[ <a href="<%=authenticator.getLogOutUrl()%>">logout</a> ] 
<script type="text/javascript">
	window.isLoggedIn = true;
</script>
	
<%
	}
%>
<br>
<div id="message"></div> 
<br>

<div id="processing"></div>

<div align="center">
	<div id="loginInstruction">			
	<%= adminConfigDao.getLoginInstruction(assignmentId) %>
	<br><br>
		<%
			if (authenticator.isLoggedIn()) {		
		%>
  <input id="uploadVideoButton" class="askButton" type="button" value="Upload a New Video" />
      <%    
        if (!adminConfigDao.isUploadOnly()) {   
      %>  
  <br><br>
  <input id="existingVideoButton" class="askButton" type="button" value="Submit an Existing Video" /> 
      <%
        }
      %>
		<%
		  } else {
		%>
  <input onclick="javascript:top.location='<%=authenticator.getLogInUrl()%>';" class="askButton" type="button" value="Login to YouTube" />
		<%
			}	
		%>
	  <%
	    if (adminConfigDao.allowPhotoSubmission() && photosEnabledForAssignment) {
	  %>
  <br><br>
  <input id="photoButton" class="askButton" type="button" value="Submit Photo(s)" /> 
    <%
      }
    %>
  </div>
</div>

<div align="center">
  <div id="postSubmitMessage" style="display: none;">      
    <%= adminConfigDao.getPostSubmitMessage(assignmentId) %>   
  </div>
</div>

<div id="existingVideoMain" style="display: none;">
  <%=potentiallyEmptyVideoSelectElement%>
	<div class="tip">Select a video below, or paste a YouTube video URL.</div>
	<div id="loadingVideos">Loading your most recent videos...</div>
	<div id="existingVideos" style="display: none;">
		<div>
			<select id="videosSelect"><option value="dummy">Select a Video...</option></select>
		</div>
		<div style="margin-top: 10px;">
			<img id="thumbnail" style="display: none;">
			<span id="existingVideoDescription"></span>
		</div>
	</div>
	<div style="clear: both; padding: 5px;"></div>
	<label class="required" for="videoUrl1">Video URL:</label> 
  <br>
  <div><input class="inputBox" type="text" name="videoUrl" id="videoUrl1" /></div>
  <a href="#" id="addAnotherVideo">add another video</a>
  <br>
	<label for="date">Date:</label>
	<br>
	<div><input class="inputBox" type="text" name="date" id="submitDate" /></div>
	<br>	
	<label for="location">Location:</label>
	<br>
	<div><input class="inputBox" type="text" name="location" id="submitLocation" /></div>
	<br>
  <label for="phoneNumber">Phone Number:</label>
  <br>
  <div><input class="inputBox" type="text" name="phoneNumber" id="phoneNumber" /></div>
  <br>  		
	<label>Email me on approval: </label><input id="submitEmailAsk" type="checkbox" />
	<input class="emailInputBox" id="submitEmail" type="text" value=""/>
	<br>
	<br>
	<div align="center">
		<input id="submitButton" class="actionButton" type="button" value="Submit" />&nbsp;
		<input id="cancelSubmitButton" class="actionButton" type="button" value="Cancel" />
	</div>
</div>

<div id="uploaderMain" style="display: none;">
  <%=potentiallyEmptyVideoSelectElement%>
	<label class="required" for="title">Video Title:</label>
	<br>
	<div><input class="inputBox" type="text" name="title" id="title" /></div>
	<br>
	<label class="required" for="description">Video Description:</label>
	<br>
	<div><textarea class="inputBox" name="description" id="description"></textarea></div>
	<br>
	<label class="required" for="tags">Tags:</label>&nbsp;<span class="small">(use "," to separate)</span>
	<br>
	<div><input class="inputBox" type="text" name="tags" id="tags" /></div>
	<br>
	<label for="date">Date:</label>
	<br>
	<div><input class="inputBox" type="text" name="date" id="uploadDate" /></div>
	<br> 
	<label for="location">Location:</label>
	<br>
	<div><input class="inputBox" type="text" name="location" id="uploadLocation" /></div>
	<br>
  <label for="uploadPhoneNumber">Phone Number:</label>
  <br>
  <div><input class="inputBox" type="text" name="uploadPhoneNumber" id="uploadPhoneNumber" /></div>
  <br>		
	<label>Email me on approval: </label><input id="uploadEmailAsk" type="checkbox" />
	<input class="emailInputBox" id="uploadEmail" type="text" value=""/>
	<br><br> 
	<form id="uploadForm" action="" method="post" enctype="multipart/form-data"> 
	<label class="required" for="file">Select file: </label><input id="file" type="file" name="file" />
	<br>
	<br>
	<div align="center">		
		<input id="token" type="hidden" name="token" value="">
		<input id="uploadButton" class="actionButton" type="submit" value="Upload" />&nbsp;
		<input id="cancelUploadButton" class="actionButton" type="button" value="Cancel" />
	</div>
	</form>
	<br>
  <div id="youTubeTOS">
    By clicking 'Upload,' you certify that you own all rights to the content or that you are
    authorized by the owner to make the content publicly available on YouTube, and that it otherwise
    complies with the YouTube Terms of Service located at
    <a href="http://www.youtube.com/t/terms">http://www.youtube.com/t/terms</a>.
  </div>
</div>

  <%    
    if (adminConfigDao.allowPhotoSubmission() && photosEnabledForAssignment) {
  %>
<div id="photoMain" style="display: none;">
  <form id="photoUploadForm" action="<%= blobstoreService.createUploadUrl("/SubmitPhoto") %>" method="post" enctype="multipart/form-data">

  <%
    if (assignmentId.equals("undefined")) {
  %>
    <label for="assignmentId">Choose a Topic: </label>
    <select id="assignmentId" name="assignmentId">
  <%
    for (Assignment assignment : assignmentDao.getActivePhotoAssignments()) {
  %>
      <option value="<%=assignment.getId()%>"><%=assignment.getDescription()%></option>
  <%
    }
  %>
    </select>
    <br>
    <br>
  <%
    }
  %>
  
    <label class="required" for="title">Photo Title:</label>
    <br>
    <div>
      <input class="inputBox" type="text" name="title" id="title" />
    </div>
    <br>
    <label class="required" for="description">Photo Description:</label>
    <br>
    <div>
      <textarea class="inputBox" name="description" id="description"></textarea>
    </div>
    <br>
    <label for="photoDate">Date:</label>
    <br>
    <div>
      <input class="inputBox" type="text" name="date" id="photoDate" />
    </div>
    <br> 
    <label for="location">Location:</label>
    <br>
    <div>
      <input class="inputBox" type="text" name="location" id="uploadLocation" />
    </div>
    <br>
    <label class="required" for="author">Your Name (for photo credit):</label>
    <br>
    <div>
      <input class="inputBox" type="text" name="author" id="author" />
    </div>        
    <br>        
    <label class="required" for="uploadEmail">Your Email: </label>
    <div>
      <input class="inputBox" id="uploadEmail" name="uploadEmail" type="text" />
    </div>    
    <br>
    <label for="phoneNumber">Phone Number:</label>
    <br>
    <div>
      <input class="inputBox" type="text" name="phoneNumber" id="phoneNumber" />
    </div>
    
    <input id="latitude" name="latitude" type="hidden" value=""/>
    <input id="longitude" name="longitude" type="hidden" value=""/>

    <%
      if (!assignmentId.equals("undefined")) {
    %>
    <input id="assignmentId" name="assignmentId" type="hidden" value="<%=assignmentId%>"/>
    <%
      }
    %>
    
    <input id="articleUrl" name="articleUrl" type="hidden" value="<%=request.getParameter("articleUrl")%>"/>
    <script type="text/javascript" src="http://api.recaptcha.net/challenge?k=<%= adminConfig.getRecaptchaPublicKey() %>"></script>
    <br>
    <div align="center">
      <input id="uploadButton" class="actionButton" type="submit" value="Upload" />
      <input id="cancelUploadButton" class="actionButton" type="button" value="Cancel" />
    </div>
  </form>
  <br>
  <div id="picasaTOS">
    By clicking "Upload," you represent that this content complies with Picasa Web Albums Program Policies
    located at <a href="http://picasa.google.com/web/policy.html"target="_blank">http://picasa.google.com/web/policy.html</a>, and
    that you own all copyrights in this content or have authorization to upload it.
  </div>
</div>
  <%
    }
  %>
</div>
</div>
</body>

</html>