package org.overbaard.ci.multi.repo.config.component;

import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfig {
    private final String componentName;
    private final List<String> exportedJobs;
    private final List<JobConfig> jobs;

    public ComponentJobsConfig(String componentName, List<String> exportedJobs, List<JobConfig> jobs) {
        this.componentName = componentName;
        this.exportedJobs = exportedJobs;
        this.jobs = jobs;
    }

    public String getComponentName() {
        return componentName;
    }

    public List<String> getExportedJobs() {
        return exportedJobs;
    }

    public List<JobConfig> getJobs() {
        return jobs;
    }
}
