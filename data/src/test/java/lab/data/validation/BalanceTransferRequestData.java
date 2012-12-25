package lab.data.validation;

import org.xillium.data.*;
import org.xillium.data.validation.*;
import java.sql.Date;
import lab.data.*;

public class BalanceTransferRequestData implements DataObject {
    @required @subtype("EmailAddress")
    //@size(12)
    public String userName;

    public Date datetime;

    @required @size(20)
    public String sourceAccount;

    @required @size(value=20, truncate=true)
    public String targetAccount;

    @required @subtype("DollarAmount")
    @range(min="0.00", max="10000.00")
    public Double amount;

    @range(min="0", max="5")
    @ranges({
        @range(min="0", inclusive=false),
        @range(min="15", max="75")
    })
    public Integer flags;

    @required
    @values({"credit", "debit", "transfer"})
    public String operation;

    @required
    public CreditCard payment;

    @required
    public StreetAddress shippingAddress;

    @subtype("DayOfWeek")
    public String day;
}
