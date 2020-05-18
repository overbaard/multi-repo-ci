package org.overbaard.ci.multi.repo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GrabProjectVersionBuilder {
    private String envVarName;
    private String fileName;


    public GrabProjectVersionBuilder setEnvVarName(String envVarName) {
        this.envVarName = envVarName;
        return this;
    }

    public GrabProjectVersionBuilder setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public Map<String, Object> build() {
        StringBuilder bash = new StringBuilder();
        // Do an initial run where we download everything from maven, which pollutes the output
        bash.append("mvn -B help:evaluate -Dexpression=project.version -pl .\n");
        bash.append("TMP=\"$(mvn -B help:evaluate -Dexpression=project.version -pl . | grep -v '^\\[')\"\n");
        bash.append("echo \"version: ${TMP}\"\n");
        if (envVarName != null) {
            bash.append("Saving version to env var: $" + envVarName + "\n");
            bash.append(String.format("echo \"::set-env name=%s::${TMP}\"\n", envVarName));
        }
        if (fileName != null) {
            Path path = Paths.get(fileName);
            if (path.getParent() != null) {
                bash.append(BashUtils.createDirectoryIfNotExist(path.getParent().toString()));
            }

            bash.append("echo \"${TMP}\" > " + fileName + "\n");
        }

        Map<String, Object> cmd = new LinkedHashMap<>();
        cmd.put("name", "Grab project version");
        cmd.put("run", bash.toString());
        return cmd;
    }
}
