package org.vphshare.tavernaplugin.instantiation;

import org.vphshare.tavernaplugin.instantiation.Workflow.WorkflowException;

public interface AtomicServiceInstance {
    public boolean isReady() throws WorkflowException;

    public void waitForInstantiation() throws WorkflowException;

    public String getEndpointURL();

    public String getId();
}
