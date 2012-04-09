package lab.data;

import org.xillium.data.*;
import org.xillium.data.validation.*;

public class StreetAddress implements DataObject {
    @required
    public String streetAddress1;

    public String streetAddress2;

    @required
    public String city;

    @required
    public String state;

    @required
    @pattern("[\\d]{5}")
    public String zipCode;
}
