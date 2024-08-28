package test.integration.aiweb;

import java.io.File;

import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

abstract class IntegrationTestDependencies {

    /**
     * Maven resolver that will try to resolve dependencies using pom.xml of the
     * project where this class is located.
     * 
     * @return MavenDependencyResolver
     */
    public static MavenDependencyResolver getMavenResolver() {
        return DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");

    }

    /**
     * Resolve artifacts without dependencies
     * 
     * @param artifactCoordinates
     * @return
     */
    static File resolveArtifactWithoutDependencies(final String artifactCoordinates) {
        final File[] artifacts = getMavenResolver().artifact(artifactCoordinates).exclusion("*").resolveAsFiles();
        if (artifacts == null) {
            throw new IllegalStateException("Artifact with coordinates " + artifactCoordinates + " was not resolved");
        }
        if (artifacts.length != 1) {
            throw new IllegalStateException("Resolved more then one artifact with coordinates " + artifactCoordinates);
        }
        return artifacts[0];
    }

}
