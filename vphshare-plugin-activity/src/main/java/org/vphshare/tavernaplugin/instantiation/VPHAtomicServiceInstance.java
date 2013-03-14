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

    private String asInstanceId;
    private String asInstanceName;
    private String asConfigId;
    private Workflow workflow;
    private VPHAtomicService atomicService;

    public VPHAtomicServiceInstance(VPHAtomicService atomicService, String asInstanceId, String asInstanceName,
            Workflow workflow) {
        this.atomicService = atomicService;
        this.asInstanceId = asInstanceId;
        this.asInstanceName = asInstanceName;
        this.workflow = workflow;
    }

    @Override
    public boolean isReady() throws WorkflowException {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        logger.info("Check, if AS instance { asId=\"" + atomicService.getId() + "\", asInstanceId=\"" + asInstanceId
                + "\", asInstanceName=\"" + asInstanceName + "\" } is ready...");
        // TODO: Depending on how the issue with the missing asInstanceId is resolved, we should use an
        // instance specific id (this.asInstanceId or this.asInstanceName) for the status()-map. But at
        // the moment we have neither: We don't know asInstanceId (is null in this object) because the HTTP
        // method that adds the AS to the workflow doesn't return it. And we cannot use asInstanceName because
        // the structure the is returned by the workflow status service doesn't contain the asInstanceName
        // that we chose when we added the AS to the workflow.
        // So right now we have to use the atomicService id - but this way we cannot distinguish 2 instances of the same AS :-(
        return workflow.status().get(atomicService.getId()).equalsIgnoreCase("running") && canConnectTo(getEndpointURL());
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

        // Make sure that the response is always up-to-date
        method.addRequestHeader("Cache-Control", "no-cache, no-store");

        try {
            // Execute the method
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NO_CONTENT
                    || statusCode == HttpStatus.SC_UNAUTHORIZED) {
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
        return "http://vph.cyfronet.pl:8000/" + workflow.getCloudFacadeId() + "/" + asConfigId + "/"
                + atomicService.getRedirectionName() + "/" + Workflow.urlencode(atomicService.getServicePath());
    }

    public void setId(String asInstanceId) {
        this.asInstanceId = asInstanceId;
    }
    
    @Override
    public String getId() {
        return asInstanceId;
    }

    public void setAsConfigId(String asConfigId) {
        this.asConfigId = asConfigId;
    }
}
