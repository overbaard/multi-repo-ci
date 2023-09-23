package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MavenBuildStepBuilder {
    String parentVersion;
    String options;

    public MavenBuildStepBuilder setOptions(String options) {
        this.options = options;
        return this;
    }

    public MavenBuildStepBuilder setParentVersion(String parentVersion) {
        this.parentVersion = parentVersion;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> mavenBuild = new LinkedHashMap<>();
        mavenBuild.put("name", "Build with Maven");
        StringBuilder sb = new StringBuilder();
        if (parentVersion != null) {
            // Add a mvn command to update the parent.
            // For the value to take effect when the 'install' runs
            // this needs to be done separately from the 'install'
            // and not just as an added goal before the install.
            sb.append("mvn -B versions:update-parent -DskipResolution -DparentVersion=");
            sb.append(parentVersion);
            sb.append("\n");
        }
        sb.append("mvn -B install ");
        if (options != null) {
            sb.append(options);
        }
        mavenBuild.put("run", sb.toString());
        return mavenBuild;
    }
}
