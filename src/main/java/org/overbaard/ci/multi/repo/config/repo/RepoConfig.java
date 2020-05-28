package org.overbaard.ci.multi.repo.config.repo;

import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RepoConfig {

    private final Map<String, String> env;

    RepoConfig(Map<String, String> env) {
        this.env = env;
    }

    RepoConfig() {
        this.env = Collections.emptyMap();
    }

    public Map<String, String> getEnv() {
        return env;
    }
}
