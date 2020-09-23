package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GrabProjectVersionStepBuilder {
    private String envVarName;


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
        bash.append(String.format("echo \"::set-env name=%s::${TMP}\"\n", GitHubActionGenerator.OB_PROJECT_VERSION_VAR_NAME));

        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("name", "Grab project version");
        cmd.put("id", "grab-version");
        cmd.put("run", bash.toString());
        return cmd;
    }
}
