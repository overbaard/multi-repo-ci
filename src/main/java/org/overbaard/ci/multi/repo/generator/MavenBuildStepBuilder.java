package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MavenBuildStepBuilder {

    final String base;
    String options;

    public MavenBuildStepBuilder(String base) {
        assert base != null : "null base";
        this.base = base;
    }

    public MavenBuildStepBuilder setOptions(String options) {
        this.options = options;
        return this;
    }

    Map<String, Object> build() {
        Map<String, Object> mavenBuild = new LinkedHashMap<>();
        mavenBuild.put("name", "Build with Maven");
        StringBuilder command = new StringBuilder("mvn -B ");
        command.append(base).append(' ');
        if (options != null) {
            command.append(options);
        }
        mavenBuild.put("run", command.toString());
        return mavenBuild;
    }
}
