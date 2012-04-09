package org.xillium.data.validation;

import java.sql.Date;


public abstract class StandardDataTypes {
    @pattern("^[\\w.%+-]+@[\\w.]+\\.[A-Za-z]{2,4}$")
    public String EmailAddress;

    @range(min="01/01/1970", max="01/01/1969")
    public Date FutureDate;

    @range(max="01/01/1970")
    public Date PastDate;

    @pattern("^[\\d]+(\\.[\\d]{1,2})?$")
    public Double DollarAmount;

    @range(min="0", inclusive=false)
    public Integer Age;

    @values({"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"})
    public String DayOfWeek;
}
