package org.vphshare.tavernaplugin.ui.view;

import java.awt.Frame;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.vphshare.tavernaplugin.VphShareActivity;
import org.vphshare.tavernaplugin.ui.config.VphShareConfigureAction;

import net.sf.taverna.t2.activities.wsdl.WSDLActivityConfigurationBean;
import net.sf.taverna.t2.activities.wsdl.actions.WSDLActivityConfigureAction;
import net.sf.taverna.t2.workbench.ui.views.contextualviews.ContextualView;


@SuppressWarnings("serial")
public class VphShareContextualView extends ContextualView {
    private final VphShareActivity activity;
    private JLabel description = new JLabel("ads");

    public VphShareContextualView(VphShareActivity activity) {
        this.activity = activity;
        initView();
    }

    @Override
    public JComponent getMainFrame() {
        JPanel jPanel = new JPanel();
        jPanel.add(description);
        refreshView();
        return jPanel;
    }

    @Override
    public String getViewTitle() {
        WSDLActivityConfigurationBean configuration = activity.getConfiguration();
        return "VPH-Share service " + configuration.getWsdl();
    }

    /**
     * Typically called when the activity configuration has changed.
     */
    @Override
    public void refreshView() {
        WSDLActivityConfigurationBean configuration = activity.getConfiguration();
        description.setText("VPH-Share service " + configuration.getWsdl());
    }

    /**
     * View position hint
     */
    @Override
    public int getPreferredPosition() {
        // We want to be on top
        return 100;
    }

    @Override
    public Action getConfigureAction(final Frame owner) {
        return new VphShareConfigureAction(activity, owner, new WSDLActivityConfigureAction(activity, owner));
    }

}
