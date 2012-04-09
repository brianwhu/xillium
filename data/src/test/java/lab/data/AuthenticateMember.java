package lab.data;

import org.xillium.data.*;
import org.xillium.data.validation.*;
import java.sql.Date;


public class AuthenticateMember extends OnymousRequest {
    @required
    public byte[] credential;
}
