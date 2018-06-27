package org.xillium.base.util;


/**
 * An EnvironmentAware uses an EnvironmentReference and adapts based on the environment values contained within.
 */
public interface EnvironmentAware {
    public void setEnvironmentReference(EnvironmentReference reference);
}
