package org.xillium.gear.auth;

import org.xillium.data.DataObject;


/**
 * Service description.
 */
public class Credential implements DataObject {
	public String id;
	public String password;

	public Credential() {
	}

	public Credential(String id, String pass) {
		this.id = id;
		this.password = pass;
	}
}
