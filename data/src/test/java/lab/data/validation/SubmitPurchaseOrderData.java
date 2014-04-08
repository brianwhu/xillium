package lab.data.validation;

import org.xillium.data.*;
import org.xillium.data.validation.*;
import java.sql.Date;
import java.math.BigDecimal;
import lab.data.*;


public class SubmitPurchaseOrderData implements DataObject {
    @required @subtype("EmailAddress")
    public String userName;

    public String[] aliases;

    public Date transactionDate;

    @required
    public Product[] products;

    @required @subtype("DollarAmount") @range(min="0.00", max="10000.00")
    public double subTotal;

    @range(min="0.00", max="1.00")
    public Double taxRate;

    @range(min="0.00", max="1.00")
    public BigDecimal rebate;

    @required @subtype("DollarAmount") @range(min="0.00", max="10000.00")
    public Double totalAmount;

    @ranges({
        @range(min="0", max="5"),
        @range(min="15", max="75"),
        @range(min="100", inclusive=false)
    })
    public Integer flags;

    @required @values({"credit", "debit", "transfer"})
    public String operation;

    @required
    public CreditCard payment;

    @required
    public StreetAddress shippingAddress;
}
