package org.overbaard.ci.multi.repo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractArtifactBuilder<T> {
    private final String action;
    private String name;
    private String path = ".";
    private IfCondition ifCondition;

    public AbstractArtifactBuilder(String action) {
        this.action = action;
    }

    public T setName(String name) {
        this.name = name;
        return getThis();
    }

    public T setPath(String path) {
        this.path = path;
        return getThis();
    }

    public T setIfCondition(IfCondition ifCondition) {
        this.ifCondition = ifCondition;
        return getThis();
    }


    Map<String, Object> build() {
        Map<String, Object> upload = new LinkedHashMap<>();
        upload.put("uses", action);
        if (ifCondition != null) {
            upload.put("if", ifCondition.getValue());
        }
        Map<String, Object> with = new LinkedHashMap<>();
        with.put("name", name);
        with.put("path", path);
        upload.put("with", with);

        return upload;
    }

    abstract T getThis();
}
