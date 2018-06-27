package org.xillium.base.util;

import java.util.concurrent.Callable;
import org.xillium.base.Open;

/**
 * An EnvironmentReference is a Callable that when called returns an Open object containing environment data values.
 */
public interface EnvironmentReference extends Callable<Open> {
}
