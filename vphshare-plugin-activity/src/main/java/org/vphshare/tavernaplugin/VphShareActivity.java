package org.vphshare.tavernaplugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.WSDLException;
import javax.xml.parsers.ParserConfigurationException;

//import net.sf.taverna.t2.activities.wsdl.security.SecurityProfiles;
import net.sf.taverna.t2.activities.wsdl.WSDLActivity;
import net.sf.taverna.t2.activities.wsdl.WSDLActivityConfigurationBean;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.ReferenceServiceException;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.OutputPort;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;
import net.sf.taverna.t2.workflowmodel.health.RemoteHealthChecker;
import net.sf.taverna.t2.workflowmodel.utils.Tools;
import net.sf.taverna.wsdl.parser.TypeDescriptor;
import net.sf.taverna.wsdl.parser.UnknownOperationException;
import net.sf.taverna.wsdl.parser.WSDLParser;

import org.apache.log4j.Logger;
import org.vphshare.tavernaplugin.instantiation.AtomicServiceInstance;
import org.vphshare.tavernaplugin.instantiation.TokenManager;
import org.vphshare.tavernaplugin.instantiation.Workflow;
import org.vphshare.tavernaplugin.instantiation.WorkflowRegister;
import org.vphshare.tavernaplugin.instantiation.Workflow.WorkflowException;
import org.xml.sax.SAXException;

/**
 * An asynchronous Activity that is concerned with WSDL based web-services.
 * <p>
 * The activity is configured according to the WSDL location and the operation.<br>
 * The ports are defined dynamically according to the WSDL specification, and in addition an output<br>
 * port <em>attachmentList</em> is added to represent any attachements that are returned by the
 * webservice.
 * </p>
 * 
 * @author Stuart Owen
 * @author Stian Soiland-Reyes
 */
public class VphShareActivity extends WSDLActivity {
    private static final String ENDPOINT_REFERENCE = "EndpointReference";
    private WSDLActivityConfigurationBean configurationBean;
    private WSDLParser parser;
    private Map<String, Integer> outputDepth = new HashMap<String, Integer>();
    private boolean isWsrfService = false;
    private String endpointReferenceInputPortName;

    public boolean isWsrfService() {
        return isWsrfService;
    }

    private static Logger logger = Logger.getLogger(VphShareActivity.class);

    /**
     * Configures the activity according to the information passed by the configuration bean.<br>
     * During this process the WSDL is parsed to determine the input and output ports.
     * 
     * @param bean
     *            the {@link WSDLActivityConfigurationBean} configuration bean
     */
    @Override
    public void configure(WSDLActivityConfigurationBean bean) throws ActivityConfigurationException {
        logger.debug("configure");
        if (this.configurationBean != null) {
            // throw new IllegalStateException(
            // "Reconfiguring WSDL activity not yet implemented");
            this.configurationBean = bean;
        } else {
            this.configurationBean = bean;
            try {
                parseWSDL();
                configurePorts();
            } catch (Exception ex) {
                throw new ActivityConfigurationException("Unable to parse the WSDL " + bean.getWsdl(), ex);
            }
        }
    }

    /**
     * @return a {@link WSDLActivityConfigurationBean} representing the WSDLActivity configuration
     */
    @Override
    public WSDLActivityConfigurationBean getConfiguration() {
        return configurationBean;
    }

    /*
     * (non-Javadoc)
     * 
     * @seenet.sf.taverna.t2.activities.wsdl.InputPortTypeDescriptorActivity#
     * getTypeDescriptorForInputPort(java.lang.String)
     */
    public TypeDescriptor getTypeDescriptorForInputPort(String portName) throws UnknownOperationException, IOException {
        List<TypeDescriptor> inputDescriptors = parser.getOperationInputParameters(configurationBean.getOperation());
        TypeDescriptor result = null;
        for (TypeDescriptor descriptor : inputDescriptors) {
            if (descriptor.getName().equals(portName)) {
                result = descriptor;
                break;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seenet.sf.taverna.t2.activities.wsdl.InputPortTypeDescriptorActivity#
     * getTypeDescriptorsForInputPorts()
     */
    public Map<String, TypeDescriptor> getTypeDescriptorsForInputPorts() throws UnknownOperationException, IOException {
        Map<String, TypeDescriptor> descriptors = new HashMap<String, TypeDescriptor>();
        List<TypeDescriptor> inputDescriptors = parser.getOperationInputParameters(configurationBean.getOperation());
        for (TypeDescriptor descriptor : inputDescriptors) {
            descriptors.put(descriptor.getName(), descriptor);
        }
        return descriptors;
    }

    /*
     * (non-Javadoc)
     * 
     * @seenet.sf.taverna.t2.activities.wsdl.OutputPortTypeDescriptorActivity#
     * getTypeDescriptorForOutputPort(java.lang.String)
     */
    public TypeDescriptor getTypeDescriptorForOutputPort(String portName) throws UnknownOperationException, IOException {
        TypeDescriptor result = null;
        List<TypeDescriptor> outputDescriptors = parser.getOperationOutputParameters(configurationBean.getOperation());
        for (TypeDescriptor descriptor : outputDescriptors) {
            if (descriptor.getName().equals(portName)) {
                result = descriptor;
                break;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @seenet.sf.taverna.t2.activities.wsdl.OutputPortTypeDescriptorActivity#
     * getTypeDescriptorsForOutputPorts()
     */
    public Map<String, TypeDescriptor> getTypeDescriptorsForOutputPorts() throws UnknownOperationException, IOException {
        Map<String, TypeDescriptor> descriptors = new HashMap<String, TypeDescriptor>();
        List<TypeDescriptor> inputDescriptors = parser.getOperationOutputParameters(configurationBean.getOperation());
        for (TypeDescriptor descriptor : inputDescriptors) {
            descriptors.put(descriptor.getName(), descriptor);
        }
        return descriptors;
    }

    private void parseWSDL() throws ParserConfigurationException, WSDLException, IOException, SAXException,
            UnknownOperationException {
        logger.debug("parseWSDL3");
        URLConnection connection = null;
        try {
            logger.debug("Try");
            URL wsdlURL = new URL(configurationBean.getWsdl());
            connection = wsdlURL.openConnection();
            connection.setConnectTimeout(RemoteHealthChecker.getTimeoutInSeconds() * 1000);
            connection.connect();
            logger.debug("Done");
        } catch (MalformedURLException e) {
            throw new IOException("Malformed URL", e);
        } catch (SocketTimeoutException e) {
            throw new IOException("Timeout", e);
        } catch (IOException e) {
            throw e;
        } finally {
            if ((connection != null) && (connection.getInputStream() != null)) {
                connection.getInputStream().close();
            }
        }
        parser = new WSDLParser(configurationBean.getWsdl());
    }

    private void configurePorts() throws UnknownOperationException, IOException {
        logger.debug("configurePorts");
        List<TypeDescriptor> inputDescriptors = parser.getOperationInputParameters(configurationBean.getOperation());
        List<TypeDescriptor> outputDescriptors = parser.getOperationOutputParameters(configurationBean.getOperation());
        for (TypeDescriptor descriptor : inputDescriptors) {
            addInput(descriptor.getName(), descriptor.getDepth(), true, null, String.class);
        }
        isWsrfService = parser.isWsrfService();
        if (isWsrfService) {
            // Make sure the port name is unique
            endpointReferenceInputPortName = ENDPOINT_REFERENCE;
            int counter = 0;
            while (Tools.getActivityInputPort(this, endpointReferenceInputPortName) != null) {
                endpointReferenceInputPortName = ENDPOINT_REFERENCE + counter++;
            }
            addInput(endpointReferenceInputPortName, 0, true, null, String.class);
        }

        for (TypeDescriptor descriptor : outputDescriptors) {
            addOutput(descriptor.getName(), descriptor.getDepth());
            outputDepth.put(descriptor.getName(), Integer.valueOf(descriptor.getDepth()));
        }

        // add output for attachment list
        addOutput("attachmentList", 1);
        outputDepth.put("attachmentList", Integer.valueOf(1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeAsynch(final Map<String, T2Reference> data, final AsynchronousActivityCallback callback) {
        logger.info("executeAsynch");
        callback.requestRun(new Runnable() {

            public void run() {

                // Extract workflowID
                String[] tokens = callback.getParentProcessIdentifier().split("[:]");
                String workflowID = tokens[0] + ":" + tokens[1];

                //
                WorkflowRegister register = WorkflowRegister.getInstance();
                Workflow workflow = null;
                String newEndpoint = "";
                try {
                    logger.info("Register Workflow");
                    workflow = register.getWorkflow(workflowID);
                    logger.info("Add Atomic Service");
                    AtomicServiceInstance service = workflow.addAtomicService(configurationBean.getWsdl(),
                            callback.getParentProcessIdentifier(), false);
                    logger.info("Get Endpoint URL");
                    newEndpoint = service.getEndpointURL();
                    logger.info("Wait for instantiation");
                    service.waitForInstantiation();
                    logger.debug("Execute");
                } catch (WorkflowException e) {
                    logger.error("Error starting workflow", e);
                    callback.fail("Error starting workflow - an exception was caught: " + e.getMessage());
                    return;
                }

                ReferenceService referenceService = callback.getContext().getReferenceService();

                Map<String, T2Reference> outputData = new HashMap<String, T2Reference>();
                Map<String, Object> invokerInputMap = new HashMap<String, Object>();

                try {
                    String endpointReference = null;
                    for (String key : data.keySet()) {
                        Object renderIdentifier = referenceService.renderIdentifier(data.get(key), String.class,
                                callback.getContext());
                        if (isWsrfService() && key.equals(endpointReferenceInputPortName)) {
                            endpointReference = (String) renderIdentifier;
                        } else {
                            invokerInputMap.put(key, renderIdentifier);
                        }
                    }
                    List<String> outputNames = new ArrayList<String>();
                    for (OutputPort port : getOutputPorts()) {
                        outputNames.add(port.getName());
                    }

                    VphShareWsdlSoapInvoker invoker = new VphShareWsdlSoapInvoker(parser, configurationBean
                            .getOperation(), outputNames, endpointReference, newEndpoint, TokenManager.getInstance());
                    WSDLActivityConfigurationBean bean = getConfiguration();

                    Map<String, Object> invokerOutputMap = invoker.invoke(invokerInputMap, bean);

                    for (String outputName : invokerOutputMap.keySet()) {
                        Object value = invokerOutputMap.get(outputName);

                        if (value != null) {
                            Integer depth = outputDepth.get(outputName);
                            if (depth != null) {
                                outputData.put(outputName,
                                        referenceService.register(value, depth, true, callback.getContext()));
                            } else {
                                logger.info("Depth not recorded for output:" + outputName);
                                outputData.put(outputName,
                                        referenceService.register(value, 0, true, callback.getContext()));
                            }
                        }
                    }
                    callback.receiveResult(outputData, new int[0]);
                } catch (ReferenceServiceException e) {
                    logger.error("Error finding the input data for " + getConfiguration().getOperation(), e);
                    callback.fail("Unable to find input data", e);

                    try {
                        workflow.userStopped(callback.getParentProcessIdentifier());
                    } catch (WorkflowException ex1) {
                        callback.fail("Exception while stopping workflow execution", ex1);
                        return;
                    }

                    return;
                } catch (Exception e) {
                    logger.error("Error invoking WSDL service " + getConfiguration().getOperation(), e);
                    callback.fail("An error occurred invoking the WSDL service", e);

                    try {
                        workflow.userStopped(callback.getParentProcessIdentifier());
                    } catch (WorkflowException ex1) {
                        callback.fail("Exception while stopping workflow execution", ex1);
                        return;
                    }

                    return;
                }

                try {
                    workflow.userStopped(callback.getParentProcessIdentifier());
                } catch (WorkflowException e) {
                    callback.fail("Exception while stopping workflow execution", e);
                    return;
                }
            }

        });

    }
}
