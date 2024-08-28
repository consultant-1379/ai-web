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
package com.ericsson.nms.security.aiweb;

import java.io.InputStream;
import java.security.cert.X509Certificate;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;

import com.ericsson.nms.security.exceptions.AutoIntegrationRestServiceException;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

@Path("/")
public class AutoIntegrationRestResource {

    @Context
    private HttpServletRequest request;

    @Inject
    AutoIntegrationRestService autoIntegrationRestService;

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    private Logger logger;

    /**
     * This method is invokable by the Node components to handle HTTPS GET in
     * order to be able to download the configuration file of the
     * auto-integration. The request must be carried over secure https
     * connection.
     * 
     * @param serialNumber
     *            The input parameter is optional. The SS certificates are
     *            mandatory to be provided with the client's request.
     * @return The final auto-integration configuration file in
     *         application/octet-stream format. The following are the server
     *         response codes and their usage:<br>
     *         200 OK - File found, sending to client<br>
     *         404 Not Found - No file exists for this serial number<br>
     *         500 Internal Server Error - Could not send the file<br>
     * 
     */
    @GET
    @Path("{param}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getConfigFile(@PathParam("param") final String serialNumber) {
        Response response = null;
        String subjectNameCert = null;

        logger.debug("AIWeb was successfully invoked.");

        // This is the first attempt at network access by a node to the core,
        // network - this is worthy of a security audit entry.
        systemRecorder.recordSecurityEvent(autoIntegrationRestService.getFileName(), "Auto Integration Web Service", "No additional info",
                "NETWORK.INITIAL_NODE_ACCESS", ErrorSeverity.INFORMATIONAL, "SUCCESS");
        try {
            final X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
            if (certs != null) {
                logger.debug("SSL certificate(s) were provided by the client.");
                if (certs[0] != null) {
                    subjectNameCert = certs[0].getSubjectX500Principal().getName();
                    final InputStream file = autoIntegrationRestService.processRequest(serialNumber, subjectNameCert);

                    if (file != null) {

                        response = initializeResponse(file, MediaType.APPLICATION_OCTET_STREAM, Response.Status.OK, subjectNameCert);

                        logger.info("File " + subjectNameCert + " was successfully fetched and attached to https response.");
                    } else {
                        logger.error("No file: "
                                + subjectNameCert
                                + " was found on the file system. Searched for file name based on the subject field from the certificate. Subject Field: "
                                + subjectNameCert);

                        systemRecorder.recordSecurityEvent(autoIntegrationRestService.getFileName(), "Auto Integration Web Service",
                                "Auto Integration attempted by node using a certificate with no matching configuration file name",
                                "NETWORK.CONFIG_FILE_NOT_FOUND", ErrorSeverity.WARNING, "FAILURE");
                        response = initializeResponse(null, null, Response.Status.NOT_FOUND, null);
                    }
                } else {
                    logger.error("No SSL certificates were provided by the client. This should not happen. Cannot continue processing.");
                    response = initializeResponse(null, null, Response.Status.NOT_FOUND, null);
                }
            } else {
                response = initializeResponse(null, null, Response.Status.NOT_FOUND, null);
                logger.error("No SSL certificates were provided by the client. This should not happen. Cannot continue processing.");

                systemRecorder.recordSecurityEvent(autoIntegrationRestService.getFileName(), "Auto Integration Web Service",
                        "Auto Integration attempted by node without presenting certificate", "NETWORK.NODE_CERTIFICATE_NOT_FOUND",
                        ErrorSeverity.WARNING, "FAILURE");
            }
        } catch (AutoIntegrationRestServiceException ex) {
            logger.error("Exception thrown while using Resource Adaptor to read a file from file system :  " + subjectNameCert, ex);
            response = initializeResponse(null, null, Response.Status.INTERNAL_SERVER_ERROR, null);
        } catch (final Exception e) {
            logger.error("Unexpected exception thrown while proccessing request to download file: " + subjectNameCert + " "
                    + Response.Status.INTERNAL_SERVER_ERROR, e);
            response = initializeResponse(null, null, Response.Status.INTERNAL_SERVER_ERROR, null);
            throw e;
        }
        return response;
    }

    /**
     * @param file
     * @param application
     *            media type
     * @param statusCode
     * @param fqdnInCert
     * @return
     */
    private Response initializeResponse(final InputStream file, final String applicationMediaType, final Status statusCode, final String fqdnInCert) {

        ResponseBuilder responseBuilder = null;

        if (file != null) {
            responseBuilder = Response.ok(file);
            if (applicationMediaType.equals(MediaType.APPLICATION_OCTET_STREAM)) {
                responseBuilder.type(MediaType.APPLICATION_OCTET_STREAM);
                responseBuilder.header("Content-Disposition", "attachment; filename=" + fqdnInCert);
            }
        } else {
            responseBuilder = Response.status(statusCode);
        }
        return responseBuilder.build();
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getConfigFile() {
        return getConfigFile("");
    }

}
