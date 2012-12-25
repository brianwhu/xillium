package lab.data;

//import org.xillium.base.etc.Auditable;
import org.xillium.data.validation.*;

public class Member extends OnymousRequest {
    @required
    public String firstName;

    @required
    public String lastName;

    public String telephone;

    public Integer level;
}
