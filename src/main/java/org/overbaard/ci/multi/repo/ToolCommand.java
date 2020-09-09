package org.overbaard.ci.multi.repo;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ToolCommand {
    String getDescription();

    void invoke(String[] args) throws Exception;
}
