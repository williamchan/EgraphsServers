package services.signature;

import junit.framework.TestCase;
import scala.Tuple3;

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

//        Tuple3<String,String,String> stringStringStringTuple3 = TestXyzmoBiometricServices.addUser(userId, userName);
//        stringStringStringTuple3.getClass();

//        XyzmoBiometricServices.addProfile(userId, profileName);

//        XyzmoBiometricServices.javatest_EnrollUser(userId, profileName, signature1, signature2, signature3, signature4, signature5, signature6);

//        WebServiceUserAndProfileStub.ResultBase resultBase = XyzmoBiometricServices.deleteUser("dzj");
//        resultBase.getBaseResult();

//        Tuple3<String, String, String> tuple3 = TestXyzmoBiometricServices.addUser(userId, userName);
//        TestXyzmoBiometricServices.addProfile(userId, profileName);
//        TestXyzmoBiometricServices.javatest_EnrollUser(userId, profileName, signature1, signature2, signature3, signature4, signature5, signature6);
//        Object tuple31 = TestXyzmoBiometricServices.verifyUser(userId, signature6);
//        tuple31.getClass();
    }

    private static String getSignatureString(File file) throws Exception {
        FileInputStream xmlIn = new FileInputStream(file);
        byte[] xmlBytes = new byte[xmlIn.available()];
        xmlIn.read(xmlBytes);
        return new String(xmlBytes);
    }
}
