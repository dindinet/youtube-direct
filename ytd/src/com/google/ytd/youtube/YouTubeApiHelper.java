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

package com.google.ytd.youtube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.youtube.CaptionTrackEntry;
import com.google.gdata.data.youtube.CaptionTrackFeed;
import com.google.gdata.data.youtube.FormUploadToken;
import com.google.gdata.data.youtube.PlaylistEntry;
import com.google.gdata.data.youtube.PlaylistFeed;
import com.google.gdata.data.youtube.PlaylistLinkEntry;
import com.google.gdata.data.youtube.PlaylistLinkFeed;
import com.google.gdata.data.youtube.UserProfileEntry;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.InvalidEntryException;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;
import com.google.gdata.util.XmlBlob;
import com.google.inject.Inject;
import com.google.ytd.dao.AdminConfigDao;
import com.google.ytd.util.Util;

/**
 * Class to handle interfacing with the Google Data Java Client Library's
 * YouTube support.
 */
public class YouTubeApiHelper {
  private static final Logger log = Logger.getLogger(YouTubeApiHelper.class.getName());

  // CONSTANTS
  private static final String ENTRY_URL_FORMAT = "http://gdata.youtube.com/feeds/api/videos/%s";
  private static final String UPLOADS_URL_FORMAT = "http://gdata.youtube.com/feeds/api/"
      + "users/%s/uploads/%s";
  private static final String PLAYLIST_ENTRY_URL_FORMAT = "http://gdata.youtube.com/feeds/api/"
      + "playlists/%s";
  private static final String PLAYLIST_FEED_URL = "http://gdata.youtube.com/feeds/api/users/"
      + "default/playlists?max-results=50";
  private static final String USER_ENTRY_URL = "http://gdata.youtube.com/feeds/api/users/default";
  private static final String UPLOAD_TOKEN_URL = "http://gdata.youtube.com/action/GetUploadToken";
  private static final String MODERATION_FEED_ENTRY_URL_FORMAT = "http://gdata.youtube.com/feeds/"
      + "api/products/default/videos/%s";
  private static final String UPDATED_ENTRY_ATOM_FORMAT = "<entry xmlns='http://www.w3.org/2005/"
      + "Atom' xmlns:yt='http://gdata.youtube.com/schemas/2007'><yt:moderationStatus>%s"
      + "</yt:moderationStatus></entry>";
  private static final String MODERATION_ACCEPTED = "accepted";
  private static final String MODERATION_REJECTED = "rejected";
  private static final String CAPTION_FEED_URL_FORMAT = "http://gdata.youtube.com/feeds/api/"
      + "videos/%s/captions";
  private static final String CAPTION_FAILURE_TAG = "invalidFormat";
  private static final String UPLOADS_FEED_URL_FORMAT = "http://gdata.youtube.com/feeds/api/"
      + "users/%s/uploads?max-results=50";
  private static final String CLAIMED = "<yt:claimed"; // closing bracked omitted for robustness 

  private Util util;
  private YouTubeService service = null;
  private AdminConfigDao adminConfigDao = null;
  
  /**
   * Create a new instance of the class, initializing a YouTubeService object
   * with parameters specified in appengine-web.xml
   */
  @Inject
  public YouTubeApiHelper(AdminConfigDao adminConfigDao) {
    this.util = Util.get();
    this.adminConfigDao = adminConfigDao;

    String clientId = this.adminConfigDao.getAdminConfig().getClientId();
    String developerKey = this.adminConfigDao.getAdminConfig().getDeveloperKey();

    if (util.isNullOrEmpty(clientId)) {
      clientId = "";
      log.warning("clientId settings property is null or empty.");
    }

    if (util.isNullOrEmpty(developerKey)) {
      log.warning("developerKey settings property is null or empty.");
      service = new YouTubeService(clientId);
    } else {
      service = new YouTubeService(clientId, developerKey);
    }
  }

  public YouTubeApiHelper(String clientId) {
    this.util = Util.get();
    if (util.isNullOrEmpty(clientId)) {
      clientId = "";
      log.warning("clientId parameter is null or empty.");
    }
    service = new YouTubeService(clientId);
  }
  
  /**
   * Sets the AuthSub token to use for API requests.
   * 
   * @param token
   *          The token to use.
   */
  public void setAuthSubToken(String token) {
    PrivateKey privateKey = adminConfigDao.getPrivateKey();
    if (privateKey == null) {
      service.setAuthSubToken(token);
    } else {
      service.setAuthSubToken(token, privateKey);
    }
  }

  /**
   * Sets the AuthSub token to use for API requests.
   * 
   * @param token
   *          The token to use.
   */
  public void setClientLoginToken(String token) {
    service.setUserToken(token);
  }  
  
  /**
   * Sets an arbitrary header for all outgoing requests using this service
   * instance.
   * 
   * @param header
   *          The name of the header.
   * @param value
   *          The header's value.
   */
  public void setHeader(String header, String value) {
    service.getRequestFactory().setHeader(header, value);
  }

  /**
   * Gets the username for the authenticated user, assumes that setToken() has
   * already been called to provide authentication.
   * 
   * @return The current username for the authenticated user.
   * @throws ServiceException
   * @throws IOException
   */
  public String getCurrentUsername() throws IOException, ServiceException {
    try {
      UserProfileEntry profile = service.getEntry(new URL(USER_ENTRY_URL), UserProfileEntry.class);
      return profile.getUsername();
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (AuthenticationException e) {
      if (e.getResponseBody().contains("NoLinkedYouTubeAccount")) {
        throw new IllegalArgumentException("Your account is not linked to a YouTube account. " +
        		"Please visit https://www.youtube.com/create_channel to link to a YouTube " +
        		"account, and try again.");
      } else {
        throw(e);
      }
    }

    return null;
  }

  /**
   * Sets the value for video moderation, which controls whether partner
   * branding shows up on the video's YouTube.com watch page.
   * 
   * This request needs to be made with the authorization of the account that
   * owns the developr token used to originally upload the video. Also, the
   * video must have at least one developer tag set at the time it was uploaded.
   * 
   * @param videoId
   *          The YouTube id of the video to moderate.
   * @param isApproved
   *          true if this video is approved, and false if not.
   */
  public void updateModeration(String videoId, boolean isApproved) {
    log.info(String.format("Setting moderation of video id '%s' to '%s'.", videoId, isApproved));

    String entryUrl = String.format(MODERATION_FEED_ENTRY_URL_FORMAT, videoId);
    String updatedEntry;
    if (isApproved) {
      updatedEntry = String.format(UPDATED_ENTRY_ATOM_FORMAT, MODERATION_ACCEPTED);
    } else {
      updatedEntry = String.format(UPDATED_ENTRY_ATOM_FORMAT, MODERATION_REJECTED);
    }

    try {
      GDataRequest request = service.createUpdateRequest(new URL(entryUrl));
      request.getRequestStream().write(updatedEntry.getBytes());
      request.execute();
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    }
  }

  /**
   * Submits video metadata to YouTube to get an upload token and URL.
   * 
   * @param newEntry
   *          The VideoEntry containing all video metadata for the upload
   * @return A FormUploadToken used when uploading a video to YouTube.
   */
  public FormUploadToken getFormUploadToken(VideoEntry newEntry) {
    try {
      URL uploadUrl = new URL(UPLOAD_TOKEN_URL);
      return service.getFormUploadToken(uploadUrl, newEntry);
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    }

    return null;
  }

  public String generateVideoEntryUrl(String videoId) {
    return String.format(ENTRY_URL_FORMAT, videoId);
  }

  public String generateUploadsVideoEntryUrl(String videoId) {
    return generateUploadsVideoEntryUrl("default", videoId);
  }

  public String generateUploadsVideoEntryUrl(String username, String videoId) {
    return String.format(UPLOADS_URL_FORMAT, username, videoId);
  }

  public VideoEntry getUploadsVideoEntry(String videoId) {
    String entryUrl = generateUploadsVideoEntryUrl(videoId);

    return makeVideoEntryRequest(entryUrl);
  }

  public VideoEntry getUploadsVideoEntry(String username, String videoId) {
    String entryUrl = generateUploadsVideoEntryUrl(username, videoId);

    return makeVideoEntryRequest(entryUrl);
  }

  public VideoEntry getVideoEntry(String videoId) {
    String entryUrl = generateVideoEntryUrl(videoId);

    return makeVideoEntryRequest(entryUrl);
  }

  public VideoFeed getUploadsFeed(String username) {
    String url = String.format(UPLOADS_FEED_URL_FORMAT, username);
    try {
      return service.getFeed(new URL(url), VideoFeed.class);
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    }

    return null;
  }

  public Map<String, String> getCaptions(String videoId) {
    String feedUrl = String.format(CAPTION_FEED_URL_FORMAT, videoId);
    try {
      CaptionTrackFeed captionTrackFeed = service.getFeed(new URL(feedUrl), CaptionTrackFeed.class);

      HashMap<String, String> languageToUrl = new HashMap<String, String>();
      for (CaptionTrackEntry captionTrackEntry : captionTrackFeed.getEntries()) {
        String languageCode = captionTrackEntry.getLanguageCode();
        Link link = captionTrackEntry.getLink("edit-media", "application/vnd.youtube.timedtext");
        if (link != null) {
          languageToUrl.put(languageCode, link.getHref());
        }
      }

      return languageToUrl;
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    }

    return null;
  }

  public String getCaptionTrack(String url) {
    try {
      GDataRequest request = service.createRequest(RequestType.QUERY, new URL(url),
          ContentType.TEXT_PLAIN);
      request.execute();

      BufferedReader reader = new BufferedReader(new InputStreamReader(request.getResponseStream(),
          "UTF-8"));
      StringBuilder builder = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        builder.append(line).append("\n");
      }

      return builder.toString();
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    }

    return null;
  }

  /**
   * Creates or updates a video caption track. Explicitly throw exceptions so
   * that the calling code knows whether a failure occurred due to a YouTube API
   * issue or due to a bad captions track.
   * 
   * @param videoId
   *          The video id of the YouTube video to update.
   * @param captionTrack
   *          The UTF-8 caption track data.
   * @return true if the caption track update was successful; false otherwise.
   * @throws MalformedURLException
   * @throws IOException
   * @throws ServiceException
   */
  public boolean updateCaptionTrack(String videoId, String captionTrack)
      throws MalformedURLException, IOException, ServiceException {
    String captionsUrl = String.format(CAPTION_FEED_URL_FORMAT, videoId);

    GDataRequest request = service.createInsertRequest(new URL(captionsUrl));
    request.getRequestStream().write(captionTrack.getBytes("UTF-8"));
    request.execute();

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request
        .getResponseStream()));
    StringBuilder builder = new StringBuilder();
    String line = null;

    while ((line = bufferedReader.readLine()) != null) {
      builder.append(line + "\n");
    }

    bufferedReader.close();

    String responseBody = builder.toString();
    log.info("Response to captions request: " + responseBody);
    if (responseBody.contains(CAPTION_FAILURE_TAG)) {
      return false;
    }

    return true;
  }

  /**
   * Gets a YouTube video entry given a specific video id. Constructs the entry
   * URL based on a hardcoded URL prefix, which might need to be changed in the
   * future.
   * 
   * @param entryUrl
   *          A URL string representing a GData video entity.
   * @return A VideoEntry representing the video in question, or null.
   */
  public VideoEntry makeVideoEntryRequest(String entryUrl) {
    try {
      return service.getEntry(new URL(entryUrl), VideoEntry.class);
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      // This may be thrown if the video is not found, i.e. because it is not done processing.
      // We don't need to log it at WARNING level.
      // TODO: Propogate AuthenticationExceptions so the calling code can invalidate the token.
      log.log(Level.INFO, "", e);
    }

    log.info(String.format("Couldn't get video entry from %s.", entryUrl));
    return null;
  }

  public PlaylistEntry getVideoInPlaylist(String playlistId, String videoId) {
    String playlistUrl = getPlaylistFeedUrl(playlistId);

    try {
      while (playlistUrl != null) {
        PlaylistFeed playlistFeed = service.getFeed(new URL(playlistUrl), PlaylistFeed.class);
        
        Link nextLink = playlistFeed.getNextLink();
        if (nextLink == null) {
          playlistUrl = null;
        } else {
          playlistUrl = nextLink.getHref();
        }
        
        for (PlaylistEntry playlistEntry : playlistFeed.getEntries()) {
          if (playlistEntry.getMediaGroup().getVideoId().equals(videoId)) {
            return playlistEntry;
          }
        }
      }
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      // TODO: Propogate AuthenticationExceptions so the calling code can
      // invalidate the token.
      log.log(Level.WARNING, "", e);
    }

    return null;
  }
  
  public boolean insertVideoIntoPlaylist(String playlistId, String videoId) {
    return insertVideoIntoPlaylist(playlistId, videoId, true);
  }

  public boolean insertVideoIntoPlaylist(String playlistId, String videoId, boolean retry) {
    log.info(String.format("Attempting to insert video id '%s' into playlist id '%s'...",
        videoId, playlistId));
    PlaylistEntry playlistEntry = new PlaylistEntry();
    playlistEntry.setId(videoId);

    if (getVideoInPlaylist(playlistId, videoId) != null) {
      log.warning(String.format("Video id '%s' is already in playlist id '%s'.", videoId,
          playlistId));
      // Return true here, so that the video is flagged as being in the playlist.
      return true;
    }
    
    // As of Aug 2011, it's now possible to set the playlist position at insertion time.
    playlistEntry.setPosition(0);

    try {
      playlistEntry = service.insert(new URL(getPlaylistFeedUrl(playlistId)), playlistEntry);
      log.info(String.format("Inserted video id '%s' into playlist id '%s' at position 1.",
        videoId, playlistId));
      return true;
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceForbiddenException e) {
      log.log(Level.INFO, "Maximum size of playlist reached.", e);

      if (retry) {
        log.info("Removing oldest entry from playlist and retrying...");

        PlaylistEntry lastVideo = getLastVideoInPlaylist(playlistId);
        if (lastVideo != null) {
          try {
            lastVideo.delete();
            log.info("Last entry removed.");

            return insertVideoIntoPlaylist(playlistId, videoId, false);
          } catch (IOException innerEx) {
            log.log(Level.WARNING, "", innerEx);
          } catch (ServiceException innerEx) {
            log.log(Level.WARNING, "", innerEx);
          } catch (UnsupportedOperationException innerEx) {
            log.log(Level.WARNING, "", innerEx);
          }
        }
      }
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    }

    return false;
  }
  
  public PlaylistEntry getLastVideoInPlaylist(String playlistId) {
    String playlistUrl = getPlaylistFeedUrl(playlistId);

    try {
      PlaylistFeed playlistFeed = null;
      
      while (playlistUrl != null) {
        playlistFeed = service.getFeed(new URL(playlistUrl), PlaylistFeed.class);
        Link nextLink = playlistFeed.getNextLink();
        
        if (nextLink == null) {
          playlistUrl = null;
        } else {
          playlistUrl = nextLink.getHref();
        }
      }
      
      if (playlistFeed != null) {
        List<PlaylistEntry> entries = playlistFeed.getEntries();
        return entries.get(entries.size() - 1);
      }
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    }

    return null;
  }

  public boolean removeVideoFromPlaylist(String playlistId, String videoId) {
    try {
      PlaylistEntry playlistEntry = getVideoInPlaylist(playlistId, videoId);

      if (playlistEntry == null) {
        log.warning(String.format("Could not find video id '%s' in playlist id '%s'.", videoId,
            playlistId));
        return false;
      } else {
        playlistEntry.delete();

        log.info(String.format("Removed video '%s' from playlist id '%s'.", videoId, playlistId));

        return true;
      }
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      // TODO: Propogate AuthenticationExceptions so the calling code can
      // invalidate the token.
      log.log(Level.WARNING, "", e);
    }

    return false;
  }

  public String getPlaylistFeedUrl(String playlistId) {
    return String.format(PLAYLIST_ENTRY_URL_FORMAT, playlistId);
  }
  
  public List<PlaylistLinkEntry> getDefaulUsersPlaylists() {
    ArrayList<PlaylistLinkEntry> playlistEntries = new ArrayList<PlaylistLinkEntry>();

    try {
      URL feedUrl = new URL(PLAYLIST_FEED_URL);

      while (feedUrl != null) {
        PlaylistLinkFeed playlistFeed = service.getFeed(feedUrl, PlaylistLinkFeed.class);
        playlistEntries.addAll(playlistFeed.getEntries());
        
        Link nextLink = playlistFeed.getNextLink();
        if (nextLink == null) {
          feedUrl = null;
        } else {
          feedUrl = new URL(nextLink.getHref());
        }
      }
      
      return playlistEntries;
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    } catch (ServiceException e) {
      log.log(Level.WARNING, "", e);
    }

    return null;
  }

  public String createPlaylist(String title, String description) throws ServiceException {
    PlaylistLinkEntry newEntry = new PlaylistLinkEntry();
    newEntry.setTitle(new PlainTextConstruct(title));
    newEntry.setSummary(new PlainTextConstruct(description));

    try {
      PlaylistLinkEntry createdEntry;
      
      try {
        createdEntry = service.insert(new URL(PLAYLIST_FEED_URL), newEntry);
      } catch(InvalidEntryException e) {
        // If the first attempt to create the playlist fails with this exception,
        // it's most likely due to a duplicate playlist title.
        // So let's make the title unique and try again.
        String newTitle = title + " - " + DateTime.now().toUiString();
        log.info(String.format("Playlist with title '%s' already exists. Attempting to create " +
        		"playlist with title '%s'.", title, newTitle));

        newEntry.setTitle(new PlainTextConstruct(newTitle));
        
        createdEntry = service.insert(new URL(PLAYLIST_FEED_URL), newEntry);
      }
      
      String id = createdEntry.getPlaylistId();

      log.info(String.format("Created new playlist with id '%s'.", id));
      return id;
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, "", e);
    } catch (IOException e) {
      log.log(Level.WARNING, "", e);
    }

    return null;
  }

  /**
   * Check to see if yt:claimed element is present. Since the entry
   * class does not include an accessor method yet, we keep the code here.
   * @param videoEntry the entry to inspect
   * @return <code>true</code> if claimed can be found
   */
  public boolean isClaimed(VideoEntry videoEntry) {
    // currently the element is only available in the extensions blob as an unrecognized extension
    // TODO - jarekw@ , once the client library supports the extension, this code needs to change
    XmlBlob xmlBlob = videoEntry.getXmlBlob();
    String text = xmlBlob.getFullText();
    return text != null && text.contains(CLAIMED);
  }
}