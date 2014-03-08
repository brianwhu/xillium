package org.xillium.gear.auth;

import org.xillium.data.DataObject;


/**
 * A secure session object.
 */
public class Session implements DataObject {
    public static final char AT = '@';

	public String id;
	public String token;
	public long clock;
	public long maxAge;

	public Session() {
	}

    public Session(String authcode) {
        int at = authcode.indexOf(AT);
        this.id = authcode.substring(0, at);
        this.token = authcode.substring(at + 1);
    }

	public Session(String id, String token) {
		this.id = id;
		this.token = token;
	}

	public Session(String id, String token, long clock) {
		this.id = id;
		this.token = token;
		this.clock = clock;
	}
}
