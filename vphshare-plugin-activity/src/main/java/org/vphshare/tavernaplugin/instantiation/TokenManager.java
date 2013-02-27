package org.vphshare.tavernaplugin.instantiation;

import java.io.IOException;
import java.net.URI;

import net.sf.taverna.t2.security.credentialmanager.CMException;
import net.sf.taverna.t2.security.credentialmanager.CredentialManager;
import net.sf.taverna.t2.security.credentialmanager.UsernamePassword;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class TokenManager {

    private static Logger logger = Logger.getLogger(TokenManager.class);

    private static TokenManager instance;

    private String user;

    private String password;

    public UsernamePasswordCredentials tokenAuthentication = null;

    public static TokenManager getInstance() {
        if (instance == null) {
            instance = new TokenManager();
        }
        return instance;
    }

    private TokenManager() {
    }

    public synchronized boolean tokenValid() {
        if (tokenAuthentication == null) {
            return false;
        }
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, tokenAuthentication);
        String url = CloudFacadeConstants.CLOUDFACADE_URL + "/workflow/list";
        GetMethod method = new GetMethod(url);
        method.setDoAuthentication(true);
        int statusCode;
        try {
            statusCode = client.executeMethod(method);
            return statusCode == HttpStatus.SC_OK;
        } catch (HttpException e) {
            e.printStackTrace();
            logger.error("Got exception while testing credentials", e);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Got exception while testing credentials", e);
            return false;
        }
    }

    private synchronized String getToken(String user, String password) {
    	// TODO: Should be https instead of http, but the https service is currently down - ask Matteo!
        String baseUrl = "http://devauth.biomedtown.org/user_login?domain=VPHSHARE&";
        String url = baseUrl + "username=" + Workflow.urlencode(user) + "&password=" + Workflow.urlencode(password);
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(url);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK && !method.getResponseBodyAsString().equals("Error")) {
                // successfully obtained a new token
            	String token = method.getResponseBodyAsString();
            	logger.debug("Successfully obtained token \"" + token + "\"");
                return token;
            } else {
                // wrong user or password
            	logger.error("Got message \"" + method.getResponseBodyAsString() + "\" with status code "
            				+ statusCode + " when trying to obtain token!");
            	return null;
            }
        } catch (HttpException e) {
            e.printStackTrace();
            logger.error("Got exception while obtaining token", e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Got exception while obtaining token", e);
            return null;
        }
    }

    public synchronized boolean updateToken(boolean forceUpdate) {
    	do {
	        // obtain user/pass from credentials manager
	        CredentialManager credManager = null;
	        try {
	            credManager = CredentialManager.getInstance();
	        } catch (CMException e) {
	            logger.error("Could not obtain credentials manager", e);
	            tokenAuthentication = null;
	            return false; // don't retry
	        }
	        UsernamePassword username_password = null;
	        URI serviceUri = URI.create("https://devauth.biomedtown.org/");
	        try {
	            username_password = credManager.getUsernameAndPasswordForService(serviceUri, false,
	                    "Please enter your Biomedtown credentials here. "
	                            + "They are used to obtain a security token that will let you execute workflows.");
	        } catch (CMException e) {
	            logger.error("Could not get user/pass from credentials manager", e);
	            tokenAuthentication = null;
	            return false; // don't retry
	        }
	        if (username_password == null) {
	        	logger.error("User refused to enter his biomedtown credentials! Probably \"Cancel\" has been pressed.");
	            tokenAuthentication = null;
	            return false; // don't retry
	        }
	        String newUser = username_password.getUsername();
	        String newPassword = username_password.getPasswordAsString();
	
	        // check, if we have to obtain a new token
	        if (!newUser.equals(user) || !newPassword.equals(password) || forceUpdate || !tokenValid()) {
	            // obtain the new token
	            user = newUser;
	            password = newPassword;
	            if (user.length() == 0 || password.length() == 0) {
	                tokenAuthentication = null;
	            } else {
		            String token = getToken(user, password);
		            if (token != null) {
		                // obtained a valid token => remember it
		                tokenAuthentication = new UsernamePasswordCredentials("", token);
		            } else {
		                // user or password wrong - remove it from the credentials manager and invalidate old token
		            	try {
		            		if (credManager.hasUsernamePasswordForService(serviceUri)) {
		            			credManager.deleteUsernameAndPasswordForService(serviceUri.toString());
		            		}
						} catch (CMException e) {
							logger.error("Exception while trying to delete invalid user/pass from cred manager", e);
							tokenAuthentication = null;
							return false; // don't retry
						}
		                tokenAuthentication = null;
		            }
	            }
	        }
    	} while (tokenAuthentication == null);

        // Final check for token validity
        if (tokenValid()) {
            return true;
        } else {
            tokenAuthentication = null;
            return false;
        }
    }

    public synchronized boolean updateToken(String user, String password) {
        this.user = user;
        this.password = password;
        return updateToken(true);
    }

    public synchronized UsernamePasswordCredentials getTokenAuthentication() {
        return getTokenAuthentication(true);
    }

    private synchronized UsernamePasswordCredentials getTokenAuthentication(boolean checkForTokenUpdates) {
        if (checkForTokenUpdates || tokenAuthentication == null) {
            updateToken(false);
        }
        return tokenAuthentication;
    }
}
