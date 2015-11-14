package lab.data;

import org.xillium.data.*;
import org.xillium.data.validation.*;

public class Product implements DataObject {
    @required
    public String identity;

    public String name;

    public String description;

    @required
    public Double price;

    @required
    public Integer quantity;

    public Coupon[] coupons;
}
