package org.overbaard.ci.multi.repo.config.component;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ComponentEndJobConfig extends BaseComponentJobConfig {
    private final String ifCondition;
    private final List<Map<String, Object>> steps;

    public ComponentEndJobConfig(String name, Map<String, String> jobEnv, String javaVersion, List<String> needs, String ifCondition, List<Map<String, Object>> steps) {
        super(name, jobEnv, javaVersion, needs);
        this.ifCondition = ifCondition;
        this.steps = steps;
    }

    public String getIfCondition() {
        return ifCondition;
    }

    public List<Map<String, Object>> getSteps() {
        return steps;
    }

    @Override
    public boolean isEndJob() {
        return true;
    }

    @Override
    public boolean isBuildJob() {
        return false;
    }
}
