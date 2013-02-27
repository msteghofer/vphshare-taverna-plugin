package org.vphshare.tavernaplugin.ui.menu;

import javax.swing.Action;

import org.vphshare.tavernaplugin.VphShareActivity;

import net.sf.taverna.t2.workbench.activitytools.AbstractConfigureActivityMenuAction;

public class VphShareConfigureMenuAction extends AbstractConfigureActivityMenuAction<VphShareActivity> {

    public VphShareConfigureMenuAction() {
        super(VphShareActivity.class);
    }

    @Override
    protected Action createAction() {
        return null;
    }

}
