package com.google.ytd.dao;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import com.google.inject.Inject;
import com.google.ytd.model.UserAuthToken;
import com.google.ytd.util.Util;

public class UserAuthTokenDaoImpl implements UserAuthTokenDao {
  private PersistenceManagerFactory pmf = null;
  
  @Inject
  private Util util;

  @Inject
  public UserAuthTokenDaoImpl(PersistenceManagerFactory pmf) {
    this.pmf = pmf;
  }

  @SuppressWarnings("unchecked")
  @Override
  public UserAuthToken getUserAuthToken(String username) {
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      if (!util.isNullOrEmpty(username)) {
        Query query = pm.newQuery(UserAuthToken.class, "youtubeName == username");
        query.declareParameters("String username");
        List<UserAuthToken> results = (List<UserAuthToken>) query.execute(username);
        if (results.size() > 0) {
          return pm.detachCopy(results.get(0));
        }
      }
    } finally {
      pm.close();
    }
    return null;
  }
  
  @Override
  public void setUserAuthToken(String username, String token) {
    // default token type is authsub
    setUserAuthToken(username, token, UserAuthToken.TokenType.AUTH_SUB);
  }
  
  @Override
  public void setUserAuthToken(String username, String token, UserAuthToken.TokenType tokenType) {
    UserAuthToken userAuthToken = getUserAuthToken(username);
    
    if (userAuthToken == null) {
      userAuthToken = new UserAuthToken();
      userAuthToken.setYoutubeName(username);
    } 
    
    switch (tokenType) {
      case AUTH_SUB:
        userAuthToken.setAuthSubToken(token);
        break;
      case CLIENT_LOGIN:
        userAuthToken.setClientLoginToken(token);
        break;      
    }
    
    PersistenceManager pm = pmf.getPersistenceManager();
    try {
      pm.makePersistent(userAuthToken);
    } finally {
      pm.close();
    }
  }  
  
}
