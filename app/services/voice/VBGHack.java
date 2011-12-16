package services.voice;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * TODO(wchan): Hack to get VBG communication working on Cloudbees.
 * This HttpsURLConnection accepts ALL certificates.
 * Copied-modified from http://stackoverflow.com/questions/1828775/httpclient-and-ssl
 */
public class VBGHack {

    public static HttpsURLConnection getVBGHack() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        HttpsURLConnection conn = (HttpsURLConnection) VBGBiometricServices._url().openConnection();
        conn.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });

        return conn;
    }

    public static class DefaultTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    }

}
