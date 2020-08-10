package org.vanilladb.bench.server.procedure.as2;

import org.vanilladb.bench.server.param.as2.UpdateItemPriceProcParamHelper;
import org.vanilladb.core.query.algebra.Plan;


import org.vanilladb.core.query.algebra.Scan;
import org.vanilladb.core.server.VanillaDb;
import org.vanilladb.core.sql.Schema;
import org.vanilladb.core.sql.Type;
import org.vanilladb.core.sql.VarcharConstant;
import org.vanilladb.core.sql.storedprocedure.SpResultRecord;
import org.vanilladb.core.sql.storedprocedure.StoredProcedure;
import org.vanilladb.core.storage.tx.Transaction;

import org.vanilladb.bench.benchmarks.as2.As2BenchConstants;
import org.vanilladb.core.remote.storedprocedure.SpResultSet;
//package org.vanilladb.core.server.VanillaDb;

public class UpdateItemPriceTxnProc extends StoredProcedure<UpdateItemPriceProcParamHelper>{
	public UpdateItemPriceTxnProc() {
		super(new UpdateItemPriceProcParamHelper());
	}

	@Override
	protected void executeSql() {
		UpdateItemPriceProcParamHelper paramHelper = getParamHelper();
		Transaction tx = getTransaction();
		
		for (int idx = 0; idx < paramHelper.getUpdateCount(); idx++) {
			int iid = paramHelper.getUpdateItemId(idx);
			String sql = "SELECT i_name, i_price FROM item WHERE i_id = " + iid;
//			Planner =
			Plan p = VanillaDb.newPlanner().createQueryPlan(sql, tx);
			Scan s = p.open();
			s.beforeFirst();
			if (s.next()) {
				String name = (String) s.getVal("i_name").asJavaVal();
				double price = (Double) s.getVal("i_price").asJavaVal();
	
				if (price > As2BenchConstants.MAX_PRICE) {
					price = As2BenchConstants.MIN_PRICE;
				} else {
					price += paramHelper.getUpdatePrice(idx);
				}
				sql = "UPDATE item SET i_price = " + price + " WHERE i_id = " + iid;
				paramHelper.setItemName(name, idx);
				paramHelper.setItemPrice(price, idx);
//		        System.out.println(sql);

				VanillaDb.newPlanner().executeUpdate(sql, tx);
			} else
				throw new RuntimeException("Cloud not find item record with i_id = " + iid);

			s.close();
		}
	}
	

}
