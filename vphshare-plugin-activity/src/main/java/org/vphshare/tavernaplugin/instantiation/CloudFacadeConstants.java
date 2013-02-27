package org.vphshare.tavernaplugin.instantiation;

public class CloudFacadeConstants {
    public static final String JSON_ENCODING_TYPE = "UTF-8";
    public static final String JSON_CONTENT_TYPE = "application/json";
    
    // public static final String CLOUDFACADE_URL = "https://vph.cyfronet.pl/cloudfacade"; // permanent server
    public static final String CLOUDFACADE_URL = "https://149.156.10.133/cloudfacade"; // testing environment with new cloudfacade version
    
    // public static final String CLOUDFACADE_WSDL_URL_PATTERN = "http[s]?://vph\\.cyfronet\\.pl/cloudfacade/as/([^/]*)/endpoint/([^/])*/(.*)"; // permanent server
    public static final String CLOUDFACADE_WSDL_URL_PATTERN = "http[s]?://149\\.156\\.10\\.133/cloudfacade/as/([^/]*)/endpoint/([^/])*/(.*)"; // testing environment with new cloudfacade version
    
    public static final boolean USE_NEW_CLOUDFACADE_PROTOCOL = true;
    
    public static final String TEXT_CONTENT_TYPE = "text/plain";
    public static final String TEXT_ENCODING_TYPE = "UTF-8";
}
