package org.overbaard.ci.multi.repo.config.component;

import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfig {
    private final String componentName;
    private final String buildJob;
    private final List<ComponentJobConfig> jobs;
    private final ComponentEndJobConfig endJob;

    ComponentJobsConfig(String componentName, String buildJob, List<ComponentJobConfig> jobs, ComponentEndJobConfig endJob) {
        this.componentName = componentName;
        this.buildJob = buildJob;
        this.jobs = jobs;
        this.endJob = endJob;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getBuildJob() {
        return buildJob;
    }

    public List<ComponentJobConfig> getJobs() {
        return jobs;
    }

    public ComponentEndJobConfig getEndJob() {
        return endJob;
    }
}
