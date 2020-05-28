package org.overbaard.ci.multi.repo.config.repo;

import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
class RepoConfigImpl implements RepoConfig {

    private final Map<String, String> env;

    RepoConfigImpl(Map<String, String> env) {
        this.env = env;
    }

    RepoConfigImpl() {
        this.env = Collections.emptyMap();
    }

    public Map<String, String> getEnv() {
        return env;
    }
}
