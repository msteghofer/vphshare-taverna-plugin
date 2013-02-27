package org.vphshare.tavernaplugin.ui.view;

import java.util.Arrays;
import java.util.List;

import org.vphshare.tavernaplugin.VphShareActivity;

import net.sf.taverna.t2.workbench.ui.views.contextualviews.ContextualView;
import net.sf.taverna.t2.workbench.ui.views.contextualviews.activity.ContextualViewFactory;


public class VphShareActivityContextViewFactory implements ContextualViewFactory<VphShareActivity> {

    public boolean canHandle(Object selection) {
        return selection instanceof VphShareActivity;
    }

    public List<ContextualView> getViews(VphShareActivity selection) {
        return Arrays.<ContextualView> asList(new VphShareContextualView(selection));
    }

}
