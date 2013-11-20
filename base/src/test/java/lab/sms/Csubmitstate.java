package lab.sms;

public class Csubmitstate {
    public int code;
    public String text;

    public void setState(State s) {
        code = Integer.parseInt(s.text);
    }

    public void setMsgstate(Msgstate s) {
        text = s.text;
    }

    public void setMsgid(Msgid id) {}

    public void setReserve(Reserve r) {}
}
