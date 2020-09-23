package org.overbaard.ci.multi.repo.generator;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class UploadArtifactStepBuilder extends AbstractArtifactStepBuilder<UploadArtifactStepBuilder> {

    UploadArtifactStepBuilder() {
        super("actions/upload-artifact@v2");
    }

    @Override
    UploadArtifactStepBuilder getThis() {
        return this;
    }
}