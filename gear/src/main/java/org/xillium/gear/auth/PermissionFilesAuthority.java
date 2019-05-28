package org.xillium.gear.auth;

import java.io.*;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


/**
 * An Authority that manages roles and permissions in permission files, which are either in the file system or on the class path.
 *
 * A permission file is a plain text document containing lines of permission specifications. Each permission specification is given
 * in a single line with the format "ROLE_ID:FUNCTION:PERMISSION".
 */
@lombok.extern.log4j.Log4j2
public class PermissionFilesAuthority implements Authority {
	private final List<Permission> _permissions = new ArrayList<Permission>();

    /**
     * Constructs a PermissionFilesAuthority.
     *
     * @param locations - a list of permission file locations or location patterns as supported by
     * org.springframework.core.io.support.PathMatchingResourcePatternResolver
     */
	public PermissionFilesAuthority(List<String> locations) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

		for (String location: locations) {
            try {
                for (Resource resource: resolver.getResources(location)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(":");
                            try {
                                _permissions.add(new Permission(parts[0], parts[1], Integer.parseInt(parts[2])));
                            } catch (Exception x) {
                                _log.warn("Ignored invalid permission: {}", line);
                            }
                        }
                    } finally {
                        reader.close();
                    }
                }
            } catch (Exception x) {}
        }
	}

	/**
	 * Loads all role permissions into memory.
	 */
    public List<Permission> loadRolesAndPermissions() throws Exception {
        return _permissions;
    }
}
