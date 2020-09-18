package org.overbaard.ci.multi.repo.directory.utils;

import org.apache.maven.model.Model;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ProjectArtifactInfo {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String relativePath;

    private ProjectArtifactInfo(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.relativePath = groupId.replace('.', '/') + '/' +
                artifactId.replace('.', '/') + '/' +
                version;
    }

    static ProjectArtifactInfo create(Model model) {
        return new ProjectArtifactInfo(
                readGroupId(model),
                readArtifactId(model),
                readVersion(model));
    }

    private static String readGroupId(Model model) {
        String groupId = model.getGroupId();
        if (groupId == null) {
            groupId = model.getParent().getGroupId();
        }
        return groupId;
    }

    private static String readArtifactId(Model model) {
        String artifactId = model.getArtifactId();
        if (artifactId == null) {
            artifactId = model.getParent().getArtifactId();
        }
        return artifactId;
    }

    private static String readVersion(Model model) {
        String version = model.getVersion();
        if (version == null) {
            version = model.getParent().getVersion();
        }
        return version;
    }

    public String getRelativePath() {
        return relativePath;
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
