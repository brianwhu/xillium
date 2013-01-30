package lab.data;

import org.xillium.data.*;
import org.xillium.data.validation.*;

public class StreetAddress implements DataObject {
    public static enum State {
        CA,
        AL,
        FL
    }

    @required
    public String streetAddress1;

    public String streetAddress2;

    @required
    public String city;

    @required
    public State state;

    @required
    @pattern("[\\d]{5}")
    public String zipCode;
}
