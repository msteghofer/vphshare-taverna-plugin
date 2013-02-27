package org.vphshare.tavernaplugin.ui.serviceprovider;

import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;

import org.apache.log4j.Logger;
import org.vphshare.tavernaplugin.VphShareActivity;
import org.vphshare.tavernaplugin.VphShareActivityConfigurationBean;

import net.sf.taverna.t2.activities.wsdl.WSDLActivityConfigurationBean;
import net.sf.taverna.t2.servicedescriptions.ServiceDescription;
import net.sf.taverna.t2.workflowmodel.processor.activity.Activity;

public class VphShareServiceDesc extends ServiceDescription<WSDLActivityConfigurationBean> {

    private static Logger logger = Logger.getLogger(VphShareServiceDesc.class);

    private ServiceDescription<WSDLActivityConfigurationBean> wsdlService;

    public VphShareServiceDesc(ServiceDescription<WSDLActivityConfigurationBean> wsdlService) {
        logger.info("Constructor");
        this.wsdlService = wsdlService;
    }

    /**
     * The subclass of Activity which should be instantiated when adding a service for this
     * description
     */
    @Override
    public Class<? extends Activity<WSDLActivityConfigurationBean>> getActivityClass() {
        logger.info("getActivityClass");
        return VphShareActivity.class;
    }

    /**
     * The configuration bean which is to be used for configuring the instantiated activity. Making
     * this bean will typically require some of the fields set on this service description, like an
     * endpoint URL or method name.
     * 
     */
    @Override
    public WSDLActivityConfigurationBean getActivityConfiguration() {
        logger.info("getActivityConfiguration");
        VphShareActivityConfigurationBean bean = new VphShareActivityConfigurationBean();
        bean.setConfig(wsdlService.getActivityConfiguration());
        return wsdlService.getActivityConfiguration();
    }

    /**
     * An icon to represent this service description in the service palette.
     */
    @Override
    public Icon getIcon() {
        logger.info("getIcon");
        return wsdlService.getIcon();
    }

    /**
     * The display name that will be shown in service palette and will be used as a template for
     * processor name when added to workflow.
     */
    @Override
    public String getName() {
        logger.info("getName");
        return wsdlService.getName() + "_VPHShare";
    }

    /**
     * The path to this service description in the service palette. Folders will be created for each
     * element of the returned path.
     */
    @Override
    public List<String> getPath() {
        logger.info("getPath");
        @SuppressWarnings("unchecked")
        List<String> path = (List<String>) wsdlService.getPath();
        List<String> vphPath = new LinkedList<String>();
        for (String pathElement : path) {
            vphPath.add("VPH-Share " + pathElement);
        }
        return vphPath;
    }

    /**
     * Return a list of data values uniquely identifying this service description (to avoid
     * duplicates). Include only primary key like fields, ie. ignore descriptions, icons, etc.
     */
    @Override
    protected List<? extends Object> getIdentifyingData() {
        logger.info("getIdentifyingData");
        List<ServiceDescription<WSDLActivityConfigurationBean>> result = new LinkedList<ServiceDescription<WSDLActivityConfigurationBean>>();
        result.add(wsdlService);
        return result;
    }
}
