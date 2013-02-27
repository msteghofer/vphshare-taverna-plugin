package org.vphshare.tavernaplugin;

import net.sf.taverna.t2.activities.wsdl.WSDLActivityConfigurationBean;

/**
 * VphShare activity configuration bean.
 * 
 */
public class VphShareActivityConfigurationBean extends WSDLActivityConfigurationBean {

    /*
     * TODO: Remove this comment.
     * 
     * The configuration specifies the variable options and configurations for an activity that has
     * been added to a workflow. For instance for a WSDL activity, the configuration contains the
     * URL for the WSDL together with the method name. String constant configurations contain the
     * string that is to be returned, while Beanshell script configurations contain both the scripts
     * and the input/output ports (by subclassing ActivityPortsDefinitionBean).
     * 
     * Configuration beans are serialised as XML (currently by using XMLBeans) when Taverna is
     * saving the workflow definitions. Therefore the configuration beans need to follow the
     * JavaBeans style and only have fields of 'simple' types such as Strings, integers, etc. Other
     * beans can be referenced as well, as long as they are part of the same plugin.
     */

    private WSDLActivityConfigurationBean wsdlConfig;

    public void setConfig(WSDLActivityConfigurationBean wsdlConfig) {
        this.wsdlConfig = wsdlConfig;
    }

    public WSDLActivityConfigurationBean getConfig() {
        return wsdlConfig;
    }

}
