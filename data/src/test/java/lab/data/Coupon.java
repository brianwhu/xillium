package lab.data;

import org.xillium.data.*;
import java.sql.Date;

public class Coupon implements DataObject {
    //@required
    public String code;

    public Date expirationDate;

    public String description;
}
