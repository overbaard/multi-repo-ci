package org.overbaard.ci.multi.repo.maven.delta;

import java.util.function.Function;

import org.apache.maven.model.Model;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ProjectArtifactInfo {
    private final String groupId;
    private final String artifactId;
    private final String version;

    private ProjectArtifactInfo(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    static ProjectArtifactInfo create(Model model) {
        return new ProjectArtifactInfo(
                getGroupId(model),
                getArtifactId(model),
                getVersion(model));
    }

    private static String getGroupId(Model model) {
        String groupId = model.getGroupId();
        if (groupId == null) {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }

    private static String getArtifactId(Model model) {
        String artifactId = model.getArtifactId();
        if (artifactId == null) {
            artifactId = model.getParent().getArtifactId();
        }
        return artifactId;
    }

    private static String getVersion(Model model) {
        String version = model.getVersion();
        if (version == null) {
            version = model.getParent().getVersion();
        }
        return version;
    }

    @Override
    public String toString() {
        return "ProjectArtifactInfo{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
