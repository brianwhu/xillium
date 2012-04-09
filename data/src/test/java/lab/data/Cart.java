package lab.data;

//import org.xillium.base.etc.Auditable;
import org.xillium.data.*;
import org.xillium.data.validation.*;

public class Cart implements DataObject {
    @required
    public Integer[] productId;

    @required
    public Integer[] quantity;
}
