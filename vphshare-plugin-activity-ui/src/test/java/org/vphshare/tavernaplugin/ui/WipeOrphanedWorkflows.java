package org.vphshare.tavernaplugin.ui;

import org.vphshare.tavernaplugin.instantiation.WorkflowRegister;
import org.vphshare.tavernaplugin.instantiation.WorkflowRegister.WorkflowRegisterException;

public class WipeOrphanedWorkflows {

    public static void main(String[] args) throws WorkflowRegisterException {
        WorkflowRegister.getInstance().deleteAllWorkflowIds();
    }

}
