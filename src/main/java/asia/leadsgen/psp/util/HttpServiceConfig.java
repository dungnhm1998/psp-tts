package asia.leadsgen.psp.util;

/**
 * Created by HungDX on 25-May-16.
 */
public class HttpServiceConfig {

    private String serviceName;
    private String serviceRegion;
    private String serviceGroup;
    private String serviceURL;
    private String serviceAuthId;
    private String serviceAuthKey;
    private String serviceAuthType;
    private String serviceAuthAlgorithm;
    private int serviceTimeOut;

    public HttpServiceConfig() {
    }

    public HttpServiceConfig(String serviceName, String serviceRegion, String serviceGroup, String serviceURL,
                             String serviceAuthId, String serviceAuthKey, String serviceAuthType, String serviceAuthAlgorithm, int serviceTimeOut) {
        this.serviceName = serviceName;
        this.serviceRegion = serviceRegion;
        this.serviceGroup = serviceGroup;
        this.serviceURL = serviceURL;
        this.serviceAuthId = serviceAuthId;
        this.serviceAuthKey = serviceAuthKey;
        this.serviceAuthType = serviceAuthType;
        this.serviceAuthAlgorithm = serviceAuthAlgorithm;
        this.serviceTimeOut = serviceTimeOut;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceRegion() {
        return serviceRegion;
    }

    public void setServiceRegion(String serviceRegion) {
        this.serviceRegion = serviceRegion;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public String getServiceAuthId() {
        return serviceAuthId;
    }

    public void setServiceAuthId(String serviceAuthId) {
        this.serviceAuthId = serviceAuthId;
    }

    public String getServiceAuthKey() {
        return serviceAuthKey;
    }

    public void setServiceAuthKey(String serviceAuthKey) {
        this.serviceAuthKey = serviceAuthKey;
    }

    public String getServiceAuthType() {
        return serviceAuthType;
    }

    public void setServiceAuthType(String serviceAuthType) {
        this.serviceAuthType = serviceAuthType;
    }

    public String getServiceAuthAlgorithm() {
        return serviceAuthAlgorithm;
    }

    public void setServiceAuthAlgorithm(String serviceAuthAlgorithm) {
        this.serviceAuthAlgorithm = serviceAuthAlgorithm;
    }

    public int getServiceTimeOut() {
        return serviceTimeOut;
    }

    public void setServiceTimeOut(int serviceTimeOut) {
        this.serviceTimeOut = serviceTimeOut;
    }
}
