package org.overbaard.ci.multi.repo.config.component;

import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfig {
    private final String componentName;
    private final String buildJob;
    private final List<JobConfig> jobs;

    ComponentJobsConfig(String componentName, String buildJob, List<JobConfig> jobs) {
        this.componentName = componentName;
        this.buildJob = buildJob;
        this.jobs = jobs;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getBuildJob() {
        return buildJob;
    }

    public List<JobConfig> getJobs() {
        return jobs;
    }
}
