/**
 * AutoIntegrationRestService class is used to provide authenticated access to
 * the initial configuration files for the newly integrated NEs. AIWeb component
 * is used by the nodes to download Initial Configuration files as a part of the
 * auto integration process by exposing the HTTPS interface by RESTful services.
 *
 * AIWeb authenticates the clients using a public key certificate.
 *
 * @author edobpet
 *
 */
package test.integration.aiweb;

import static junit.framework.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import junit.framework.Assert;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ArchiveImportException;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoIntegrationRestServiceTest class will create https client and send secure
 * requests to ai-web service in order to retrieve the configuration file.
 * 
 * @author edobpet
 */
@RunWith(Arquillian.class)
public class AutoIntegrationRestServiceTest {

    private static final String SERVER = "jboss_managed";
    private static final String BASE_URI = "https://localhost:8443/";
    private static final String CONTEXT_ROOT = "autobind";
    private static final String PARAMETER = "/SERIALNUMBER123";
    private final static String EAR_FOLDER = "../../../ai-web-ear/target/";
    private InputStream in;
    private static final String CERTS_FOLDER = "src/test/resources/jboss_config/";
    private static final String CAAS_WEB_CLIENT_KEYSTORE = CERTS_FOLDER + "aiweb_client_keystore.jks";
    private static final String CAAS_WEB_CLIENT_TRUSTORE = CERTS_FOLDER + "ai-web-client-trustore";
    public static final String PIB_EAR = "com.ericsson.oss.itpf.common:PlatformIntegrationBridge-ear:ear";
    public static final String REST_EASY = "org.jboss.resteasy:resteasy-jaxrs:jar";
    private static final Logger logger = LoggerFactory.getLogger(AutoIntegrationRestServiceTest.class);
    private final static char[] PASSWORD = { 'c', 'h', 'a', 'n', 'g', 'e', 'i', 't' };

    static {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession sslSession) {
                return hostname.equals(sslSession.getPeerHost());
            }
        });
    }

    @Deployment(name = "pib-ear")
    public static Archive<?> createADeployablePKICoreEAR() {
        return IntegrationTestDeploymentFactory.createEARDeploymentFromMavenCoordinates(PIB_EAR);
    }

    @Deployment(testable = false)
    @TargetsContainer(SERVER)
    public static Archive<?> createDeployment() {
        final File earDir = new File(EAR_FOLDER);
        EnterpriseArchive archive = null;
        for (final File file : earDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".ear")) {
                try {
                    archive = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, file);
                } catch (IllegalArgumentException | ArchiveImportException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (archive == null) {
            logger.error("AIWeb ear file does not exist. Run mvn clean install");
        }
        return archive;
    }

    @Deployment(name = "warTest")
    @TargetsContainer(SERVER)
    public static Archive<?> createTestArchive() {
        final MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "aiweb_war_test.war").addAsWebInfResource("META-INF/beans.xml");
        archive.addAsLibraries(resolver.artifact(REST_EASY).resolveAsFiles());
        return archive;
    }

    @Before
    public void setCert() {
        setupSecureSSLConnection();
    }

    @BeforeClass
    public static void setup() throws IOException {
        final String files = "./src/test/resources/ericsson/tor/data/autointegration/files/";
        if (new File(files).exists()) {
            changeConfiguredPropertyValue("autointegration_filelocation", files);
        } else {
            logger.error(files + " does not exist.");
        }
    }

    private static void changeConfiguredPropertyValue(final String paramName, final String paramValue) throws ClientProtocolException, IOException {
        final String updateUrlPrefix = "http://localhost:8080/pib/configurationService/updateConfigParameterValue?paramName=";
        final String updateUrl = updateUrlPrefix + paramName + "&paramValue=" + paramValue;

        final HttpGet httpget = new HttpGet(new URL(updateUrl).toExternalForm());
        final DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.execute(httpget);
    }

    @Test
    @RunAsClient
    public void test200() {
        final String secureUrlAddr = BASE_URI + CONTEXT_ROOT + PARAMETER;
        Assert.assertEquals("The HTTP request has received non-200 server response code.", HttpURLConnection.HTTP_OK, get(secureUrlAddr));
    }

    @Test
    @RunAsClient
    public void testFileEmpty() {
        final String secureUrlAddr = BASE_URI + CONTEXT_ROOT + PARAMETER;
        get(secureUrlAddr);
        Assert.assertFalse(isFileEmpty());
    }

    @Test
    @RunAsClient
    public void test404() {
        final String secureWrongUrlAddr = BASE_URI + CONTEXT_ROOT + "wrongurl" + PARAMETER;
        Assert.assertEquals("The HTTP request has received non-404 server response code.", HttpURLConnection.HTTP_NOT_FOUND, get(secureWrongUrlAddr));
    }

    @Test
    @RunAsClient
    public void testNoParameterPassed() {
        final String secureUrlAddrNoParam = BASE_URI + CONTEXT_ROOT + "/";
        Assert.assertEquals("The HTTP request has received non-200 server response code.", HttpURLConnection.HTTP_OK, get(secureUrlAddrNoParam));
    }

    private int get(final String url) {

        int responseCode = 0;
        HttpsURLConnection httpsCon = null;
        try {
            httpsCon = (HttpsURLConnection) new URL(url).openConnection();
            httpsCon.setRequestMethod("GET");
            responseCode = httpsCon.getResponseCode();
            in = httpsCon.getInputStream();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (httpsCon != null) {
                httpsCon.disconnect();
            }
        }
        return responseCode;
    }

    private boolean isFileEmpty() {
        int size = 0;
        final byte[] buffer = new byte[4096];
        try {
            while ((in.read(buffer)) != -1) {
                ++size;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size > 0;
    }

    private void setupSecureSSLConnection() {
        TrustManager mytm[] = null;
        KeyManager mykm[] = null;
        final String protocol = "SSL";
        final File caasWebClientTrustoreFile = new File(CAAS_WEB_CLIENT_TRUSTORE);
        final File caasWebClientKeystoreFile = new File(CAAS_WEB_CLIENT_KEYSTORE);
        if (caasWebClientTrustoreFile.exists() && caasWebClientTrustoreFile.exists()) {

            try {
                mytm = new TrustManager[] { new CaasX509TrustManager(CAAS_WEB_CLIENT_TRUSTORE, PASSWORD) };
                mykm = new KeyManager[] { new CaasX509KeyManager(CAAS_WEB_CLIENT_KEYSTORE, PASSWORD) };
                SSLContext context = null;
                try {
                    context = SSLContext.getInstance(protocol);
                    context.init(mykm, mytm, null);
                } catch (NoSuchAlgorithmException ex) {
                    logger.error("No Provider supports a TrustManagerFactorySpi implementation for the specified protocol.", ex);
                } catch (KeyManagementException ex) {
                    logger.error("SSL context for this connection was not initialized.", ex);
                }
                context.init(mykm, mytm, null);
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            } catch (Exception ex) {
                logger.error("Certificate for client was not loaded.", ex);
            }
        } else {
            logger.error(CAAS_WEB_CLIENT_TRUSTORE + " or " + caasWebClientKeystoreFile + " do not exist.");
            fail();
        }
    }

    class CaasX509TrustManager implements X509TrustManager {

        /*
         * The default PKIX X509TrustManager9. We'll delegate decisions to it,
         * and fall back to the logic in this class if the default
         * X509TrustManager doesn't trust it.
         */
        X509TrustManager pkixTrustManager;

        CaasX509TrustManager(final String trustStore, final char[] password) {
            this(new File(trustStore), password);
        }

        CaasX509TrustManager(final File trustStore, final char[] password) {
            // create a "default" JSSE X509TrustManager.

            try {
                final KeyStore ks = KeyStore.getInstance("JKS");

                ks.load(new FileInputStream(trustStore), password);

                final TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
                tmf.init(ks);

                final TrustManager tms[] = tmf.getTrustManagers();
                for (final TrustManager tm : tms) {
                    if (tm instanceof X509TrustManager) {
                        pkixTrustManager = (X509TrustManager) tm;
                        return;
                    }
                }
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }

            throw new IllegalStateException("Couldn't initialize");
        }

        /*
         * Delegate to the default trust manager.
         */
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            try {
                pkixTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException excep) {
                //excep.printStackTrace();
            }
        }

        /*
         * Delegate to the default trust manager.
         */
        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            try {
                pkixTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException excep) {
                //excep.printStackTrace();
            }
        }

        /*
         * Merely pass this through.
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return pkixTrustManager.getAcceptedIssuers();
        }
    }

    class CaasX509KeyManager implements X509KeyManager {

        X509KeyManager pkixKeyManager;

        CaasX509KeyManager(final String keyStore, final char[] password) {
            this(new File(keyStore), password);
        }

        CaasX509KeyManager(final File keyStore, final char[] password) {
            // create a "default" JSSE X509KeyManager.

            try {
                final KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream(keyStore), password);

                final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
                kmf.init(ks, password);

                final KeyManager kms[] = kmf.getKeyManagers();
                for (final KeyManager km : kms) {
                    if (km instanceof X509KeyManager) {
                        pkixKeyManager = (X509KeyManager) km;
                        return;
                    }
                }

            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }

            /*
             * Find some other way to initialize, or else we have to fail the
             * constructor.
             */
            throw new IllegalStateException("Couldn't initialize");
        }

        @Override
        public PrivateKey getPrivateKey(final String alias) {
            return pkixKeyManager.getPrivateKey(alias);
        }

        @Override
        public X509Certificate[] getCertificateChain(final String alias) {
            return pkixKeyManager.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(final String keyType, final Principal[] issuers) {
            return pkixKeyManager.getClientAliases(keyType, issuers);
        }

        @Override
        public String chooseClientAlias(final String[] keyType, final Principal[] issuers, final Socket socket) {
            return pkixKeyManager.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String[] getServerAliases(final String keyType, final Principal[] issuers) {
            return pkixKeyManager.getServerAliases(keyType, issuers);
        }

        @Override
        public String chooseServerAlias(final String keyType, final Principal[] issuers, final Socket socket) {
            return pkixKeyManager.chooseServerAlias(keyType, issuers, socket);
        }
    }
}
