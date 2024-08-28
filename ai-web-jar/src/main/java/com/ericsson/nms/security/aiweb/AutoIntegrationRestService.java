/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.nms.security.aiweb;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;

import com.ericsson.nms.security.exceptions.AutoIntegrationRestServiceException;
import com.ericsson.oss.itpf.modeling.annotation.constraints.NotNull;
import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.itpf.sdk.resources.Resource;
import com.ericsson.oss.itpf.sdk.resources.Resources;

public class AutoIntegrationRestService {

    private final static String DOMAIN_NAME_PATTERN = "^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$"; // NOPMD by edobpet on 18/11/13 15:58
    private final Pattern pattern = Pattern.compile(DOMAIN_NAME_PATTERN); // NOPMD by edobpet on 18/11/13 15:58

    @Inject
    private Logger logger;

    @Inject
    @Configured(propertyName = "autointegration_filelocation")
    @NotNull
    private String autointegration_filelocation;
    private Resource resource;

    private String fileName = autointegration_filelocation;

    /**
     * The method is processing the request sent by the node using the serial
     * number of the node and and the fully qualified domain name as in the
     * client's SSL certificate CN field.
     * 
     * @param inputParam
     *            Serial number of the node.
     * @param subjectNameCert
     * @return Returns the InputStream object back to the node.
     * @throws com.ericsson.nms.security.exceptions.AutoIntegrationRestServiceException
     */
    public InputStream processRequest(final String inputParam, final String subjectNameCert) throws AutoIntegrationRestServiceException {
        InputStream configFile = null;

        final String cnInCert = getCNfromDN(subjectNameCert);
        //There can be two formats: CN=Serial123 or CN=Serial123.ericsson.com
        final String[] fqdnFromCert = cnInCert.split("\\.");
        final String fileNameFromSerialNumberInCert = fqdnFromCert[0];

        if (!inputParam.equals(cnInCert)) {
            logger.warn("Serial number mismatch between certificate subject field and serial number in uri. Certificate subject field was "
                    + cnInCert + " but serial number in uri was " + inputParam);
        }

        fileName = autointegration_filelocation + cnInCert;

        if (hasDomainName(fqdnFromCert) && isDomainNameValid(fqdnFromCert)) {
            configFile = fetchFile(fileName);
        } else if (!hasDomainName(fqdnFromCert)) {
            configFile = fetchFile(fileName);
            if (configFile == null) {
                configFile = fetchFile(fileName + ".ericsson.com");
                if (configFile != null) {
                    logger.warn("Expected to find file " + fileNameFromSerialNumberInCert + " , but found file " + fileName + ".ericsson.com"
                            + ". Returning " + fileName + ".ericsson.com" + ".");
                }
            }
        } else {
            logger.error("Subject name field in certificate has invalid domain name. Download of file cannot proceed.");
        }
        return configFile;
    }

    private InputStream fetchFile(final String fileName) {
        InputStream configFile = null;
        resource = Resources.getFileSystemResource(fileName);
        if (resource.exists()) {
            configFile = resource.getInputStream();
        }
        return configFile;
    }

    void listenForConfigurationChanges(@Observes @ConfigurationChangeNotification(propertyName = "autointegration.files") final String newFolder) {
        this.autointegration_filelocation = newFolder;
    }

    protected boolean isDomainNameValid(final String[] fqdnListFromParam) {
        String domainNameFromParam = "";
        for (int i = 1; i < fqdnListFromParam.length; i++) {
            if (i == 1) {
                domainNameFromParam += fqdnListFromParam[i];
            } else {
                domainNameFromParam += "." + fqdnListFromParam[i];
            }
        }
        final Matcher matcher = pattern.matcher(domainNameFromParam);
        return matcher.matches();
    }

    protected boolean hasDomainName(final String[] fqdnListFromParam) {
        return fqdnListFromParam.length > 1;
    }

    /**
     * The method is processing the DN filed from the SSL certificate to extract
     * the CN filed value.
     * 
     * @param dn
     *            The DN field as a string
     * @return
     */
    protected String getCNfromDN(final String dn) {
        final String neededAttributeType = "CN";
        String result = null;
        try {
            final LdapName dn2 = new LdapName(dn);
            final List<Rdn> rdns = dn2.getRdns();

            for (final Iterator<Rdn> iterator = rdns.iterator(); iterator.hasNext();) {
                final Rdn rdn = (Rdn) iterator.next();

                if (rdn != null) {
                    if (rdn.getType().equalsIgnoreCase(neededAttributeType)) {
                        result = (String) Rdn.unescapeValue(rdn.getValue().toString());
                        break;
                    }
                }
            }
        } catch (final InvalidNameException e) {
            logger.error("Invalid DN: " + dn + e.getMessage());
        }
        return result;
    }

    protected void setLogger(final Logger logger) {
        this.logger = logger;
    }

    protected void setAutointegration_filelocation(final String autointegration_filelocation) {
        this.autointegration_filelocation = autointegration_filelocation;
    }

    public String getFileName() {
        return fileName;
    }
}
