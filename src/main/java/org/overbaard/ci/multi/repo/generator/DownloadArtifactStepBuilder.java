package org.overbaard.ci.multi.repo.generator;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DownloadArtifactStepBuilder extends AbstractArtifactStepBuilder<DownloadArtifactStepBuilder> {
    public DownloadArtifactStepBuilder() {
        super("actions/download-artifact@v1");
    }

    @Override
    DownloadArtifactStepBuilder getThis() {
        return this;
    }
}
