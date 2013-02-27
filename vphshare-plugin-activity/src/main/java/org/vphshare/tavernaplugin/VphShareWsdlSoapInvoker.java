package org.vphshare.tavernaplugin;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.taverna.t2.activities.wsdl.T2WSDLSOAPInvoker;
import net.sf.taverna.t2.activities.wsdl.WSDLActivityConfigurationBean;
import net.sf.taverna.t2.security.credentialmanager.CMException;
import net.sf.taverna.t2.security.credentialmanager.CredentialManager;
import net.sf.taverna.t2.security.credentialmanager.UsernamePassword;
import net.sf.taverna.wsdl.parser.WSDLParser;

import org.apache.axis.AxisProperties;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.vphshare.tavernaplugin.instantiation.TokenManager;

/**
 * Invokes SOAP based Web Services from T2.
 * 
 * Subclasses WSDLSOAPInvoker used for invoking Web Services from Taverna 1.x and extends it to
 * provide support for invoking secure Web services.
 */
public class VphShareWsdlSoapInvoker extends T2WSDLSOAPInvoker {

    private static final String REFERENCE_PROPERTIES = "ReferenceProperties";
    private static final String ENDPOINT_REFERENCE = "EndpointReference";
    private static Logger logger = Logger.getLogger(VphShareWsdlSoapInvoker.class);
    private static final Namespace wsaNS = Namespace.getNamespace("wsa",
            "http://schemas.xmlsoap.org/ws/2004/03/addressing");

    private String wsrfEndpointReference = null;
    
    private String newEndpoint;
    
    private TokenManager tokenManager;

    public VphShareWsdlSoapInvoker(WSDLParser parser, String operationName, List<String> outputNames) {
        super(parser, operationName, outputNames);
    }

    public VphShareWsdlSoapInvoker(WSDLParser parser, String operationName, List<String> outputNames,
            String wsrfEndpointReference, String newEndpoint, TokenManager tokenManager) {
        this(parser, operationName, outputNames);
        this.wsrfEndpointReference = wsrfEndpointReference;
        this.newEndpoint = newEndpoint;
        this.tokenManager = tokenManager;
    }

    @SuppressWarnings("unchecked")
    protected void addEndpointReferenceHeaders(List<SOAPHeaderElement> soapHeaders) {
        Document wsrfDoc;
        try {
            wsrfDoc = parseWsrfEndpointReference(wsrfEndpointReference);
        } catch (JDOMException e) {
            logger.warn("Could not parse endpoint reference, ignoring:\n" + wsrfEndpointReference, e);
            return;
        } catch (IOException e) {
            logger.error("Could not read endpoint reference, ignoring:\n" + wsrfEndpointReference, e);
            return;
        }

        Element endpointRefElem = null;
        Element wsrfRoot = wsrfDoc.getRootElement();
        if (wsrfRoot.getNamespace().equals(wsaNS) && wsrfRoot.getName().equals(ENDPOINT_REFERENCE)) {
            endpointRefElem = wsrfRoot;
        } else {
            // Only look for child if the parent is not an EPR
            Element childEndpoint = wsrfRoot.getChild(ENDPOINT_REFERENCE, wsaNS);
            if (childEndpoint != null) {
                // Support wrapped endpoint reference for backward compatibility
                // and convenience (T2-677)
                endpointRefElem = childEndpoint;
            } else {
                logger.warn("Unexpected element name for endpoint reference, but inserting anyway: "
                        + wsrfRoot.getQualifiedName());
                endpointRefElem = wsrfRoot;
            }
        }

        Element refPropsElem = endpointRefElem.getChild(REFERENCE_PROPERTIES, wsaNS);
        if (refPropsElem == null) {
            logger.warn("Could not find " + REFERENCE_PROPERTIES);
            return;
        }

        List<Element> refProps = refPropsElem.getChildren();
        // Make a copy of the list as it would be modified by
        // prop.detach();
        for (Element prop : new ArrayList<Element>(refProps)) {
            DOMOutputter domOutputter = new DOMOutputter();
            SOAPHeaderElement soapElem;
            prop.detach();
            try {
                org.w3c.dom.Document domDoc = domOutputter.output(new Document(prop));
                soapElem = new SOAPHeaderElement(domDoc.getDocumentElement());
            } catch (JDOMException e) {
                logger.warn("Could not translate wsrf element to DOM:\n" + prop, e);
                continue;
            }
            soapElem.setMustUnderstand(false);
            soapElem.setActor(null);
            soapHeaders.add(soapElem);
        }
    }

    protected void configureSecurity(Call call, WSDLActivityConfigurationBean bean) throws Exception {
    	UsernamePasswordCredentials credentials = tokenManager.getTokenAuthentication();
        MessageContext context = call.getMessageContext();
        context.setUsername(credentials.getUserName());
        context.setPassword(credentials.getPassword());
    }

    /**
     * Get username and password from Credential Manager or ask user to supply one. Username is the
     * first element of the returned array, and the password is the second.
     */
    protected UsernamePassword getUsernameAndPasswordForService(WSDLActivityConfigurationBean bean,
            boolean usePathRecursion) throws CMException {

        // Try to get username and password for this service from Credential
        // Manager (which should pop up UI if needed)
        CredentialManager credManager = null;
        credManager = CredentialManager.getInstance();
        String wsdl = bean.getWsdl();
        URI serviceUri = URI.create(wsdl);
        UsernamePassword username_password = credManager.getUsernameAndPasswordForService(serviceUri, usePathRecursion,
                null);
        if (username_password == null) {
            throw new CMException("No username/password provided for service " + bean.getWsdl());
        }
        return username_password;
    }

    @Override
    protected List<SOAPHeaderElement> makeSoapHeaders() {
        List<SOAPHeaderElement> soapHeaders = new ArrayList<SOAPHeaderElement>(super.makeSoapHeaders());
        if (wsrfEndpointReference != null && getParser().isWsrfService()) {
            addEndpointReferenceHeaders(soapHeaders);
        }
        return soapHeaders;
    }

    protected org.jdom.Document parseWsrfEndpointReference(String wsrfEndpointReference) throws JDOMException,
            IOException {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(new StringReader(wsrfEndpointReference));
    }

    public Map<String, Object> invoke(Map<String, Object> inputMap, WSDLActivityConfigurationBean bean)
            throws Exception {

        EngineConfiguration wssEngineConfiguration = null;

        // Setting Axis property only works when we also set the Thread's classloader as below
        // (we do it from the
        // net.sf.taverna.t2.workflowmodel.processor.dispatch.layers.Invoke.requestRun())
        // Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        if (AxisProperties.getProperty("axis.socketSecureFactory") == null
                || !AxisProperties.getProperty("axis.socketSecureFactory").equals(
                        "net.sf.taverna.t2.activities.wsdl.security.TavernaAxisCustomSSLSocketFactory")) {
            AxisProperties.setProperty("axis.socketSecureFactory",
                    "net.sf.taverna.t2.activities.wsdl.security.TavernaAxisCustomSSLSocketFactory");
        }

        Call call = super.getCall(wssEngineConfiguration);

        logger.info("Changing endpoint to: " + newEndpoint);

        call.setTargetEndpointAddress(newEndpoint);

        // Now that we have an axis Call object, configure any additional
        // security properties on it (or its message context or its Transport
        // handler), such as WS-Security UsernameToken or HTTP Basic AuthN
        configureSecurity(call, bean);
        
        return invoke(inputMap, call);
    }

}
