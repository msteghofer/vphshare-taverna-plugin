package org.vphshare.tavernaplugin.ui.serviceprovider;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.vphshare.tavernaplugin.VphShareActivity;

import net.sf.taverna.t2.workbench.activityicons.ActivityIconSPI;
import net.sf.taverna.t2.workflowmodel.processor.activity.Activity;

public class VphShareServiceIcon implements ActivityIconSPI {

    private static Icon icon;

    public int canProvideIconScore(Activity<?> activity) {
        if (activity instanceof VphShareActivity) {
            return DEFAULT_ICON;
        }
        return NO_ICON;
    }

    public Icon getIcon(Activity<?> activity) {
        return getIcon();
    }

    public static Icon getIcon() {
        if (icon == null) {
            icon = new ImageIcon(VphShareServiceIcon.class.getResource("/icon-small.png"));
        }
        return icon;
    }

}
