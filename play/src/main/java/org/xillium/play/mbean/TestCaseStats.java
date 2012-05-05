package org.xillium.play.mbean;

import java.lang.management.*;
import java.util.*;
import javax.management.*;

import org.xillium.play.TestFailureException;


public class TestCaseStats implements TestCaseStatsMBean {
	public static class ErrorSummary {
		String message;
		long count;
	}

	private int numThreads;
	private long successPeg;
	private long failurePeg;
	private long latencyMin = Integer.MAX_VALUE;
	private long latencyMax;
	private double latencyAvg;
	private long _latency;
	private String _name;
	private final Map<Integer, ErrorSummary> _errors = new HashMap<Integer, ErrorSummary>();

	public TestCaseStats(String name) {
		this._name = name;
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName("play:type=TestCaseStats,name="+name));
		} catch (JMException x) {
			x.printStackTrace(System.err);
		}
	}

	public synchronized void updateThreadCount(int update) {
		numThreads += update;
	}

	public synchronized void addSuccessPeg(long latency) {
		++successPeg;
		if (latency < latencyMin) {
			latencyMin = latency;
		}
		if (latency > latencyMax) {
			latencyMax = latency;
		}
		_latency += latency;
		latencyAvg = _latency / (double)successPeg;
	}

	public synchronized void addFailurePeg(TestFailureException x) {
		++failurePeg;

		Integer code = x.getStatusCode();
		ErrorSummary summary = _errors.get(code);
		if (summary != null) {
			++summary.count;
			if (summary.message == null) {
				summary.message = x.getMessage();
			}
		} else {
			summary = new ErrorSummary();
			summary.count = 1;
			summary.message = x.getMessage();
			_errors.put(code, summary);
		}
	}

	public String getName() {
		return _name;
	}

	public synchronized int getThreadCount() {
		return numThreads;
	}

	public synchronized long getSuccessPeg() {
		return successPeg;
	}

	public synchronized long getFailurePeg() {
		return failurePeg;
	}

	public synchronized String getSuccessRatio() {
		long total = successPeg + failurePeg;
		if (total > 0) {
			return String.valueOf(successPeg*100/(successPeg + failurePeg)) + '%';
		} else {
			return "-";
		}
	}

	public synchronized long getLatencyMax() {
		return latencyMax;
	}

	public synchronized long getLatencyMin() {
		return latencyMin;
	}

	public synchronized double getLatencyAvg() {
		return latencyAvg;
	}

	public Map<Integer, ErrorSummary> getErrorSummary() {
		return _errors;
	}

	public void report(StringBuilder sb) {
		sb.append("\n\t    Successes: ").append(successPeg)
		  .append("\n\t     Failures: ").append(failurePeg)
		  .append("\n\tLatency - min: ").append(latencyMin)
		  .append("\n\t          max: ").append(latencyMax)
		  .append("\n\t      average: ").append(Math.round(latencyAvg))
		  .append("\n\t # of threads: ").append(numThreads);
	}

	private boolean _reporting = true;
}

