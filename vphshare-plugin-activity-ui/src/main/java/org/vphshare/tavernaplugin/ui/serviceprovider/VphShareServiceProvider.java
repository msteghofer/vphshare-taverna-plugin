package org.vphshare.tavernaplugin.ui.serviceprovider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.log4j.Logger;
import org.vphshare.tavernaplugin.instantiation.TokenManager;

import net.sf.taverna.t2.security.credentialmanager.CMException;
import net.sf.taverna.t2.security.credentialmanager.CredentialManager;
import net.sf.taverna.t2.security.credentialmanager.UsernamePassword;
import net.sf.taverna.t2.servicedescriptions.ConfigurableServiceProvider;
import net.sf.taverna.t2.servicedescriptions.ServiceDescription;
import net.sf.taverna.t2.activities.wsdl.servicedescriptions.WSDLServiceProvider;
import net.sf.taverna.t2.activities.wsdl.servicedescriptions.WSDLServiceProviderConfig;
import net.sf.taverna.t2.servicedescriptions.ServiceDescriptionProvider;
import net.sf.taverna.t2.workflowmodel.ConfigurationException;

public class VphShareServiceProvider implements ServiceDescriptionProvider,
        ConfigurableServiceProvider<WSDLServiceProviderConfig> {

    private WSDLServiceProvider wsdlProvider;

    private static Logger logger = Logger.getLogger(VphShareServiceProvider.class);

    public VphShareServiceProvider() {
        logger.info("Constructor");
        this.wsdlProvider = new WSDLServiceProvider();
    }

    private static final URI providerId = URI.create("http://gimias.org/2012/service-provider/vphshare");

    /**
     * Do the actual search for services. Return using the callBack parameter.
     */
    @SuppressWarnings("unchecked")
    public void findServiceDescriptionsAsync(final FindServiceDescriptionsCallBack callBack) {
        logger.info("findServiceDescriptionsAsync");

        final FindServiceDescriptionsCallBack wrappedCallback = new FindServiceDescriptionsCallBack() {

        	private void cleanup() {
        		String uri = wsdlProvider.getConfiguration().getURI().toString();
                try {
                    CredentialManager credManager = CredentialManager.getInstance();
                	credManager.deleteUsernameAndPasswordForService(uri);

                } catch (CMException e) {
                    logger.error("Could not remove security token from credentials manager for URI \"" + uri + "\"", e);
                    return;
                }
        	}
        	
            @Override
            public void fail(String message, Throwable exception) {
            	cleanup();
                callBack.fail(message, exception);
            }

            @Override
            public void finished() {
            	cleanup();
            	callBack.finished();
            }

            @Override
            public void partialResults(Collection<? extends ServiceDescription> wsdlServices) {
                List<ServiceDescription> results = new ArrayList<ServiceDescription>();

                for (ServiceDescription wsdlService : wsdlServices) {
                    VphShareServiceDesc service = new VphShareServiceDesc(wsdlService);
                    logger.info("Service: " + wsdlService.getName());
                    service.setDescription(wsdlService.getDescription());
                    results.add(service);
                }

                callBack.partialResults(results);
            }

            @Override
            public void status(String status) {
                callBack.status(status);
            }

            @Override
            public void warning(String message) {
                callBack.warning(message);
            }

        };
        
        // call "findServiceDescriptions" in a new thread
        new Thread(new Runnable() {
			@Override
			public void run() {
				findServiceDescriptions(wrappedCallback);
			}
        }).start();
    }
    
    private void findServiceDescriptions(FindServiceDescriptionsCallBack wrappedCallback) {
    	logger.info("findServiceDescriptions");
    	
        CredentialManager credManager = null;
        try {
            credManager = CredentialManager.getInstance();
        } catch (CMException e) {
        	String message = "Could not obtain credentials manager";
            logger.error(message, e);
            wrappedCallback.fail("Could not obtain credentials manager", e);
            return;
        }
        UsernamePasswordCredentials tokenCredentials = TokenManager.getInstance().getTokenAuthentication();
        if (tokenCredentials == null) {
        	String message = "Could not obtain security token using the given Biomedtown credentials!";
        	logger.error(message);
        	wrappedCallback.fail(message, null);
        	return;
        }
        UsernamePassword tokenUserPass = new UsernamePassword(tokenCredentials.getUserName(), tokenCredentials.getPassword());
        try {
			credManager.saveUsernameAndPasswordForService(tokenUserPass, wsdlProvider.getConfiguration().getURI());
		} catch (CMException e) {
			logger.error("Couldn't write token to credentials manager", e);
			wrappedCallback.fail("Couldn't write token to credentials manager", e);
			return;
		}
        this.wsdlProvider.findServiceDescriptionsAsync(wrappedCallback);
    }

    /**
     * Icon for service provider
     */
    public Icon getIcon() {
        logger.info("getIcon");
        return wsdlProvider.getIcon();
    }

    /**
     * Name of service provider, appears in right click for 'Remove service provider'
     */
    public String getName() {
        logger.info("getName");
        return "VPH-Share service";
    }

    @Override
    public String toString() {
        logger.info("toString");
        return getName();
    }

    public String getId() {
        logger.info("getId");
        return providerId.toASCIIString();
    }

    @Override
    public void configure(WSDLServiceProviderConfig configuration) throws ConfigurationException {
        logger.info("configure");
        wsdlProvider.configure(configuration);
    }

    @Override
    public WSDLServiceProviderConfig getConfiguration() {
        logger.info("getConfiguration");
        return wsdlProvider.getConfiguration();
    }

    @Override
    public List<WSDLServiceProviderConfig> getDefaultConfigurations() {
        logger.info("getDefaultConfigurations");
        return new LinkedList<WSDLServiceProviderConfig>();
    }

    public ConfigurableServiceProvider<WSDLServiceProviderConfig> clone() {
        logger.info("clone");
        try {
            VphShareServiceProvider result = (VphShareServiceProvider) super.clone();
            result.wsdlProvider = (WSDLServiceProvider) this.wsdlProvider.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            logger.error(e);
        }
        return null;

    }

}
