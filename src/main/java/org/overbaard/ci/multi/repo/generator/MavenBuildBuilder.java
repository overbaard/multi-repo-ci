package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MavenBuildBuilder {
    String options;

    public MavenBuildBuilder setOptions(String options) {
        this.options = options;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> mavenBuild = new LinkedHashMap<>();
        mavenBuild.put("name", "Build with Maven");
        StringBuilder command = new StringBuilder("mvn -B install ");
        if (options != null) {
            command.append(options);
        }
        mavenBuild.put("run", command.toString());
        return mavenBuild;
    }
}
