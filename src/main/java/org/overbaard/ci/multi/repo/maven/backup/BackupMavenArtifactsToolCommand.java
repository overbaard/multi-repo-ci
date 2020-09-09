package org.overbaard.ci.multi.repo.maven.backup;

import org.overbaard.ci.multi.repo.ToolCommand;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class BackupMavenArtifactsToolCommand implements ToolCommand {
    @Override
    public String getDescription() {
        return "Backs up the maven artifacts produced by this build";
    }

    @Override
    public void invoke(String[] args) throws Exception {
        BackupMavenArtifacts.backup(args);
    }
}
