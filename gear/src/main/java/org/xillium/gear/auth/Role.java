package org.xillium.gear.auth;

import org.xillium.data.DataObject;


/**
 * A user/merchant role.
 */
public class Role implements DataObject {
	public String id;
	public String roleId;
    public int permission;
    public int prerequisite;
}
