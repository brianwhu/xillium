package lab.data;

import org.xillium.data.*;
import org.xillium.data.validation.*;

public class Cart implements DataObject {
    @required
    public Integer[] productId;

    @required
    public Integer[] quantity;
}
