package lab.data;

import org.xillium.data.*;
import org.xillium.data.validation.*;
import java.sql.Date;

public class Coupon implements DataObject {
    //@required
    public String code;

    public Date expirationDate;

    public String description;
}
