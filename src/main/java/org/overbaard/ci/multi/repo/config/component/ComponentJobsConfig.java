package org.overbaard.ci.multi.repo.config.component;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentJobsConfig {
    private final String componentName;
    private final Set<String> exportedJobs;
    private final List<JobConfig> jobs;

    ComponentJobsConfig(String componentName, Set<String> exportedJobs, List<JobConfig> jobs) {
        this.componentName = componentName;
        this.exportedJobs = exportedJobs;
        this.jobs = jobs;
    }

    public String getComponentName() {
        return componentName;
    }

    public Set<String> getExportedJobs() {
        return exportedJobs;
    }

    public List<JobConfig> getJobs() {
        return jobs;
    }
}
