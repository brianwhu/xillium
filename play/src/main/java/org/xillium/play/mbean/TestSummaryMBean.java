package org.xillium.play.mbean;

public interface TestSummaryMBean {
    public void start();

    public void stop();

    public void reset();

    public void report(String filename);

    public int getThreadCount();

    public double getThroughput();

    public String getSuccessRatio();

    public long getElapsedTime();
}
