package lab.data;

//import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import java.sql.Date;

// not necessarily an OnymousRequest
public class ShoppingCartCreate implements DataObject {
    @subtype("EmailAddress")
    public String email;

    @required
    public Date transactionDate;

    @required
    public Product[] products;
}
