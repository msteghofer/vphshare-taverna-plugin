package org.vphshare.tavernaplugin.instantiation;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.*;

public class Workflow {

    public static class WorkflowException extends Exception {
        private static final long serialVersionUID = 1L;

        private WorkflowException(String description, Throwable exception) {
            super(description, exception);
        }

        private WorkflowException(String description) {
            super(description);
        }
    }
    
    private static class PathInfo {
        private String atomicServiceId;
        private String redirectionName;
    }

    private static Logger logger = Logger.getLogger(Workflow.class);

    private Set<Object> atomicServiceUsers;

    private TreeMap<String, AtomicServiceInstance> sharableAtomicServices;

    private Credentials credentials;

    // Taverna workflow ID
    private String tavernaId;

    // Workflow cloud facade ID
    private String cloudFacadeId;

    public Workflow(String tavernaId, Credentials credentials) {
        init(tavernaId, credentials, null);
    }

    public Workflow(String tavernaId, Credentials credentials, String cloudFacadeId) {
        init(tavernaId, credentials, cloudFacadeId);
    }

    private void init(String tavernaId, Credentials credentials, String cloudFacadeId) {
        this.tavernaId = tavernaId;
        this.credentials = credentials;
        this.cloudFacadeId = cloudFacadeId;
        this.atomicServiceUsers = new TreeSet<Object>();
        this.sharableAtomicServices = new TreeMap<String, AtomicServiceInstance>();
    }

    public Map<String, String> status() throws WorkflowException {
        // Create an instance of HttpClient
        HttpClient client = new HttpClient();

        // Create a method instance
        String url = CloudFacadeConstants.CLOUDFACADE_URL + "/workflow/" + cloudFacadeId;
        GetMethod method = new GetMethod(url);

        // Authentication
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, credentials);

        WorkflowException ex = null;
        Map<String, String> asStatusMap = new TreeMap<String, String>();
        try {
            // Execute the method
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                String message = "Method failed: " + method.getStatusLine() + "\n" + method.getResponseBodyAsString();
                logger.error(message);
                ex = new WorkflowException(message);
            }
            String jsonResponse = method.getResponseBodyAsString();
            JSONObject jsonObject = JSONObject.fromObject(jsonResponse);
            List<Object> asInstances = WorkflowRegister.propertyToList(jsonObject, "atomicServiceInstances");
            for (Object asInstance : asInstances) {
                // TODO: Depending on how the issue with the missing asInstanceId is resolved, we should use an
                // instance specific id (this.asInstanceId or this.asInstanceName) for the asStatusMap. But at
                // the moment we have neither: We don't know asInstanceId  because the HTTP method that adds
                // the AS to the workflow doesn't return it. And we cannot use asInstanceName because the
                // structure the is returned by the workflow status service doesn't contain the asInstanceName
                // that we chose when we added the AS to the workflow.
                // So right now we have to use the "atomicServiceId" - but this way we cannot
                // distinguish 2 instances of the same AS :-(
                String asInstanceName = (String) PropertyUtils.getProperty(asInstance, "atomicServiceId");
                
                String asInstanceStatus = (String) PropertyUtils.getProperty(asInstance, "status");
                asStatusMap.put(asInstanceName, asInstanceStatus);
            }
        } catch (HttpException e) {
            String message = "HttpException while asking for workflow status";
            logger.error(message, e);
            ex = new WorkflowException(message);
        } catch (IOException e) {
            String message = "IOException while asking for workflow status";
            logger.error(message, e);
            ex = new WorkflowException(message);
        } catch (IllegalAccessException e) {
            String message = "IllegalAccessException while masking for workflow status";
            logger.error(message, e);
            ex = new WorkflowException(message);
        } catch (InvocationTargetException e) {
            String message = "InvocationTargetException while asking for workflow status";
            logger.error(message, e);
            ex = new WorkflowException(message);
        } catch (NoSuchMethodException e) {
            String message = "NoSuchMethodException while asking for workflow status";
            logger.error(message, e);
            ex = new WorkflowException(message);
        } finally {
            // Release the connection
            method.releaseConnection();
        }
        if (ex != null) {
            throw ex;
        }
        return asStatusMap;
    }

    public void deleteWorkflow() throws WorkflowException {
        logger.info("deleteWorkflow");
        if (cloudFacadeId != null) {
            // Create an instance of HttpClient
            HttpClient client = new HttpClient();

            // Create a method instance
            String url = CloudFacadeConstants.CLOUDFACADE_URL + "/workflow/" + cloudFacadeId;
            DeleteMethod method = new DeleteMethod(url);

            // Authentication
            client.getParams().setAuthenticationPreemptive(true);
            client.getState().setCredentials(AuthScope.ANY, credentials);

            WorkflowException ex = null;
            try {
                // Execute the method
                int statusCode = client.executeMethod(method);
                if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NO_CONTENT) {
                    String message = "Method failed: " + method.getStatusLine() + "\n"
                            + method.getResponseBodyAsString();
                    logger.error(message);
                    ex = new WorkflowException(message);
                }
            } catch (HttpException e) {
                String message = "HttpException while deleting workflow";
                logger.error(message, e);
                ex = new WorkflowException(message);
            } catch (IOException e) {
                String message = "IOException while deleting workflow";
                logger.error(message, e);
                ex = new WorkflowException(message);
            } finally {
                // Release the connection
                method.releaseConnection();
            }
            if (ex != null) {
                throw ex;
            }
        }
    }

    public static String urlencode(String raw) {
        StringBuffer encoded = new StringBuffer();
        for (char character : raw.toCharArray()) {
            if ('a' <= character && character <= 'z') {
                encoded.append(character);
            } else if ('A' <= character && character <= 'Z') {
                encoded.append(character);
            } else if ('0' <= character && character <= '9') {
                encoded.append(character);
            } else {
                encoded.append("%");
                int number = character;
                encoded.append(String.format("%2x", number));
            }
        }
        return encoded.toString();
    }

    public void start() throws WorkflowException {
        logger.info("startWorkflow");
        if (cloudFacadeId != null) {
            // Already started; silently do nothing
            return;
        }

        // Create an instance of HttpClient.
        HttpClient client = new HttpClient();

        // Authentication
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, credentials);

        // Create a method instance.
        String url = CloudFacadeConstants.CLOUDFACADE_URL + "/workflow/start";
        PostMethod method = new PostMethod(url);

        Map<String, String> parameters = new TreeMap<String, String>();
        parameters.put("name", tavernaId);
        parameters.put("description", "");
        parameters.put("priority", "55");
        parameters.put("type", "workflow");
        try {
            method.setRequestEntity(new StringRequestEntity(JSONObject.fromObject(parameters).toString(),
                    CloudFacadeConstants.JSON_CONTENT_TYPE, CloudFacadeConstants.JSON_ENCODING_TYPE));
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported Encoding: " + e.getMessage());
            throw new WorkflowException("Transport Error", e);
        }

        method.setDoAuthentication(true);

        WorkflowException ex = null;
        try {
            // Execute the method.
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: " + method.getStatusLine());
            }
            // Read the response body.
            cloudFacadeId = method.getResponseBodyAsString();
        } catch (HttpException e) {
            logger.error("Fatal protocol violation: " + e.getMessage());
            ex = new WorkflowException("Transport Error", e);
        } catch (IOException e) {
            logger.error("Fatal transport error: " + e.getMessage());
            ex = new WorkflowException("Transport Error", e);
        } finally {
            // Release the connection.
            method.releaseConnection();
            if (ex != null) {
                throw ex;
            }
        }

    }

    /**
     * 
     * GET /as/{atomicServiceId}/configurations - gets atomic service configurations
     * 
     * The structure of the returned JSON includes:
     * 
     * id - configuration id name - configuration name
     * 
     * @param atomicServiceId
     * @throws UnsupportedEncodingException
     * @throws WorkflowException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public String getAtomicServiceConfigId(String atomicServiceId) throws UnsupportedEncodingException,
            WorkflowException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String url = CloudFacadeConstants.CLOUDFACADE_URL + "/as/" + urlencode(atomicServiceId) + "/configurations";
        logger.debug("Getting URL \"" + url + "\"");
        GetMethod method = new GetMethod(url);

        // Create an instance of HttpClient
        HttpClient client = new HttpClient();

        // Authentication
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, credentials);

        String atomicServiceConfigId = "";
        WorkflowException ex = null;
        String result = "";
        try {
            // Execute the method
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                String message = "Error 404; probably the given atomicServiceId \"" + atomicServiceId
                        + "\" doesn't exist.";
                logger.error(message);
                ex = new WorkflowException(message);

            } else if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: " + method.getStatusLine());
                ex = new WorkflowException("Method failed: " + method.getStatusLine());
            } else {
                // Read the atomicServiceConfigId from the response body
                atomicServiceConfigId = method.getResponseBodyAsString();
                JSONArray jsonArray = JSONArray.fromObject(atomicServiceConfigId);
                JSONObject jsonObject = JSONObject.fromObject(jsonArray.get(0));
                result = (String) PropertyUtils.getProperty(jsonObject, "id");
            }
        } catch (HttpException e) {
            logger.error("HttpException while getting asConfigId: " + e.getMessage(), e);
            ex = new WorkflowException("HttpException while getting asConfigId", e);
        } catch (IOException e) {
            logger.error("IOException while getting asConfigId: " + e.getMessage(), e);
            ex = new WorkflowException("IOException while getting asConfigId", e);
        } catch (Throwable e) {
        	logger.error("Unexpected exception", e);
        } finally {
            // Release the connection
            method.releaseConnection();
        }
        if (ex != null) {
            throw ex;
        }
        return result;
    }

    public AtomicServiceInstance addAtomicService(String wsdlURL, Object user, boolean shared) throws WorkflowException {
        logger.info("addAtomicService");
        VPHAtomicService atomicService;
        try {
            atomicService = serviceURLtoAtomicServiceInstance(wsdlURL);
        } catch (HttpException e) {
            String message = "HttpException while finding out atomicServiceId from WSDL URL";
            logger.error(message, e);
            throw new WorkflowException(message);
        } catch (IOException e) {
            String message = "IOException while finding out atomicServiceId from WSDL URL";
            logger.error(message, e);
            throw new WorkflowException(message);
        }
        String asInstanceName = urlencode(user.toString() + ";" + wsdlURL);
        asInstanceName = String.format("%040x", new BigInteger(asInstanceName.getBytes()));
        String asInstanceId = null; // not known yet
        VPHAtomicServiceInstance atomicServiceInstance = new VPHAtomicServiceInstance(atomicService, asInstanceId, asInstanceName, this);
        if (!sharableAtomicServices.containsKey(atomicService.getId()) || !shared) {
            logger.info("AddAtomicService \"" + atomicService.getId() + "\"");

            String configId;
            try {
                configId = getAtomicServiceConfigId(atomicService.getId());
                atomicServiceInstance.setAsConfigId(configId);
            } catch (UnsupportedEncodingException e) {
                String message = "UnsupportedEncodingException while getting AtomicServiceConfigId: " + e.getMessage();
                logger.error(message);
                throw new WorkflowException(message, e);
            } catch (IllegalAccessException e) {
                String message = "IllegalAccessException while getting AtomicServiceConfigId: " + e.getMessage();
                logger.error(message);
                throw new WorkflowException(message, e);
            } catch (InvocationTargetException e) {
                String message = "InvocationTargetException while getting AtomicServiceConfigId: " + e.getMessage();
                logger.error(message);
                throw new WorkflowException(message, e);
            } catch (NoSuchMethodException e) {
                String message = "NoSuchMethodException while getting AtomicServiceConfigId: " + e.getMessage();
                logger.error(message);
                throw new WorkflowException(message, e);
            }

            // Create an instance of HttpClient
            HttpClient client = new HttpClient();

            // Authentication
            client.getParams().setAuthenticationPreemptive(true);
            client.getState().setCredentials(AuthScope.ANY, credentials);

            // Create a method instance
            String url = CloudFacadeConstants.CLOUDFACADE_URL + "/workflow/" + cloudFacadeId + "/as/" + configId + "/" + asInstanceName;
            PutMethod method = new PutMethod(url);
            try {
                method.setRequestEntity(new StringRequestEntity(atomicService.getId(), CloudFacadeConstants.TEXT_CONTENT_TYPE, CloudFacadeConstants.TEXT_ENCODING_TYPE));
            } catch (UnsupportedEncodingException e) {
                logger.error("UnsupportedEncodingException while trying to encode the AS instance name: " + e.getMessage());
                throw new WorkflowException("UnsupportedEncodingException while trying to encode the AS instance name", e);
            }

            WorkflowException ex = null;
            try {
                // Execute the method
                logger.debug("Executing PUT on URL \"" + method.getURI() + "\"");
                int statusCode = client.executeMethod(method);
                if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NO_CONTENT) {
                    String message = "Method failed: " + method.getStatusLine() + "\n"
                            + method.getResponseBodyAsString();
                    logger.error(message);
                    ex = new WorkflowException(message);
                }
                if (shared) {
                    // TODO: Maybe in a future version of CloudFacade we can obtain the asInstanceId from this HTTP call
                    // and write it into the atomicServiceInstance object
                    // atomicServiceInstance.setId(method.getResponseBodyAsString());
                    
                    sharableAtomicServices.put(atomicService.getId(), atomicServiceInstance);
                }
                atomicServiceUsers.add(user);
            } catch (HttpException e) {
                String message = "HttpException while instantiating AtomicService" + e.getMessage();
                logger.error(message);
                ex = new WorkflowException(message, e);
            } catch (IOException e) {
                String message = "HttpException while instantiating AtomicService" + e.getMessage();
                logger.error(message);
                ex = new WorkflowException(message, e);
            } finally {
                // Release the connection.
                method.releaseConnection();
            }
            if (ex != null) {
                throw ex;
            }
            return atomicServiceInstance;
        } else {
            atomicServiceUsers.add(user);
            return sharableAtomicServices.get(atomicService.getId());
        }
    }

    private VPHAtomicService serviceURLtoAtomicServiceInstance(String wsdlURL) throws HttpException,
            IOException, WorkflowException {
        Pattern p = Pattern.compile("http[s]?://vph\\.cyfronet\\.pl/cloudfacade/as/([^/]*)/endpoint/([^/])*/(.*)");
        Matcher m = p.matcher(wsdlURL);
        if (m.find()) {
            String servicePath = m.group(3);
            PathInfo pathInfo = requestPathInfo(wsdlURL);
            return new VPHAtomicService(pathInfo.atomicServiceId, servicePath, pathInfo.redirectionName);
        } else {
            String message = "Could not identify AtomicService from WSDL URL " + wsdlURL;
            logger.error(message);
            throw new WorkflowException(message);
        }
        // For testing purposes:
    	/*String servicePath = "axis2/services/wsGimias";
    	String atomicServiceId = requestAtomicServiceId("https://vph.cyfronet.pl/cloudfacade/as/Gaussian%20Blur/endpoint/8443/axis2/services/wsGimias");
    	return new VPHAtomicServiceInstance(atomicServiceId, this, servicePath);*/
    }

    private PathInfo requestPathInfo(String url) throws HttpException, IOException, WorkflowException {
        // Create request
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url + "/get_path_info");

        // Add authentication
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, credentials);

        // Execute call
        client.executeMethod(method);
        if (method.getStatusCode() == HttpStatus.SC_OK) {
            String body = method.getResponseBodyAsString();
            JSONObject jsonObject = JSONObject.fromObject(body);
            PathInfo result = new PathInfo();
            try {
                result.atomicServiceId = (String) PropertyUtils.getProperty(jsonObject, "atomicServiceId");
                result.redirectionName = (String) PropertyUtils.getProperty(jsonObject, "redirectionName");
                return result;
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                String message = "Caught exception while processing the response of \"" + method.getURI()
                        + "\" - Response body: \"" + method.getResponseBodyAsString() + "\"";
                logger.error(message, e);
                throw new WorkflowException(message, e);
            }
        } else {
            String message = "Bad response while trying to find out path info from \"" + method.getURI() + "\": "
                    + method.getStatusLine() + " - Body: \"" + method.getResponseBodyAsString() + "\"";
            logger.error(message);
            throw new WorkflowException(message);
        }
    }

    public void userStopped(Object user) throws WorkflowException {
        atomicServiceUsers.remove(user);
        if (atomicServiceUsers.isEmpty()) {
            deleteWorkflow();
        }
    }

    public String getTavernaId() {
        return tavernaId;
    }

    public String getCloudFacadeId() {
        return cloudFacadeId;
    }
}
