package rexready.gui;

import javax.swing.table.AbstractTableModel;

import rexready.Good;
import rexready.PriceData;
import se.sics.tac.aw.TACAgent;

public class MarketplaceTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;
	
	private PriceData priceData;
	
	public void setPriceData(PriceData priceData) {
		this.priceData = priceData;
		this.fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return Good.values().length;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}
	
	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Name";
		case 1:
			return "Price";
		case 2:
			return "Available";
		default:
			return null;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return TACAgent.getAuctionTypeAsString(rowIndex);
		case 1:
			return priceData == null ? 0.0 : priceData.getPrice(Good.values()[rowIndex]);
		case 2:
			return priceData == null ? false : priceData.isAvailable(Good.values()[rowIndex]);
		default:
			return null;
		}
	}
	
}
