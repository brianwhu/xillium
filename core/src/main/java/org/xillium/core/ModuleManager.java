package org.xillium.core;

import java.util.stream.*;
import org.xillium.core.util.*;
import lombok.Getter;


/**
 * Management of application modules
 */
public class ModuleManager {
    private static final java.util.function.Function<ServiceModule, String> toMapping = m -> "/" + m.simple + "/*";
    @Getter private ModuleSorter.Sorted packaged;
    @Getter private ModuleSorter.Sorted external;

    public ModuleManager() {
        packaged = ServiceModule.unpack();
        external = ServiceModule.scan(System.getProperty("xillium.service.ExtensionsRoot"));
    }

    public String[] mappings() {
        return Stream.concat(
            Stream.of("/x!/*"),
            Stream.concat(
                Stream.concat(packaged.getSpecials().stream().map(toMapping), packaged.getRegulars().stream().map(toMapping)),
                Stream.concat(external.getSpecials().stream().map(toMapping), external.getRegulars().stream().map(toMapping))
            )
        ).toArray(String[]::new);
    }
}
