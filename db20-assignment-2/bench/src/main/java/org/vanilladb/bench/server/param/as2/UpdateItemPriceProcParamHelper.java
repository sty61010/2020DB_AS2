package org.vanilladb.bench.server.param.as2;
import org.vanilladb.core.remote.storedprocedure.SpResultSet;

import org.vanilladb.core.sql.DoubleConstant;
import org.vanilladb.core.sql.IntegerConstant;
import org.vanilladb.core.sql.Schema;
import org.vanilladb.core.sql.Type;
import org.vanilladb.core.sql.VarcharConstant;
import org.vanilladb.core.sql.storedprocedure.SpResultRecord;
import org.vanilladb.core.sql.storedprocedure.StoredProcedureParamHelper;
public class UpdateItemPriceProcParamHelper extends StoredProcedureParamHelper{
	// Parameters
	private int updateCount;
	private int[] updateItemId;

	private double[] updatePrice;

	// Results
	private String[] itemName;
	private double[] itemPrice;

	public int getUpdateCount() {
		return updateCount;
	}

	public int getUpdateItemId(int index) {
		return updateItemId[index];
	}

	public double getUpdatePrice(int index) {
		return updatePrice[index];
	}
	
	public void setItemName(String s, int idx) {
		itemName[idx] = s;
	}

	public void setItemPrice(double d, int idx) {
		itemPrice[idx] = d;
	}

	@Override
	public void prepareParameters(Object... pars) {
		// Show the contents of paramters
		// System.out.println("Params: " + Arrays.toString(pars));

		updateCount = (Integer) pars[0];
		
		updateItemId = new int[updateCount];
		updatePrice = new double[updateCount];
		
		itemName = new String[updateCount];
		itemPrice = new double[updateCount];

		for (int i = 0; i < updateCount; i++) {
			updateItemId[i] = (Integer) pars[2*i + 1];
			updatePrice[i] = (Double) pars[2*i + 2];

		}
	}

	

	@Override
	public Schema getResultSetSchema() {
		Schema sch = new Schema();
		Type intType = Type.INTEGER;
		Type itemPriceType = Type.DOUBLE;
		Type itemNameType = Type.VARCHAR(24);
		sch.addField("rc", intType);
		int l = itemName.length;
		for (int i = 0; i < l; i++) {
			sch.addField("i_name_" + i, itemNameType);
			sch.addField("i_price_" + i, itemPriceType);
		}
		return sch;
	}

	@Override
	public SpResultRecord newResultSetRecord() {
		SpResultRecord rec = new SpResultRecord();
		rec.setVal("rc", new IntegerConstant(itemName.length));
		for (int i = 0; i < itemName.length; i++) {
			rec.setVal("i_name_" + i, new VarcharConstant(itemName[i], Type.VARCHAR(24)));
			rec.setVal("i_price_" + i, new DoubleConstant(itemPrice[i]));
		}
		return rec;
	}
	

}
