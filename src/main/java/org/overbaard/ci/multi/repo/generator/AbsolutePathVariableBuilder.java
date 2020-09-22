package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AbsolutePathVariableBuilder {
    private final String variableName;

    public AbsolutePathVariableBuilder(String variableName) {
        this.variableName = variableName;
    }

    Map<String, Object> build() {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("name", "Make " + variableName + " an absolute path");
        StringBuilder run = new StringBuilder();
        run.append(
                String.format("echo \"::set-env name=%s::${GITHUB_WORKSPACE}/${%s}\"", variableName, variableName));

        command.put("run", run.toString());

        return command;
    }
}
