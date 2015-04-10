package org.xillium.gear.auth;

import org.xillium.data.DataObject;


/**
 * Service description.
 */
public class Credential implements DataObject, Comparable<Credential> {
	public String id;
	public String password;

	public Credential() {
	}

	public Credential(String id, String pass) {
        if (id == null || pass == null) {
            throw new IllegalArgumentException("MissingIdentityOrPassword");
        }
		this.id = id;
		this.password = pass;
	}

    @Override
    public int compareTo(Credential o) {
        int c = id.compareTo(o.id);
        return (c == 0) ? password.compareTo(o.password) : c;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Credential) && compareTo((Credential)o) == 0;
    }

    @Override
    public int hashCode() {
        return (id + password).hashCode();
    }
}
