package org.overbaard.ci.multi.repo.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ZipFolderBuilder {
    private IfCondition ifCondition;
    private String childDirectory;
    private boolean removeDirectory;
    private String containingDirectory;

    public ZipFolderBuilder setContainingDirectory(String containingDirectory) {
        this.containingDirectory = containingDirectory;
        return this;
    }

    public ZipFolderBuilder setChildDirectoryToZip(String childDirectory) {
        if (childDirectory.contains("/")) {
            throw new IllegalStateException("Child directory name should not contain slashes. " +
                    "It should be a direct child of the containing directory");
        }
        this.childDirectory = childDirectory;
        return this;
    }

    public ZipFolderBuilder setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return this;
    }

    public ZipFolderBuilder removeDirectory() {
        removeDirectory = true;
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> zip = new LinkedHashMap<>();
        if (ifCondition != null) {
            zip.put("if", ifCondition.getValue());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("cd " + containingDirectory +"\n");
        sb.append("zip -r " + childDirectory + ".zip " + childDirectory + "\n");
        if (removeDirectory) {
            sb.append("rm -rf " + childDirectory + "\n");
        }
        zip.put("run", sb.toString());

        return zip;
    }

}
