package org.xillium.play.mbean;

public interface TestCaseStatsMBean {
    /**
     * @return the number of parallel threads in this test case
     */
    public int getThreadCount();

    /**
     * @return total number of successful test case invocations
     */
    public long getSuccessPeg();

    /**
     * @return total number of failed test case invocations
     */
    public long getFailurePeg();

    /**
     * @return test case success ratio
     */
    public String getSuccessRatio();

    /**
     * @return observed minimum latency in milliseconds
     */
    public long getLatencyMin();

    /**
     * @return observed maximum latency in milliseconds
     */
    public long getLatencyMax();

    /**
     * @return observed average latency in milliseconds
     */
    public double getLatencyAvg();
}
