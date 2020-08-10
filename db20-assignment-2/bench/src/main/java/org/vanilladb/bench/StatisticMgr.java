/*******************************************************************************
 * Copyright 2016, 2018 vanilladb.org contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.vanilladb.bench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.bench.util.BenchProperties;

public class StatisticMgr {
	private static Logger logger = Logger.getLogger(StatisticMgr.class.getName());

	private static final File OUTPUT_DIR;


	static {
		String outputDirPath = BenchProperties.getLoader().getPropertyAsString(StatisticMgr.class.getName()
				+ ".OUTPUT_DIR", null);
		
		if (outputDirPath == null) {
			OUTPUT_DIR = new File(System.getProperty("user.home"), "benchmark_results");
		} else {
			OUTPUT_DIR = new File(outputDirPath);
		}

		// Create the directory if that doesn't exist
		if (!OUTPUT_DIR.exists())
			OUTPUT_DIR.mkdir();
		
		
	}

	private static class TxnStatistic {
		private BenchTransactionType mType;
		private int txnCount = 0;
		private long totalResponseTimeNs = 0;

		public TxnStatistic(BenchTransactionType txnType) {
			this.mType = txnType;
		}

		public BenchTransactionType getmType() {
			return mType;
		}

		public void addTxnResponseTime(long responseTime) {
			txnCount++;
			totalResponseTimeNs += responseTime;
		}

		public int getTxnCount() {
			return txnCount;
		}

		public long getTotalResponseTime() {
			return totalResponseTimeNs;
		}
	}

	private List<TxnResultSet> resultSets = new ArrayList<TxnResultSet>();
	private List<BenchTransactionType> allTxTypes;
	private String fileNamePostfix = "";
	private long recordStartTime = -1;
	
	public StatisticMgr(Collection<BenchTransactionType> txTypes) {
		allTxTypes = new LinkedList<BenchTransactionType>(txTypes);
	}
	
	public StatisticMgr(Collection<BenchTransactionType> txTypes, String namePostfix) {
		allTxTypes = new LinkedList<BenchTransactionType>(txTypes);
		fileNamePostfix = namePostfix;
	}
	
	/**
	 * We use the time that this method is called at as the start time for recording.
	 */
	public synchronized void setRecordStartTime() {
		if (recordStartTime == -1)
			recordStartTime = System.nanoTime();
	}

	public synchronized void processTxnResult(TxnResultSet trs) {
		if (recordStartTime == -1)
			recordStartTime = trs.getTxnEndTime();
		resultSets.add(trs);
	}

	public synchronized void outputReport() {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss"); // E.g. "20180524-200824"
			String fileName = formatter.format(Calendar.getInstance().getTime());
			if (fileNamePostfix != null && !fileNamePostfix.isEmpty())
				fileName += "-" + fileNamePostfix; // E.g. "20200524-200824-postfix"
			
			outputDetailReport(fileName);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (logger.isLoggable(Level.INFO))
			logger.info("Finnish creating tpcc benchmark report");
	}
	
	/*private void outputDetailReport(String fileName) throws IOException {
		Map<BenchTransactionType, TxnStatistic> txnStatistics = new HashMap<BenchTransactionType, TxnStatistic>();
		Map<BenchTransactionType, Integer> abortedCounts = new HashMap<BenchTransactionType, Integer>();
		
		for (BenchTransactionType type : allTxTypes) {
			txnStatistics.put(type, new TxnStatistic(type));
			abortedCounts.put(type, 0);
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(OUTPUT_DIR, fileName + ".txt")))) {
			// First line: total transaction count
			writer.write("# of txns (including aborted) during benchmark period: " + resultSets.size());
			writer.newLine();
			
			// Detail latency report
			for (TxnResultSet resultSet : resultSets) {
				if (resultSet.isTxnIsCommited()) {
					// Write a line: {[Tx Type]: [Latency]}
					writer.write(resultSet.getTxnType() + ": "
							+ TimeUnit.NANOSECONDS.toMillis(resultSet.getTxnResponseTime()) + " ms");
					writer.newLine();
					//writer.write(resultSet.getOutMsg());
					//writer.newLine();
					
					
					// Count transaction for each type
					TxnStatistic txnStatistic = txnStatistics.get(resultSet.getTxnType());
					txnStatistic.addTxnResponseTime(resultSet.getTxnResponseTime());
					
					
				} else {
					writer.write(resultSet.getTxnType() + ": ABORTED");
					writer.newLine();
					
					// Count transaction for each type
					Integer count = abortedCounts.get(resultSet.getTxnType());
					abortedCounts.put(resultSet.getTxnType(), count + 1);
				}
			}
			writer.newLine();
			
			// Last few lines: show the statistics for each type of transactions
			int abortedTotal = 0;
			for (Entry<BenchTransactionType, TxnStatistic> entry : txnStatistics.entrySet()) {
				TxnStatistic value = entry.getValue();
				int abortedCount = abortedCounts.get(entry.getKey());
				abortedTotal += abortedCount;
				long avgResTimeMs = 0;
				
				if (value.txnCount > 0) {
					avgResTimeMs = TimeUnit.NANOSECONDS.toMillis(
							value.getTotalResponseTime() / value.txnCount);
				}
				
				writer.write(value.getmType() + " - committed: " + value.getTxnCount() +
						", aborted: " + abortedCount + ", avg latency: " + avgResTimeMs + " ms");
				writer.newLine();
			}
			
			// Last line: Total statistics
			int finishedCount = resultSets.size() - abortedTotal;
			double avgResTimeMs = 0;
			if (finishedCount > 0) { // Avoid "Divide By Zero"
				for (TxnResultSet rs : resultSets)
					avgResTimeMs += rs.getTxnResponseTime() / finishedCount;
			}
			writer.write(String.format("TOTAL - committed: %d, aborted: %d, avg latency: %d ms", 
					finishedCount, abortedTotal, Math.round(avgResTimeMs / 1000000)));
		}
	}*/
	
	public static Long[] fourDivsion(List<Long> list){
		Long[] param = new Long[list.size()];
		param = list.toArray(param);
        
        BigDecimal[] datas = new BigDecimal[param.length];
        for(int i=0; i<param.length; i++){
            datas[i] = BigDecimal.valueOf(param[i]);
        }
        int len = datas.length;
        Arrays.sort(datas);   
        BigDecimal q1 = null;  
        BigDecimal q2 = null;  
        BigDecimal q3 = null;  
        int index = 0; 

        if(len%2 == 0){ 
            index = new BigDecimal(len).divide(new BigDecimal("4")).intValue();
            q1 = datas[index-1].multiply(new BigDecimal("0.25")).add(datas[index].multiply(new BigDecimal("0.75")));
            q2 = datas[len/2].add(datas[len/2-1]).divide(new BigDecimal("2"));
            index = new BigDecimal(3*(len+1)).divide(new BigDecimal("4")).intValue();
            q3 = datas[index-1].multiply(new BigDecimal("0.75")).add(datas[index].multiply(new BigDecimal("0.25")));
        }else{ 
            q1 = datas[new BigDecimal(len).multiply(new BigDecimal("0.25")).intValue()];
            q2 = datas[new BigDecimal(len).multiply(new BigDecimal("0.5")).intValue()];
            q3 = datas[new BigDecimal(len).multiply(new BigDecimal("0.75")).intValue()];
        }
        
        long Q1 = q1.longValue();
        long Q2 = q2.longValue();
        long Q3 = q3.longValue();
        Long[] Qarray = new Long[] {Q1, Q2, Q3}; 
        return Qarray;
        
    }
	
	private void outputDetailReport(String fileName) throws IOException {
		Map<BenchTransactionType, TxnStatistic> txnStatistics = new HashMap<BenchTransactionType, TxnStatistic>();
		Map<BenchTransactionType, Integer> abortedCounts = new HashMap<BenchTransactionType, Integer>();
		
		for (BenchTransactionType type : allTxTypes) {
			txnStatistics.put(type, new TxnStatistic(type));
			abortedCounts.put(type, 0);
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(OUTPUT_DIR, fileName + ".csv")))) {
			// First line: total transaction count
			writer.write("time(sec), throughput(txs), avg_latency(ms), min(ms), max(ms), 25th_lat(ms), median_lat(ms), 75th_lat(ms)");
			writer.newLine();
			
			long time = 5;
			int txnCount = 0;
			long totalResponseTimeNs = 0;
			List<Long> respTimeList = new ArrayList<>();
			// Detail latency report
			for (TxnResultSet resultSet : resultSets) {
				if (resultSet.isTxnIsCommited()) {
					// Write a line: {[Tx Type]: [Latency]}
					
					long elapseTime = TimeUnit.NANOSECONDS.toSeconds(resultSet.getTxnEndTime() - recordStartTime);
					
					if (time < elapseTime) {
						long throughput = txnCount;
						long avg_latency = totalResponseTimeNs / txnCount;
						Long[] Qarray = fourDivsion(respTimeList);
						writer.write( time + ","
							+ throughput + "," 
							+ TimeUnit.NANOSECONDS.toMillis(avg_latency) + ","
							+ TimeUnit.NANOSECONDS.toMillis(Collections.min(respTimeList)) + ","
							+ TimeUnit.NANOSECONDS.toMillis(Collections.max(respTimeList)) + ","
							+ TimeUnit.NANOSECONDS.toMillis(Qarray[0]) + ","
							+ TimeUnit.NANOSECONDS.toMillis(Qarray[1]) + ","
							+ TimeUnit.NANOSECONDS.toMillis(Qarray[2])
						);
						writer.newLine();
						time += 5;
						txnCount = 0;
						totalResponseTimeNs = 0;
						respTimeList = new ArrayList<>();
						
					}
					respTimeList.add(resultSet.getTxnResponseTime());
					totalResponseTimeNs += resultSet.getTxnResponseTime();
					txnCount++;
					
					// Count transaction for each type
					TxnStatistic txnStatistic = txnStatistics.get(resultSet.getTxnType());
					txnStatistic.addTxnResponseTime(resultSet.getTxnResponseTime());
					
					
				} else {
					writer.write(resultSet.getTxnType() + ": ABORTED");
					writer.newLine();
					
					// Count transaction for each type
					Integer count = abortedCounts.get(resultSet.getTxnType());
					abortedCounts.put(resultSet.getTxnType(), count + 1);
				}
			}
			writer.newLine();
			
			// Last few lines: show the statistics for each type of transactions
			int abortedTotal = 0;
			for (Entry<BenchTransactionType, TxnStatistic> entry : txnStatistics.entrySet()) {
				TxnStatistic value = entry.getValue();
				int abortedCount = abortedCounts.get(entry.getKey());
				abortedTotal += abortedCount;
				long avgResTimeMs = 0;
				
				if (value.txnCount > 0) {
					avgResTimeMs = TimeUnit.NANOSECONDS.toMillis(
							value.getTotalResponseTime() / value.txnCount);
				}
				
				writer.write(value.getmType() + " - committed: " + value.getTxnCount() +
						", aborted: " + abortedCount + ", avg latency: " + avgResTimeMs + " ms");
				writer.newLine();
			}
			
			// Last line: Total statistics
			int finishedCount = resultSets.size() - abortedTotal;
			double avgResTimeMs = 0;
			if (finishedCount > 0) { // Avoid "Divide By Zero"
				for (TxnResultSet rs : resultSets)
					avgResTimeMs += rs.getTxnResponseTime() / finishedCount;
			}
			writer.write(String.format("TOTAL - committed: %d, aborted: %d, avg latency: %d ms", 
					finishedCount, abortedTotal, Math.round(avgResTimeMs / 1000000)));
		}
	}
	
}