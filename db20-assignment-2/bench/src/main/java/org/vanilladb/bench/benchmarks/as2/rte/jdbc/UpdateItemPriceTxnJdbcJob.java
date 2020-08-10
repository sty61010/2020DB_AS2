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
package org.vanilladb.bench.benchmarks.as2.rte.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.bench.benchmarks.as2.As2BenchConstants;
import org.vanilladb.bench.remote.SutResultSet;
import org.vanilladb.bench.remote.jdbc.VanillaDbJdbcResultSet;
import org.vanilladb.bench.rte.jdbc.JdbcJob;

public class UpdateItemPriceTxnJdbcJob implements JdbcJob {
	private static Logger logger = Logger.getLogger(UpdateItemPriceTxnJdbcJob.class
			.getName());
	
	private static final double maxPrice;
	private static final double minPrice; 
	
	static {
		maxPrice = As2BenchConstants.MAX_PRICE;
		minPrice = As2BenchConstants.MIN_PRICE;
	}
	
	@Override
	public SutResultSet execute(Connection conn, Object[] pars) throws SQLException {
		// Parse parameters
		int UpdateCount = (Integer) pars[0];
		int[] itemIds = new int[UpdateCount];
		double[] priceRaise = new double[UpdateCount];
		for (int i = 0; i < UpdateCount; i++) {
			itemIds[i] = (Integer) pars[i*2 + 1];
			priceRaise[i] = (Double) pars[i*2 + 2];
		}
		
		// Output message
		StringBuilder outputMsg = new StringBuilder("[");
		// Execute logic
		try {
			Statement statement = conn.createStatement();
			ResultSet rs = null;
			for (int i = 0; i < 10; i++) {
				String sql = "SELECT i_name, i_price FROM item WHERE i_id = " + itemIds[i];
				rs = statement.executeQuery(sql);
				rs.beforeFirst();
				if (rs.next()) {
					outputMsg.append(String.format("id: %d, price: %f", itemIds[i], rs.getDouble("i_price")));
					//outputMsg.append(String.format("'%s', ", rs.getString("i_name")));
					double curPrice = rs.getDouble("i_price");
					if(curPrice > maxPrice) {
						curPrice = minPrice;
					}
					else {
						curPrice = curPrice + priceRaise[i];						
					}
					sql = "UPDATE item SET i_price = " + curPrice + " WHERE i_id = " + itemIds[i];
					statement.executeUpdate(sql);
				} else
					throw new RuntimeException("cannot find the record with i_id = " + itemIds[i]);
				rs.close();
			}
			conn.commit();
			
			outputMsg.deleteCharAt(outputMsg.length() - 2);
			outputMsg.append("]");
			
			return new VanillaDbJdbcResultSet(true, outputMsg.toString());
		} catch (Exception e) {
			if (logger.isLoggable(Level.WARNING))
				logger.warning(e.toString());
			return new VanillaDbJdbcResultSet(false, "");
		}
	}
}
