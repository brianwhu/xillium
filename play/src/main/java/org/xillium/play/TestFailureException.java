package org.xillium.play;

public class TestFailureException extends Exception {
    @SuppressWarnings("compatibility:-4493609153254097016")
    private static final long serialVersionUID = 1L;
    
    private final int code;

    public TestFailureException(int code, Throwable throwable) {
        super(throwable);
        this.code = code;
    }

    public TestFailureException(int code, String string, Throwable throwable) {
        super(string, throwable);
        this.code = code;
    }

    public TestFailureException(int code, String string) {
        super(string);
        this.code = code;
    }

    public TestFailureException(int code) {
        super();
        this.code = code;
    }
    
    public int getStatusCode() {
        return code;
    }
}
