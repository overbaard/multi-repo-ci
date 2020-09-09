package org.overbaard.ci.multi.repo.log.copy;

import org.overbaard.ci.multi.repo.ToolCommand;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CopyLogArtifactsToolCommand implements ToolCommand {
    @Override
    public String getDescription() {
        return "Copies across the log files to the artifacts";
    }

    @Override
    public void invoke(String[] args) throws Exception {
        CopyLogArtifacts.copy(args);
    }
}
