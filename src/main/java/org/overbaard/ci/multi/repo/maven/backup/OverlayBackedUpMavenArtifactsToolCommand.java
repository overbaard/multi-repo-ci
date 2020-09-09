package org.overbaard.ci.multi.repo.maven.backup;

import org.overbaard.ci.multi.repo.ToolCommand;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OverlayBackedUpMavenArtifactsToolCommand implements ToolCommand {
    @Override
    public String getDescription() {
        return "Overlays the maven repository with backed up maven artifacts";
    }

    @Override
    public void invoke(String[] args) throws Exception {
        OverlayBackedUpMavenArtifacts.overlay(args);
    }
}
