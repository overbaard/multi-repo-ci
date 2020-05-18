package org.overbaard.ci.multi.repo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ZipLogsAndReportsBuilder {
    String fileName;
    private IfCondition ifCondition;

    public ZipLogsAndReportsBuilder setFile(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public ZipLogsAndReportsBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> zip = new LinkedHashMap<>();
        zip.put("name", "Zip logs and surefire reports");
        if (ifCondition != null) {
            zip.put("if", ifCondition.getValue());
        }
        String marker = "marker-" + fileName;
        zip.put("run",
                    "echo \"" + marker + "\" >> " + marker +"\n" +
                        "zip -R " + fileName + " '" + marker + "' '*.log' 'surefire-reports/*.txt' 'surefire-reports/*.xml'");

        return zip;
    }
}
