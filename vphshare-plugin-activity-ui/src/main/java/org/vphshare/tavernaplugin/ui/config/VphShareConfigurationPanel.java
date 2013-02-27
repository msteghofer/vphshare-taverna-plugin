package org.vphshare.tavernaplugin.ui.config;

import java.awt.GridLayout;

import org.vphshare.tavernaplugin.VphShareActivity;
import org.vphshare.tavernaplugin.VphShareActivityConfigurationBean;

import net.sf.taverna.t2.activities.wsdl.WSDLActivity;
import net.sf.taverna.t2.activities.wsdl.WSDLActivityConfigurationBean;
import net.sf.taverna.t2.workbench.ui.views.contextualviews.activity.ActivityConfigurationPanel;


@SuppressWarnings("serial")
public class VphShareConfigurationPanel extends
        ActivityConfigurationPanel<VphShareActivity, VphShareActivityConfigurationBean> {

    private ActivityConfigurationPanel<WSDLActivity, WSDLActivityConfigurationBean> panel;

    VphShareActivityConfigurationBean config;

    public VphShareConfigurationPanel(VphShareActivity activity) {
        // panel = new WSDLActivityConfigurationView(null);

        setLayout(new GridLayout(0, 1));
        add(panel);
    }

    /**
     * Check that user values in UI are valid
     */
    @Override
    public boolean checkValues() {
        return panel.checkValues();
    }

    /**
     * Return configuration bean generated from user interface last time noteConfiguration() was
     * called.
     */
    @Override
    public VphShareActivityConfigurationBean getConfiguration() {
        return config;
    }

    /**
     * Check if the user has changed the configuration from the original
     */
    @Override
    public boolean isConfigurationChanged() {
        return panel.isConfigurationChanged();
    }

    /**
     * Prepare a new configuration bean from the UI, to be returned with getConfiguration()
     */
    @Override
    public void noteConfiguration() {
        panel.noteConfiguration();

        config = new VphShareActivityConfigurationBean();
        config.setConfig(panel.getConfiguration());
    }

    /**
     * Update GUI from a changed configuration bean (perhaps by undo/redo).
     * 
     */
    @Override
    public void refreshConfiguration() {
        panel.refreshConfiguration();
    }
}
