package org.overbaard.ci.multi.repo.generator;

import static org.overbaard.ci.multi.repo.generator.GitHubActionGenerator.OB_ISSUE_DATA_JSON_VAR_NAME;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GitRevParseIntoOutputVariableStepBuilder {
    private final String stepId;
    private final String envVarName;
    private String componentName;

    GitRevParseIntoOutputVariableStepBuilder(String stepId, String envVarName) {
        this.stepId = stepId;
        this.envVarName = envVarName;
    }

    public GitRevParseIntoOutputVariableStepBuilder setComponentName(String componentName) {
        this.componentName = componentName;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", "Parsing SHA-1 into output variable");
        step.put("id", stepId);
        StringBuilder bash = new StringBuilder();
        bash.append("TMP=$(git rev-parse HEAD)\n");
        bash.append("echo \"Saving version to env var: \\$" + envVarName + "\"\n");
        bash.append(String.format("echo \"::set-output name=%s::${TMP}\"\n", envVarName));

        if (componentName != null) {
            // Update the json file with the sha-1
            bash.append("tmpfile=$(mktemp)\n");
            bash.append("jq --arg sha \"${TMP}\" '.components[\"" + componentName + "\"].sha=$sha' "
                    + "\"${" + OB_ISSUE_DATA_JSON_VAR_NAME + "}\" > \"${tmpfile}\"\n");
            bash.append("mv \"${tmpfile}\" \"${" + OB_ISSUE_DATA_JSON_VAR_NAME + "}\"\n");
        }


        step.put("run", bash.toString());
        return step;
    }
}
