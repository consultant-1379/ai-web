package test.integration.aiweb;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;

abstract class IntegrationTestDeploymentFactory {

    /**
     * Create deployment from given maven coordinates
     * 
     * @param mavenCoordinates
     *            Maven coordinates in form of groupId:artifactId:type
     * @return Deployment archive represented by this maven artifact
     */
    static Archive<?> createEARDeploymentFromMavenCoordinates(final String mavenCoordinates) {
        final File archiveFile = IntegrationTestDependencies.resolveArtifactWithoutDependencies(mavenCoordinates);
        if (archiveFile == null) {
            throw new IllegalStateException("Unable to resolve artifact " + mavenCoordinates);
        }
        final EnterpriseArchive ear = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, archiveFile);
        return ear;
    }

}
