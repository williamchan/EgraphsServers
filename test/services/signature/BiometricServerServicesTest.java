package services.signature;

import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub;
import com.xyzmo.wwww.biometricserver.WebServiceUserAndProfileStub.*;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;

public class BiometricServerServicesTest extends TestCase {

    private BiometricServerServices services = new BiometricServerServices();
//    private WebServiceBiometricPartStub webServiceProxyBiometricPart = services.getWebServiceProxyBiometricPart();
//    private WebServiceUserAndProfileStub webServiceProxyUserAndProfile = services.getWebServiceProxyUserAndProfile();

    private static String getSignatureString(File file) throws Exception {
        FileInputStream xmlIn = new FileInputStream(file);
        byte[] xmlBytes = new byte[xmlIn.available()];
        xmlIn.read(xmlBytes);
        return new String(xmlBytes);
    }

    public void test() throws Exception {
        String userId = "will";
        String userName = "William";
        String profileNameDyn1 = "will profile 3";

        File path = new File("test/files");
        String signature1 = getSignatureString(new File(path, "signature1.xml"));
        String signature2 = getSignatureString(new File(path, "signature2.xml"));
        String signature3 = getSignatureString(new File(path, "signature3.xml"));
        String signature4 = getSignatureString(new File(path, "signature4.xml"));
        String signature5 = getSignatureString(new File(path, "signature5.xml"));
        String signature6 = getSignatureString(new File(path, "signature6.xml"));
        String signature7 = getSignatureString(new File(path, "signature7.xml"));

//        clearProfiles(userId);
//        addUser(userId, userName);

//        addProfile(userId, profileNameDyn1);
//        enrollUser(userId, profileNameDyn1, signature1, signature2, signature3, signature4, signature5, signature6);

        verifyUserBySignatureDynamicToDynamic_v1(userId, signature7);
    }

    // Why doesn't profile_Delete_v1 work????!!!!! !@#$
    private void clearProfiles(String userId) throws RemoteException {
        User_GetInfosFromAllProfiles_v1 profiles = new User_GetInfosFromAllProfiles_v1();
        profiles.setBioUserId(userId);
        User_GetInfosFromAllProfiles_v1Response profilesResponse = services.getWebServiceProxyUserAndProfile().user_GetInfosFromAllProfiles_v1(profiles);
        ResultBase profilesResult = profilesResponse.getUser_GetInfosFromAllProfiles_v1Result();
        if (profilesResult.getBaseResult() == BaseResultEnum.ok) {
            ProfileInfo[] profileList = ((ProfileInfosResult_v1) profilesResult).getOkInfo().getProfileList().getProfileInfo();
            for (ProfileInfo profileInfo : profileList) {
                Profile_Delete_v1 profile_Delete_v1 = new Profile_Delete_v1();
                profile_Delete_v1.setProfileId(profileInfo.getProfileId());
                services.getWebServiceProxyUserAndProfile().profile_Delete_v1(profile_Delete_v1);
            }
        } else {
            BiometricServerServices.log.error("Failed getting profiles: " + profilesResult.getErrorInfo().getErrorMsg());
        }
    }

    private void addUser(String userId, String userName) throws java.rmi.RemoteException {
        User_Add_v1 user = new User_Add_v1();
        user.setBioUserId(userId);
        user.setDisplayName(userName);
        user.setBioUserStatus(BioUserStatus.Active);
        User_Add_v1Response userAddResponse = services.getWebServiceProxyUserAndProfile().user_Add_v1(user);
        ResultBase user_add_v1Result = userAddResponse.getUser_Add_v1Result();
        if (user_add_v1Result.getBaseResult() == BaseResultEnum.ok) {
            BiometricServerServices.log.info("User_Add_v1 succeeded: User " + userId + " has been created successfully.");
        } else {
            BiometricServerServices.log.error("Error during User_Add_v1: " + user_add_v1Result.getErrorInfo().getErrorMsg());
            if (user_add_v1Result.getErrorInfo().getError() == ErrorStatus.BioUserAlreadyExists) {
                //do errorhandling here. For all possible error codes see integration guide. } }
            }
        }
    }

    private void addProfile(String userId, String profileNameDyn1) throws java.rmi.RemoteException {
        Profile_Add_v1 profile = new Profile_Add_v1();
        profile.setBioUserId(userId);
        profile.setProfileName(profileNameDyn1);
        profile.setProfileType(ProfileType.Dynamic);
        Profile_Add_v1Response profileAddResponse = services.getWebServiceProxyUserAndProfile().profile_Add_v1(profile);
        ProfileInfoResult_v1 profile_add_v1Result = profileAddResponse.getProfile_Add_v1Result();
        if (profile_add_v1Result.getBaseResult() == BaseResultEnum.ok) {
            BiometricServerServices.log.info("Profile_Add_v1 succeeded: profile for " + userId + " has been created successfully.");
        } else {
            BiometricServerServices.log.error("Error during Profile_Add_v1: " + profile_add_v1Result.getErrorInfo().getErrorMsg());
            if (profile_add_v1Result.getErrorInfo().getError() == ErrorStatus.BioUserAlreadyExists) {
                //do errorhandling here. For all possible error codes see integration guide. } }
            }
        }
    }

    private void enrollUser(String userId, String profileNameDyn1, String signature1, String signature2, String signature3, String signature4, String signature5, String signature6) throws java.rmi.RemoteException {
        WebServiceBiometricPartStub.ArrayOfString signatures = new WebServiceBiometricPartStub.ArrayOfString();
        signatures.addString(signature1);
        signatures.addString(signature2);
        signatures.addString(signature3);
        signatures.addString(signature4);
        signatures.addString(signature5);
        signatures.addString(signature6);
        WebServiceBiometricPartStub.EnrollDynamicProfile_v1 enrollProfileInfo = new WebServiceBiometricPartStub.EnrollDynamicProfile_v1();
        enrollProfileInfo.setBioUserId(userId);
        enrollProfileInfo.setProfileName(profileNameDyn1);
        enrollProfileInfo.setContinuous(false);
        enrollProfileInfo.setSignatureDataContainerXmlStrArr(signatures);
        WebServiceBiometricPartStub.EnrollDynamicProfile_v1Response enrollDynamicResponse1 = services.getWebServiceProxyBiometricPart().enrollDynamicProfile_v1(enrollProfileInfo);
        WebServiceBiometricPartStub.EnrollResultInfo_v1 enrollResult1 = enrollDynamicResponse1.getEnrollDynamicProfile_v1Result();
        if (enrollResult1.getBaseResult() == WebServiceBiometricPartStub.BaseResultEnum.ok) {
            BiometricServerServices.log.info("EnrollDynamicProfile_v1: EnrollResult is " + enrollResult1.getOkInfo().getEnrollResult().getValue());
            // Fucking NPE on .length
//            BiometricServerServices.log.info("EnrollDynamicProfile_v1: " + enrollResult1.getOkInfo().getRejectedSignatures().getRejectedSignature().length + " signatures rejected during enrollment.");
            BiometricServerServices.log.info("EnrollDynamicProfile_v1: Profile " + enrollResult1.getOkInfo().getInfoEnrollOk().getProfileId() + " created; contains " + enrollResult1.getOkInfo().getInfoEnrollOk().getNrEnrolled() + " signatures.");
        } else {
            BiometricServerServices.log.error("Error during EnrollDynamicProfile_v1: " + enrollResult1.getErrorInfo().getErrorMsg());
            if (enrollResult1.getErrorInfo().getError() == WebServiceBiometricPartStub.ErrorStatus.ProfileAlreadyEnrolled) { //do errorhandling here. For all possible error codes see integration guide. } } } else { log.error("Error during EnrollDynamicProfile_v1: " + profileResult.getErrorInfo().getErrorMsg()); if (profileResult.getErrorInfo().getError() == WebServiceUserAndProfileStub.ErrorStatus.ProfileAlreadyExists) { //do errorhandling here. For all possible error codes see integration guide. }
            }
        }
    }

    private void verifyUserBySignatureDynamicToDynamic_v1(String userId, String signature) throws java.rmi.RemoteException {
        WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1 verifyUserBySignatureDynamicToDynamic_v1 = new WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1();
        verifyUserBySignatureDynamicToDynamic_v1.setBioUserId(userId);
        verifyUserBySignatureDynamicToDynamic_v1.setSignatureDataContainerXmlStr(signature);
        WebServiceBiometricPartStub.VerifyUserBySignatureDynamicToDynamic_v1Response verifyResponse = services.getWebServiceProxyBiometricPart().verifyUserBySignatureDynamicToDynamic_v1(verifyUserBySignatureDynamicToDynamic_v1);
        WebServiceBiometricPartStub.VerifyResultInfo_v1 verifyResult = verifyResponse.getVerifyUserBySignatureDynamicToDynamic_v1Result();
        assertEquals(WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch, verifyResult.getOkInfo().getVerifyResult());
    }
}
