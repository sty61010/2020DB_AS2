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
package org.vanilladb.bench.benchmarks.as2.rte;

import java.sql.Connection;

import org.vanilladb.bench.BenchmarkerParameters;
import org.vanilladb.bench.StatisticMgr;
import org.vanilladb.bench.benchmarks.as2.As2BenchConstants;
import org.vanilladb.bench.benchmarks.as2.As2BenchTxnType;
import org.vanilladb.bench.remote.SutConnection;
import org.vanilladb.bench.rte.RemoteTerminalEmulator;
import org.vanilladb.bench.util.BenchProperties;
import org.vanilladb.bench.util.RandomValueGenerator;

public class As2BenchRte extends RemoteTerminalEmulator<As2BenchTxnType> {
	
	private As2BenchTxExecutor executor_read;
	private As2BenchTxExecutor executor_write;
	
	public static final double READ_WRITE_TX_RATE;
	
	static {
		READ_WRITE_TX_RATE = BenchProperties.getLoader().getPropertyAsDouble(
				As2BenchRte.class.getName() + ".READ_WRITE_TX_RATE", 0.5);
	}

	public As2BenchRte(SutConnection conn, StatisticMgr statMgr) {
		super(conn, statMgr);
		executor_read = new As2BenchTxExecutor(new As2ReadItemParamGen());
		executor_write = new As2BenchTxExecutor(new As2UpdateItemPriceParamGen());
	}
	
	protected As2BenchTxnType getNextTxType() {
		RandomValueGenerator rvg = new RandomValueGenerator();
		double rand = rvg.fixedDecimalNumber(3, 0.0, 1.0);
		if(rand < READ_WRITE_TX_RATE) return As2BenchTxnType.UPDATE_ITEM;
		else return As2BenchTxnType.READ_ITEM;
	}
	
	protected As2BenchTxExecutor getTxExeutor(As2BenchTxnType type) {
		switch (type) {
		case READ_ITEM:
			return executor_read;
		default:
			return executor_write;
		}
	}
}
