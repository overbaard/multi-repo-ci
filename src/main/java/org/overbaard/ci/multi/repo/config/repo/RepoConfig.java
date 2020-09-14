package org.overbaard.ci.multi.repo.config.repo;

import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RepoConfig {

    public static final boolean DEFAULT_COMMENTS_REPORTING = true;

    private final Map<String, String> env;
    private final String javaVersion;
    private final boolean commentsReporting;
    private final String successLabel;
    private final String failureLabel;

    RepoConfig(Map<String, String> env, String javaVersion, boolean commentsReporting, String successLabel, String failureLabel) {
        this.env = env;
        this.javaVersion = javaVersion;
        this.commentsReporting = commentsReporting;
        this.successLabel = successLabel;
        this.failureLabel = failureLabel;
    }

    RepoConfig() {
        this(Collections.emptyMap(), null, DEFAULT_COMMENTS_REPORTING, null, null);
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public boolean isCommentsReporting() {
        return commentsReporting;
    }

    public String getSuccessLabel() {
        return successLabel;
    }

    public String getFailureLabel() {
        return failureLabel;
    }
}
