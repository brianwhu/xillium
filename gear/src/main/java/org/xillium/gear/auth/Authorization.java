package org.xillium.gear.auth;

import java.util.List;


/**
 * A holder for assembly.
 */
public class Authorization {
	public Credential credential;
	public List<Role> roles;

	public void setCredential(Credential c) {
		credential = c;
	}

    public void setList(List<Role> l) {
        roles = l;
    }
}
