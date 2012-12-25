package lab.data;

import org.xillium.data.*;
import org.xillium.data.validation.*;

public class OnymousRequest implements DataObject {
    @required @subtype("EmailAddress")
    public String email;
}
