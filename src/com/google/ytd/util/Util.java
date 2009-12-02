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

package com.google.ytd.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.datastore.Text;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.ytd.YouTubeApiManager;
import com.google.ytd.model.AdminConfig;
import com.google.ytd.model.Assignment;
import com.google.ytd.model.VideoSubmission;
import com.google.ytd.model.VideoSubmission.ModerationStatus;

/**
 * Misc. utility methods.
 */
@Singleton
public class Util {
  private static final Logger log = Logger.getLogger(Util.class.getName());
  private static final String DATE_TIME_PATTERN = "EEE, d MMM yyyy HH:mm:ss Z";

  public final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
      .setDateFormat(DATE_TIME_PATTERN).registerTypeAdapter(Text.class, new TextToStringAdapter())
      .create();

  private PersistenceManagerFactory pmf = null;

  @Inject
  private Injector injector;

  @Inject
  public Util(PersistenceManagerFactory pmf) {
    this.pmf = pmf;
  }

  public class TextToStringAdapter implements JsonSerializer<Text>, JsonDeserializer<Text> {
    public JsonElement toJson(Text text, Type type, JsonSerializationContext context) {
      return serialize(text, type, context);
    }

    public Text fromJson(JsonElement json, Type type, JsonDeserializationContext context) {
      return deserialize(json, type, context);
    }

    public JsonElement serialize(Text text, Type type, JsonSerializationContext context) {
      return new JsonPrimitive(text.getValue());
    }

    public Text deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
      try {
        return new Text(json.getAsString());
      } catch (JsonParseException e) {
        // TODO: This is kind of a hacky way of reporting back a parse exception.
        return new Text(e.toString());
      }
    }
  }

  public Object persistJdo(Object entry) {
    PersistenceManager pm = pmf.getPersistenceManager();

    try {
      entry = pm.makePersistent(entry);
      entry = pm.detachCopy(entry);
    } finally {
      pm.close();
    }

    return entry;
  }

  public void removeJdo(Object entry) {
    PersistenceManager pm = pmf.getPersistenceManager();

    try {
      pm.deletePersistent(entry);
    } finally {
      pm.close();
    }
  }

  public void sendNewSubmissionEmail(VideoSubmission videoSubmission) {
    AdminConfig adminConfig = getAdminConfig();

    String address = adminConfig.getNewSubmissionAddress();
    if (!isNullOrEmpty(address)) {
      try {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        Message msg = new MimeMessage(session);

        msg.setFrom(new InternetAddress(address, address));
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(address, address));

        msg.setSubject(String.format("New submission for assignment id %d",
                videoSubmission.getAssignmentId()));

        msg.setText(String.format("Video %s was submitted by YouTube user %s in response to " +
                "assignment id %d.", videoSubmission.getWatchUrl(),
                videoSubmission.getYouTubeName(), videoSubmission.getAssignmentId()));

        Transport.send(msg);
      } catch (UnsupportedEncodingException e) {
        log.log(Level.WARNING, "", e);
      } catch (MessagingException e) {
        log.log(Level.WARNING, "", e);
      }
    }
  }

  public void sendNotificationEmail(VideoSubmission entry, ModerationStatus status) {
    try {
      String toAddress = entry.getNotifyEmail();
      if (isNullOrEmpty(toAddress)) {
        throw new IllegalArgumentException("No destination email address in VideoSubmission.");
      }

      AdminConfig adminConfig = getAdminConfig();

      String body;
      switch (status) {
        case APPROVED:
          body = adminConfig.getApprovalEmailText();
        break;

        case REJECTED:
          body = adminConfig.getRejectionEmailText();
        break;

        default:
          throw new IllegalArgumentException(String.format("ModerationStatus %s is not valid.",
                status.toString()));
      }
      if (isNullOrEmpty(body)) {
        throw new IllegalArgumentException("No email body found in configuration.");
      }

      String fromAddress = adminConfig.getFromAddress();
      if (isNullOrEmpty(fromAddress)) {
        throw new IllegalArgumentException("No from address found in configuration.");
      }

      body = body.replace("ARTICLE_URL", entry.getArticleUrl());
      body = body.replace("YOUTUBE_URL", entry.getWatchUrl());

      Properties props = new Properties();
      Session session = Session.getDefaultInstance(props, null);
      Message msg = new MimeMessage(session);

      msg.setFrom(new InternetAddress(fromAddress, fromAddress));
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress, toAddress));

      msg.setSubject("Your Recent Video Submission");

      msg.setText(body);

      Transport.send(msg);

      log.info(String.format("Sent %s notification email for status %s", toAddress,
              status.toString()));
    } catch(IllegalArgumentException e) {
      log.log(Level.WARNING, "", e);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.WARNING, "", e);
    } catch (MessagingException e) {
      log.log(Level.WARNING, "", e);
    }
  }

  public Assignment getAssignmentById(long id) {
    PersistenceManager pm = pmf.getPersistenceManager();

    try {
      Assignment assignment = pm.getObjectById(Assignment.class, id);
      return pm.detachCopy(assignment);
    } catch (JDOObjectNotFoundException e) {
      log.log(Level.WARNING, "", e);
      return null;
    } finally {
      pm.close();
    }
  }

  /**
   * Retrieves an Assignment from the datastore given its id.
   *
   * @param id
   *          An ID corresponding to an Assignment object in the datastore.
   * @return The Assignment object whose id is specified, or null if the id is
   *         invalid.
   */
  public Assignment getAssignmentById(String id) {
    try {
      return getAssignmentById(Long.parseLong(id));
    } catch (NumberFormatException e) {
      log.log(Level.WARNING, "", e);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public AdminConfig getAdminConfig() {
    AdminConfig adminConfig = null;

    PersistenceManager pm = pmf.getPersistenceManager();

    try {
      Query query = pm.newQuery(AdminConfig.class);
      List<AdminConfig> adminConfigs = (List<AdminConfig>) query.execute();

      if (adminConfigs.size() > 0) {
        adminConfig = pm.detachCopy(adminConfigs.get(0));
      } else {
        log.info("No admin config found in datastore.  Creating a new one.");
        adminConfig = new AdminConfig();
        pm.makePersistent(adminConfig);
        adminConfig = pm.detachCopy(adminConfig);
      }
    } catch (JDOObjectNotFoundException e) {
      // this path can only occur when there is model class errors (model binary mistmatch in store)
      log.log(Level.WARNING, "Query cannot be executed against AdminConfig model class.  " +
          "Has model class been changed?", e);
    } finally {
      pm.close();
    }

    return adminConfig;
  }

  public boolean isUploadOnly() {
    boolean uploadOnly = false;
    AdminConfig adminConfig = getAdminConfig();
    if (adminConfig.getSubmissionMode() == AdminConfig.SubmissionModeType.NEW_ONLY.ordinal()) {
      uploadOnly = true;
    }
    return uploadOnly;
  }

  public String getPostBody(HttpServletRequest req) throws IOException {
    InputStream is = req.getInputStream();

    StringBuffer body = new StringBuffer();
    String line = null;
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    while ((line = br.readLine()) != null) {
      body.append(line);
      body.append("\n");
    }
    return body.toString();
  }

  public String getSelfUrl(HttpServletRequest request) {
    StringBuffer url = new StringBuffer();

    url.append(request.getRequestURL());
    String queryString = request.getQueryString();
    if (!isNullOrEmpty(queryString)) {
      url.append("?");
      url.append(queryString);
    }

    return url.toString();
  }

  public boolean isNullOrEmpty(String input) {
    if (input == null || input.length() <= 0) {
      return true;
    } else {
      return false;
    }
  }

  public String toJson(Object o) {
    return GSON.toJson(o);
  }

  /**
   * Sorts a list and then performs a join into one large string, using the
   * delimeter specified.
   *
   * @param strings
   *          The list of strings to sort and join.
   * @param delimeter
   *          The delimeter string to insert in between each string in the list.
   * @return A string consisting of a sorted list of strings, joined with
   *         delimeter.
   */
  public String sortedJoin(List<String> strings, String delimeter) {
    Collections.sort(strings);

    StringBuffer tempBuffer = new StringBuffer();
    for (int i = 0; i < strings.size(); i++) {
      tempBuffer.append(strings.get(i));
      if (i < strings.size() - 1) {
        tempBuffer.append(delimeter);
      }
    }

    return tempBuffer.toString();
  }

  @SuppressWarnings("unchecked")
  public long getDefaultMobileAssignmentId() {
    long assignmentId = -1;
    String defaultMobileAssignmentDescription = "default mobile assignment";
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      Query query = pm.newQuery(Assignment.class);
      query.declareParameters("String defaultMobileAssignmentDescription");
      query.setFilter("description == defaultMobileAssignmentDescription");
      List<Assignment> results = (List<Assignment>)
          query.execute(defaultMobileAssignmentDescription);
      if (results.size() > 0) {
        assignmentId = results.get(0).getId();
      } else {
        // create the singleton default mobile assignment
        Assignment assignment = new Assignment();
        assignment.setCategory("News");
        assignment.setDescription(defaultMobileAssignmentDescription);
        assignment.setStatus(Assignment.AssignmentStatus.ACTIVE);
        assignment = pm.makePersistent(assignment);

        YouTubeApiManager apiManager = injector.getInstance(YouTubeApiManager.class);
        String token = getAdminConfig().getYouTubeAuthSubToken();
        if (isNullOrEmpty(token)) {
          log.warning(String.format("Could not create new playlist for assignment '%s' because no" +
              " YouTube AuthSub token was found in the config.", assignment.getDescription()));
        } else {
          apiManager.setToken(token);
          String playlistId = apiManager.createPlaylist(String.format("Playlist for Assignment #%d",
                  assignment.getId()), assignment.getDescription());
          assignment.setPlaylistId(playlistId);
          assignment = pm.makePersistent(assignment);
        }
        assignmentId = assignment.getId();
      }
    } finally {
      pm.close();
    }
    return assignmentId;
  }
}
