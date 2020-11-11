package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AbsolutePathVariableStepBuilder {
    private final String variableName;

    public AbsolutePathVariableStepBuilder(String variableName) {
        this.variableName = variableName;
    }

    Map<String, Object> build() {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("name", "Make " + variableName + " an absolute path");
        StringBuilder run = new StringBuilder();
        run.append(
                BashUtils.setEnvVar(variableName, String.format("${GITHUB_WORKSPACE}/${%s}", variableName)));

        command.put("run", run.toString());

        return command;
    }
}
