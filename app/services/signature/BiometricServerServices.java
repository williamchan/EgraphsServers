package services.signature;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.log4j.Logger;

public class BiometricServerServices {
    public static Logger log = Logger.getLogger(BiometricServerServices.class);
    private com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub _webServiceBiometricPart = null;
    private com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub _webServiceUserAndProfile = null;
    private String username = null;
    private String password = null;
    private String domain = null;

    private int port = 50200;
    private String host = "testlab.xyzmo.com"; // "beta.testlab.xyzmo.com"
    private String url = "http://testlab.xyzmo.com:50200/WebServices/"; // "http://beta.testlab.xyzmo.com:50200/biometricserver/WebServices"


    public com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub getWebServiceProxyBiometricPart() {
        try {
            _webServiceBiometricPart = new com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub(
                    url + "/WebServiceBiometricPart.asmx");
            HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
            auth.setHost(host);
            auth.setPort(port);
            auth.setDomain(domain);
            auth.setUsername(username);
            auth.setPassword(password);
            _webServiceBiometricPart._getServiceClient().getOptions().setProperty(HTTPConstants.AUTHENTICATE, auth);
            return _webServiceBiometricPart;
        } catch (Exception e) {
            log.error("Troubles initializing WebService BiometricPart.", e);
            return null;
        }
    }

    public com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub getWebServiceProxyUserAndProfile() {
        try {
            _webServiceUserAndProfile = null;
            _webServiceUserAndProfile = new com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub(
                    url + "/WebServiceUserAndProfile.asmx");
            HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
            auth.setHost(host);
            auth.setPort(port);
            auth.setDomain(domain);
            auth.setUsername(username);
            auth.setPassword(password);
            _webServiceUserAndProfile._getServiceClient().getOptions().setProperty(HTTPConstants.AUTHENTICATE, auth);
            return _webServiceUserAndProfile;
        } catch (Exception e) {
            log.error("Troubles initializing WebService UserAndProfile", e);
            return null;
        }
    }

    public BiometricServerServices() {
//        this("usermanager", "usermanager", "betatestlab");
        this("usermanager", "%User%01", "testlab");
    }

    public BiometricServerServices(String username, String password, String domain) {
        this.username = username;
        this.password = password;
        this.domain = domain;
    }
}

