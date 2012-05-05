package org.xillium.play.mbean;

import org.xillium.play.TestSuite;

import java.lang.management.*;
import java.util.*;
import java.io.*;
import javax.management.*;


public class TestSummary implements TestSummaryMBean {
	private static final List<TestCaseStats> _stats = new ArrayList<TestCaseStats>();
	private long _clock;
	private TestSuite _suite;

	public TestSummary(TestSuite suite) {
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName("play:type=TestSummary"));
		} catch (JMException x) {
			x.printStackTrace(System.err);
		}
		_suite = suite;
		_clock = System.currentTimeMillis();
	}

	public void addTestCaseStats(TestCaseStats stats) {
		_stats.add(stats);
	}

	public void start() {
		throw new RuntimeException("Not supported");
	}

	public void stop() {
		//System.exit(0);
		_suite.stop();
	}

	public void reset() {
		_clock = System.currentTimeMillis();
	}

	public void report(String filename) {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(filename + ".csv"));
			try {
				pw.println(
					"Test Case,Threads,Success,Failure,Success Ratio,Minimum Latency,Maximum Latency,Average Latency,Throughput"
				);
				Iterator<TestCaseStats> it = _stats.iterator();
				while (it.hasNext()) {
					TestCaseStats stats = it.next();
					pw.print(stats.getName()); pw.print(',');
					pw.print(stats.getThreadCount()); pw.print(',');
					pw.print(stats.getSuccessPeg()); pw.print(',');
					pw.print(stats.getFailurePeg()); pw.print(',');
					pw.print(stats.getSuccessRatio()); pw.print(',');
					pw.print(stats.getLatencyMin()); pw.print(',');
					pw.print(stats.getLatencyMax()); pw.print(',');
					pw.print(stats.getLatencyAvg()); pw.println();
				}
				pw.print("[Total],,,,,,,,"); pw.println(getThroughput());
				pw.println();

				pw.println("Test Case,Occurance,Code,Message");
				it = _stats.iterator();
				while (it.hasNext()) {
					TestCaseStats stats = it.next();
					Map<Integer, TestCaseStats.ErrorSummary> summary = stats.getErrorSummary();
					synchronized (summary) {
						Iterator<Map.Entry<Integer, TestCaseStats.ErrorSummary>> errors = summary.entrySet().iterator();
						while (errors.hasNext()) {
							Map.Entry<Integer, TestCaseStats.ErrorSummary> entry = errors.next();
							TestCaseStats.ErrorSummary es = entry.getValue();
							pw.print(stats.getName()); pw.print(',');
							pw.print(es.count); pw.print(',');
							pw.print(entry.getKey()); pw.print(',');
							pw.print(es.message); pw.println();
						}
					}
				}
			} finally {
				pw.close();
			}
		} catch (IOException x) {
			throw new RuntimeException(x.getMessage(), x);
		}
	}

	public int getThreadCount() {
		int count = 0;
		Iterator<TestCaseStats> it = _stats.iterator();
		while (it.hasNext()) {
			count += it.next().getThreadCount();
		}
		return count;
	}

	public double getThroughput() {
		long success = 0;
		Iterator<TestCaseStats> it = _stats.iterator();
		while (it.hasNext()) {
			success += it.next().getSuccessPeg();
		}
		return success * 1000.00 / (System.currentTimeMillis() - _clock);
	}

	public String getSuccessRatio() {
		long success = 0, failure = 0;
		Iterator<TestCaseStats> it = _stats.iterator();
		while (it.hasNext()) {
			TestCaseStats stats = it.next();
			success += stats.getSuccessPeg();
			failure += stats.getFailurePeg();
		}
		if (success + failure > 0) {
			return String.valueOf(success * 100.00 / (success + failure)) + '%';
		} else {
			return "-";
		}
	}

	public long getElapsedTime() {
		return System.currentTimeMillis() - _clock;
	}
}

