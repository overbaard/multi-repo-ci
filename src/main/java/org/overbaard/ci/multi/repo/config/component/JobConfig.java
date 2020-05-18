package org.overbaard.ci.multi.repo.config.component;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JobConfig {
    private final String name;
    private final Map<String, String> jobEnv;
    private final List<String> needs;
    private final List<JobRunElementConfig> runElements;

    public JobConfig(String name, Map<String, String> jobEnv, List<String> needs, List<JobRunElementConfig> runElements) {
        this.name = name;
        this.jobEnv = jobEnv;
        this.needs = needs;
        this.runElements = runElements;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getJobEnv() {
        return jobEnv;
    }

    public List<String> getNeeds() {
        return needs;
    }

    public List<JobRunElementConfig> getRunElements() {
        return runElements;
    }
}
