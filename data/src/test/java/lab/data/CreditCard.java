package lab.data;

//import org.xillium.base.etc.Auditable;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import java.sql.Date;

public class CreditCard implements DataObject {
    @required @size(16)
    public String cardNumber;

    public Date expirationDate;

    public StreetAddress billingAddress;
}
