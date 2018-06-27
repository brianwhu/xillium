package lab.model;

public class User {
    public String id;
    public String first;
    public String last;
    public String group;
    public String note; // no setter

    public void setFirst(String s) {
        first = s;
    }

    public void setLast(String s) {
        last = s;
    }

    public void setGroup(String s) {
        group = s;
    }

    public void setId(String s) {
        id = s;
    }
}
