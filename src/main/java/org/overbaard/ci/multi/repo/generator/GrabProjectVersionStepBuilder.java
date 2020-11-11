package org.overbaard.ci.multi.repo.generator;

import static org.overbaard.ci.multi.repo.generator.GitHubActionGenerator.ISSUE_DATA_JSON_PATH;
import static org.overbaard.ci.multi.repo.generator.GitHubActionGenerator.OB_ISSUE_DATA_JSON_VAR_NAME;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GrabProjectVersionStepBuilder {
    private String componentName;
    private String envVarName;

    public GrabProjectVersionStepBuilder setComponentName(String componentName) {
        this.componentName = componentName;
        return this;
    }

    public GrabProjectVersionStepBuilder setEnvVarName(String envVarName) {
        this.envVarName = envVarName;
        return this;
    }

    public Map<String, Object> build() {
        StringBuilder bash = new StringBuilder();
        // Do an initial run where we download everything from maven, which pollutes the output
        bash.append("mvn -B help:evaluate -Dexpression=project.version -pl .\n");
        bash.append("TMP=\"$(mvn -B help:evaluate -Dexpression=project.version -pl . | grep -v '^\\[')\"\n");
        bash.append("echo \"version: ${TMP}\"\n");
        if (envVarName != null) {
            bash.append("echo \"Saving version to env var: \\$" + envVarName + "\"\n");
            bash.append(String.format("echo \"::set-output name=%s::${TMP}\"\n", envVarName));
        }
        // Make the project version available to other steps in the workflow
        // it is a convenience when there is a component job template so that people know where to copy files
        // and so on
        bash.append(BashUtils.setEnvVar(GitHubActionGenerator.OB_PROJECT_VERSION_VAR_NAME, "${TMP}"));

        if (componentName != null) {
            // Update the json file with the version
            bash.append("tmpfile=$(mktemp)\n");
            bash.append("jq --arg version \"${TMP}\" '.components[\"" + componentName + "\"].version=$version' "
                    + "\"${" + OB_ISSUE_DATA_JSON_VAR_NAME + "}\" > \"${tmpfile}\"\n");
            bash.append("mv \"${tmpfile}\" \"${" + OB_ISSUE_DATA_JSON_VAR_NAME + "}\"\n");
        }



        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("name", "Grab project version");
        cmd.put("id", "grab-version");
        cmd.put("run", bash.toString());
        return cmd;
    }
}
