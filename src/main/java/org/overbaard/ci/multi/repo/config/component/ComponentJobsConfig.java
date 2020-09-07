package org.overbaard.ci.multi.repo.config.component;

import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfig {
    private final String componentName;
    private final String buildStep;
    private final List<JobConfig> jobs;

    ComponentJobsConfig(String componentName, String buildStep, List<JobConfig> jobs) {
        this.componentName = componentName;
        this.buildStep = buildStep;
        this.jobs = jobs;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getBuildStep() {
        return buildStep;
    }

    public List<JobConfig> getJobs() {
        return jobs;
    }
}
