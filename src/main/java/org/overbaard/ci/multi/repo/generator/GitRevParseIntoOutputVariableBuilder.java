package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitRevParseIntoOutputVariableBuilder {
    private final String stepId;
    private final String envVarName;

    GitRevParseIntoOutputVariableBuilder(String stepId, String envVarName) {
        this.stepId = stepId;
        this.envVarName = envVarName;
    }

    public Map<String, Object> build() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Parsing SHA-1 into output variable");
        step.put("id", stepId);
        StringBuilder bash = new StringBuilder();
        bash.append("TMP=$(git rev-parse HEAD)\n");
        bash.append("echo \"Saving version to env var: \\$" + envVarName + "\"\n");
        bash.append(String.format("echo \"::set-output name=%s::${TMP}\"\n", envVarName));
        step.put("run", bash.toString());
        return step;
    }
}
