package org.vphshare.tavernaplugin.ui.config;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import org.vphshare.tavernaplugin.VphShareActivity;

import net.sf.taverna.t2.activities.wsdl.WSDLActivity;
import net.sf.taverna.t2.activities.wsdl.WSDLActivityConfigurationBean;
import net.sf.taverna.t2.workbench.ui.actions.activity.ActivityConfigurationAction;


@SuppressWarnings("serial")
public class VphShareConfigureAction extends ActivityConfigurationAction<WSDLActivity, WSDLActivityConfigurationBean> {

    private ActivityConfigurationAction<WSDLActivity, WSDLActivityConfigurationBean> action;

    public VphShareConfigureAction(VphShareActivity activity, Frame owner,
            ActivityConfigurationAction<WSDLActivity, WSDLActivityConfigurationBean> action) {
        super(activity);
        this.action = action;
    }

    public void actionPerformed(ActionEvent event) {
        action.actionPerformed(event);
    }

}
