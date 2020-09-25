package org.overbaard.ci.multi.repo.config.component;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobConfig extends BaseComponentJobConfig {
    private final boolean buildJob;
    private final List<JobRunElementConfig> runElements;

    ComponentJobConfig(String name, boolean buildJob, Map<String, String> jobEnv, String javaVersion, List<String> needs, List<String> runsOn, List<JobRunElementConfig> runElements) {
        super(name, jobEnv, javaVersion, needs, runsOn);
        this.buildJob = buildJob;
        this.runElements = runElements;
    }

    public List<JobRunElementConfig> getRunElements() {
        return runElements;
    }

    @Override
    public boolean isEndJob() {
        return false;
    }

    @Override
    public boolean isBuildJob() {
        return buildJob;
    }
}
