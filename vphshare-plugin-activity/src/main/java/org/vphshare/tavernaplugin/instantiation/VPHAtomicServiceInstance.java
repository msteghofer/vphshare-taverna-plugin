package org.vphshare.tavernaplugin.instantiation;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.vphshare.tavernaplugin.instantiation.Workflow.WorkflowException;


public class VPHAtomicServiceInstance implements AtomicServiceInstance {
    
    private static Logger logger = Logger.getLogger(VPHAtomicServiceInstance.class);
    
    private TokenManager authentication = TokenManager.getInstance();
    
    private String asId;
    private String asConfigId;
    private Workflow workflow;
    private String servicePath;

    public VPHAtomicServiceInstance(String asId, Workflow workflow, String servicePath) {
        this.asId = asId;
        this.workflow = workflow;
        this.servicePath = servicePath;
    }

    @Override
    public boolean isReady() throws WorkflowException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        logger.info("Check, if AS \"" + this.asId + "\" is ready...");
        return workflow.status().get(this.asId).equalsIgnoreCase("running") && canConnectTo(getEndpointURL());
    }
    
    private boolean canConnectTo(String url) {
        logger.info("Checking, if we can connect to " + url);
        boolean canConnectTo = false;
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        
        // Create an instance of HttpClient
        HttpClient client = new HttpClient();

        // Create a method instance
        GetMethod method = new GetMethod(url);

        // Authentication
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, authentication.getTokenAuthentication());

        try {
            // Execute the method
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NO_CONTENT || statusCode == HttpStatus.SC_UNAUTHORIZED) {
                canConnectTo = true;
                logger.info("Yes, we could connect to " + url);
                logger.debug("Status code: " + statusCode);
            } else {
                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                	// this shoudn't happen
                	String message = "Received 404 when trying to access the URL \"" + url
                			+ "\". Probably there's a problem with the nginx server at cyfronet.";
                	logger.error(message);
                	throw new RuntimeException(message);
                } else {
                    logger.info("Could not connect to " + url + " (received status code " + statusCode
                    		+ "). Probably the AS just has not yet booted completely.");
                    logger.debug("Status code: " + statusCode);
                }
            }
        } catch (IOException e) {
            String message = "IOException while checking AS status";
            logger.error(message, e);
        } finally {
            // Release the connection
            method.releaseConnection();
        }
        return canConnectTo;
    }

    @Override
    public void waitForInstantiation() throws WorkflowException {
        while (!isReady()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        logger.debug("Ready!");
    }

    @Override
    public String getEndpointURL() {
        return "http://vph.cyfronet.pl:8000/" + workflow.getCloudFacadeId() + "/" + asConfigId + "/gimias/" + Workflow.urlencode(servicePath);
    }

    @Override
    public String getId() {
        return asId;
    }

    public void setAsConfigId(String asConfigId) {
        this.asConfigId = asConfigId;
    }
}
