package org.overbaard.ci.multi.repo.config.component;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class BaseComponentJobConfig {
    protected final String name;
    protected final Map<String, String> jobEnv;
    protected final String javaVersion;
    protected final List<String> needs;
    private final List<String> runsOn;

    protected BaseComponentJobConfig(String name, Map<String, String> jobEnv,
                                     String javaVersion, List<String> needs, List<String> runsOn) {
        this.name = name;
        this.jobEnv = jobEnv;
        this.javaVersion = javaVersion;
        this.needs = needs;
        this.runsOn = runsOn;
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

    public List<String> getRunsOn() {
        return runsOn;
    }

    public abstract boolean isEndJob();

    public abstract boolean isBuildJob();
}
