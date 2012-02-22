package services.signature;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;

// TO BE DELETED
public class XyzmoServerServicesJavaTests extends TestCase {

    public void test() throws Exception {
        String s = "william4";
        String userId = s;
        String userName = s;
        String profileName = s;

        File path = new File("test/files");
        String signature1 = getSignatureString(new File(path, "xyzmo_signature1.xml"));
        String signature2 = getSignatureString(new File(path, "xyzmo_signature2.xml"));
        String signature3 = getSignatureString(new File(path, "xyzmo_signature3.xml"));
        String signature4 = getSignatureString(new File(path, "xyzmo_signature4.xml"));
        String signature5 = getSignatureString(new File(path, "xyzmo_signature5.xml"));
        String signature6 = getSignatureString(new File(path, "xyzmo_signature6.xml"));
        String signature7 = getSignatureString(new File(path, "xyzmo_signature7.xml"));
        String signature_nomatch = getSignatureString(new File(path, "xyzmo_signature_nomatch.xml"));

//        XyzmoBiometricServices.addUser(userId, userName);
//        XyzmoBiometricServices.addProfile(userId, profileName);
//        XyzmoBiometricServices.javatest_EnrollUser(userId, profileName, signature1, signature2, signature3, signature4, signature5, signature6);
//        XyzmoBiometricServices.verifyUser(userId, signature7);
    }

    private static String getSignatureString(File file) throws Exception {
        FileInputStream xmlIn = new FileInputStream(file);
        byte[] xmlBytes = new byte[xmlIn.available()];
        xmlIn.read(xmlBytes);
        return new String(xmlBytes);
    }
}
