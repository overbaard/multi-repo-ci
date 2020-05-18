package org.overbaard.ci.multi.repo;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class UploadArtifactBuilder extends AbstractArtifactBuilder<UploadArtifactBuilder> {

    UploadArtifactBuilder() {
        super("actions/upload-artifact@v2");
    }
    @Override
    UploadArtifactBuilder getThis() {
        return this;
    }
}