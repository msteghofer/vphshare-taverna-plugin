package org.vphshare.tavernaplugin.instantiation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.json.JSONObject;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.vphshare.tavernaplugin.instantiation.Workflow.WorkflowException;

public class WorkflowRegister {

    public class WorkflowRegisterException extends Exception {
        private static final long serialVersionUID = 1L;

        private WorkflowRegisterException(String description, Throwable exception) {
            super(description, exception);
        }

        private WorkflowRegisterException(String description) {
            super(description);
        }
    }

    private static Logger logger = Logger.getLogger(WorkflowRegister.class);

    private Map<String, Workflow> workflows;

    private static WorkflowRegister instance;

    private TokenManager authentication = TokenManager.getInstance();

    private WorkflowRegister() {
        workflows = new TreeMap<String, Workflow>();
    }

    public static WorkflowRegister getInstance() {
        if (instance == null) {
            instance = new WorkflowRegister();
        }
        return instance;
    }

    public Workflow getWorkflow(String id) throws WorkflowException {
        Workflow workflow = workflows.get(id);
        if (workflow == null) {
        	authentication.updateToken(true); // Get a fresh token, just in case this is a long workflow
            workflow = new Workflow(id, authentication.getTokenAuthentication());
            workflow.start();
            workflows.put(id, workflow);
        }
        return workflow;
    }

    public List<String> getAllWorkflowIds() throws WorkflowRegisterException {
        // Create an instance of HttpClient
        HttpClient client = new HttpClient();

        // Authentication
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(AuthScope.ANY, authentication.getTokenAuthentication());

        // Create a method instance
        String url = CloudFacadeConstants.CLOUDFACADE_URL + "/workflow/list";
        GetMethod method = new GetMethod(url);
        method.setDoAuthentication(true);

        WorkflowRegisterException ex = null;
        List<String> result = new LinkedList<String>();
        try {
            // Execute the method
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                String message = "Method failed: " + method.getStatusLine();
                logger.error(message);
                ex = new WorkflowRegisterException(message);
            }

            // Read the response body
            String jsonResponse = method.getResponseBodyAsString();

            // Convert JSON to Bean
            JSONObject jsonObject = JSONObject.fromObject(jsonResponse);
            List<Object> workflows = propertyToList(jsonObject, "workflows");
            for (Object workflow : workflows) {
                String id = (String) PropertyUtils.getProperty(workflow, "id");
                result.add(id);
            }
        } catch (HttpException e) {
            String message = "Fatal protocol violation: " + e.getMessage();
            logger.error(message);
            ex = new WorkflowRegisterException(message, e);
        } catch (IOException e) {
            String message = "Fatal protocol violation: " + e.getMessage();
            logger.error(message);
            ex = new WorkflowRegisterException(message, e);
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
        if (ex != null) {
            throw ex;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> propertyToList(JSONObject jsonObject, String propertyName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<Object> list;
        if (!jsonObject.containsKey(propertyName)) {
            // list not present (web service doesn't return empty lists)
            list = new LinkedList<Object>();
        } else {
            Object workflowsListObject = PropertyUtils.getProperty(jsonObject, propertyName);
            if (workflowsListObject instanceof List) {
                // if more than 1 element
                list = (List<Object>) workflowsListObject;
            } else {
                // if only 1 element
                list = new LinkedList<Object>();
                list.add(workflowsListObject);
            }
        }
        return list;
    }

    public void deleteAllWorkflowIds() throws WorkflowRegisterException {
        List<String> workflowIds = getAllWorkflowIds();
        for (String workflowId : workflowIds) {
            Workflow workflow = new Workflow("", authentication.getTokenAuthentication(), workflowId);
            try {
                workflow.deleteWorkflow();
            } catch (WorkflowException e) {
                String message = "Could not delete workflow " + workflowId;
                logger.error(message, e);
                throw new WorkflowRegisterException(message, e);
            }
        }
    }
}
