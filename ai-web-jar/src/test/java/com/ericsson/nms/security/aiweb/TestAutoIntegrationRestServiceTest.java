package com.ericsson.nms.security.aiweb;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.ericsson.nms.security.exceptions.AutoIntegrationRestServiceException;

public class TestAutoIntegrationRestServiceTest extends TestCase {

    private AutoIntegrationRestService autoIntegrationRestService;

    private static final String VALID_SERIAL_NUMBER = "SERIALNUMER123";
    private static final String INVALID_SERIAL_NUMBER = "AnotherManufacturer";
    private static final String ERICSSON = "ericsson";
    private static final String COM = "com";
    private static final String INVALID_DOMAIN = "another";

    private static final String VALID_FQDN = VALID_SERIAL_NUMBER + "." + ERICSSON + "." + COM;
    private static final String INVALID_FQDN = INVALID_SERIAL_NUMBER + "." + ERICSSON + "." + COM;

    private static final String[] VALID_DOMAIN_NAME_ARRAY = { VALID_SERIAL_NUMBER, ERICSSON, COM };
    private static final String[] VALID_SERIAL_NUMBER_ONLY_ARRAY = { VALID_SERIAL_NUMBER };
    private static final String[] INVALID_DOMAIN_NAME_ARRAY = { VALID_SERIAL_NUMBER, INVALID_DOMAIN };

    private static final String VALID_CN = "CN=" + VALID_FQDN;
    private static final String INVALID_CN = "CN=" + INVALID_FQDN;

    private static final String TEST_CONFIG_FILE_LOCATION = "src/test/resources/ericsson/tor/data/autointegration/files/";

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        autoIntegrationRestService = new AutoIntegrationRestService();
        logger = mock(Logger.class);
        autoIntegrationRestService.setLogger(logger);
        autoIntegrationRestService.setAutointegration_filelocation(TEST_CONFIG_FILE_LOCATION);
    }

    @Test
    public void testProcessRequest_FileNotNull() {
        try {
            Assert.assertNotNull(autoIntegrationRestService.processRequest(VALID_FQDN, VALID_CN));
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcessRequest_ValidFileNameOnly() {
        InputStream streamReturned = null;
        try {
            streamReturned = autoIntegrationRestService.processRequest(VALID_SERIAL_NUMBER, VALID_CN);
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(checkFileHasContent(streamReturned));
    }

    @Test
    public void testProcessRequest_ValidFileNameAndValidDomainName() {
        InputStream streamReturned = null;
        try {
            streamReturned = autoIntegrationRestService.processRequest(VALID_FQDN, VALID_CN);
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(checkFileHasContent(streamReturned));
    }

    @Test
    public void testProcessRequest_InValidFileName() {
        InputStream streamReturned = null;
        try {
            streamReturned = autoIntegrationRestService.processRequest(INVALID_SERIAL_NUMBER, VALID_CN);
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(checkFileHasContent(streamReturned));
    }

    @Test
    public void testProcessRequest_NoNamePassedIn() {
        InputStream streamReturned = null;
        try {
            streamReturned = autoIntegrationRestService.processRequest("", VALID_CN);
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(checkFileHasContent(streamReturned));
    }

    @Test
    public void testProcessRequest_ValidFileNameAndInValidDomainName() {
        try {
            Assert.assertNull(autoIntegrationRestService.processRequest(VALID_FQDN, INVALID_CN));
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcessRequest_InValidFQDN_InvalidFQDN() {
        try {
            Assert.assertNull(autoIntegrationRestService.processRequest(INVALID_FQDN, INVALID_CN));
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcessRequest_ValidSerialOnly_InvalidFQDN() {
        try {
            Assert.assertNull(autoIntegrationRestService.processRequest(VALID_SERIAL_NUMBER, INVALID_CN));
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcessRequest_InValidSerialOnly_InvalidFQDN() {
        try {
            Assert.assertNull(autoIntegrationRestService.processRequest(INVALID_SERIAL_NUMBER, INVALID_CN));
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcessRequest_NoNamePassedIn_InvalidFQDN() {
        try {
            Assert.assertNull(autoIntegrationRestService.processRequest("", INVALID_CN));
        } catch (AutoIntegrationRestServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testhasDomainName_ValidDomainName() {
        Assert.assertTrue(autoIntegrationRestService.hasDomainName(VALID_DOMAIN_NAME_ARRAY));
    }

    @Test
    public void testhasDomainName_NoDomainName() {
        Assert.assertFalse(autoIntegrationRestService.hasDomainName(VALID_SERIAL_NUMBER_ONLY_ARRAY));
    }

    @Test
    public void testIsDomainNameValid_ValidDomainName() {
        Assert.assertTrue(autoIntegrationRestService.isDomainNameValid(VALID_DOMAIN_NAME_ARRAY));
    }

    @Test
    public void testIsDomainNameValid_InValidDomainName() {
        Assert.assertFalse(autoIntegrationRestService.isDomainNameValid(INVALID_DOMAIN_NAME_ARRAY));
    }

    @Test
    public void testGetCnFromDn_ValidCn() {
        Assert.assertEquals(VALID_FQDN, autoIntegrationRestService.getCNfromDN(VALID_CN));
    }

    @Test
    public void testGetCnFromDn_InValidCn() {
        Assert.assertNotSame(VALID_FQDN, autoIntegrationRestService.getCNfromDN(INVALID_CN));
    }

    @Test
    public void testGetCnFromDn_FullDN() {
        Assert.assertEquals(VALID_FQDN, autoIntegrationRestService.getCNfromDN("O=Test123;CN=" + VALID_FQDN + ";OU=Test345"));
        Assert.assertEquals(VALID_FQDN, autoIntegrationRestService.getCNfromDN("CN=" + VALID_FQDN + ";OU=Test345;O=Test123"));
        Assert.assertEquals(VALID_FQDN, autoIntegrationRestService.getCNfromDN("CN=" + VALID_FQDN + ";OU=Test345"));
    }

    private boolean checkFileHasContent(final InputStream streamReturned) {

        int numOfBytes = 0;
        boolean hasContent = false;
        if (streamReturned == null) {
            logger.error("The stream entered was null: " + streamReturned);
        } else {
            try (InputStreamReader is = new InputStreamReader(streamReturned)) {
                int next = is.read();
                while (next > -1) {
                    numOfBytes++;
                    next = is.read();
                }
            } catch (IOException e) {
                logger.error("The file requested was empty");
            }
            if (numOfBytes > 0) {
                hasContent = true;
            }
        }
        return hasContent;
    }

}
