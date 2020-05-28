package org.overbaard.ci.multi.repo.config.repo;

import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface RepoConfig {
    Map<String, String> getEnv();
}
