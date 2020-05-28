package org.overbaard.ci.multi.repo.config.repo;

import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RepoConfig {

    private final Map<String, String> env;
    private final String javaVersion;

    RepoConfig(Map<String, String> env, String javaVersion) {
        this.env = env;
        this.javaVersion = javaVersion;
    }

    RepoConfig() {
        this.env = Collections.emptyMap();
        this.javaVersion = null;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String getJavaVersion() {
        return javaVersion;
    }
}
