package org.vphshare.tavernaplugin;

import java.util.ArrayList;
import java.util.List;


import net.sf.taverna.t2.visit.VisitReport;
import net.sf.taverna.t2.workflowmodel.health.HealthCheck;
import net.sf.taverna.t2.workflowmodel.health.HealthChecker;

/**
 * VphShare health checker
 * 
 */
public class VphShareActivityHealthChecker implements HealthChecker<VphShareActivity> {

    public boolean canVisit(Object o) {
        // Return True if we can visit the object. We could do
        // deeper (but not time consuming) checks here, for instance
        // if the health checker only deals with VphShareActivity where
        // a certain configuration option is enabled.
        return o instanceof VphShareActivity;
    }

    public boolean isTimeConsuming() {
        // Return true if the health checker does a network lookup
        // or similar time consuming checks, in which case
        // it would only be performed when using File->Validate workflow
        // or File->Run.
        return false;
    }

    public VisitReport visit(VphShareActivity activity, List<Object> ancestry) {
        List<VisitReport> subReports = new ArrayList<VisitReport>();
        return new VisitReport(HealthCheck.getInstance(), activity, "VPH-Share service OK", HealthCheck.NO_PROBLEM,
                subReports);
    }

}
