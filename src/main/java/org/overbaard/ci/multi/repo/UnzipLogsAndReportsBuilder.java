package org.overbaard.ci.multi.repo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class UnzipLogsAndReportsBuilder {
    String fileName;
    String directory;
    private IfCondition ifCondition;

    public UnzipLogsAndReportsBuilder setName(String jobName) {
        this.fileName = jobName;
        return this;
    }

    public UnzipLogsAndReportsBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    public UnzipLogsAndReportsBuilder setDirectory(String directory) {
        this.directory = directory;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> zip = new LinkedHashMap<>();
        zip.put("name", "Unzip logs and surefire reports");
        if (ifCondition != null) {
            zip.put("if", ifCondition.getValue());
        }
        zip.put("run",
                "mkdir -p " + directory + "\n" +
                        "unzip " + fileName + " -d " + directory + "\n");

        return zip;
    }
}
