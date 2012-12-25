package lab.data;

import org.xillium.data.validation.*;


public class AuthenticateMember extends OnymousRequest {
    @required
    public byte[] credential;
}
