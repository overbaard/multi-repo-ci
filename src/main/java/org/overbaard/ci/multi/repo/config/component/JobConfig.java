package org.overbaard.ci.multi.repo.config.component;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JobConfig {
    private final String name;
    private final Map<String, String> jobEnv;
    private final String javaVersion;
    private final List<String> needs;
    private final List<JobRunElementConfig> runElements;

    JobConfig(String name, Map<String, String> jobEnv, String javaVersion, List<String> needs, List<JobRunElementConfig> runElements) {
        this.name = name;
        this.jobEnv = jobEnv;
        this.javaVersion = javaVersion;
        this.needs = needs;
        this.runElements = runElements;
    }

    public String getName() {
        return name;
    }

    public String getJavaVersion() {
        return javaVersion;
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
