package org.vphshare.tavernaplugin.instantiation;

public class VPHAtomicService {
    private String asId;
    private String servicePath;
    private String redirectionName;

    public VPHAtomicService(String asId, String servicePath, String redirectionName) {
        this.asId = asId;
        this.servicePath = servicePath;
        this.redirectionName = redirectionName;
    }

    public String getId() {
        return asId;
    }
    
    public String getServicePath() {
        return servicePath;
    }
    
    public String getRedirectionName() {
        return redirectionName;
    }
}
