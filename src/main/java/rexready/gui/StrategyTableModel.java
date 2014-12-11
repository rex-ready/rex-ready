package rexready.gui;

import java.util.Map;

import javax.swing.table.AbstractTableModel;

import rexready.ClientPreferences;
import rexready.EntertainmentType;
import rexready.GoodsList;
import rexready.Package;
import rexready.PriceData;
import rexready.Strategy;

public class StrategyTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;
	
	private static final String[] columnNames = {
		"PrefArrival", "PrefDeparture", "HotelBonus", "E1", "E2", "E3",
		"ActualArrival", "ActualDeparure", "GoodHotel", "E1?", "E2?", "E3?", "Utility", "Cost"
	};
	private Object[][] values = new Object[8][14];

	public void update(Strategy strategy, PriceData priceData, GoodsList ownedGoods) {
		int i = 0;
		for (Map.Entry<ClientPreferences, Package> entry : strategy.getPackages().entrySet()) {
			ClientPreferences client = entry.getKey();
			Package pkg = entry.getValue();

			values[i][0] = client.arrival;
			values[i][1] = client.departure;
			values[i][2] = client.hotelValue;
			values[i][3] = client.e1Value;
			values[i][4] = client.e2Value;
			values[i][5] = client.e3Value;

			if (pkg == null) {
				for (int j = 6; j <= 13; ++j) {
					values[i][j] = "-";
				}
			}
			else {
				values[i][6] = pkg.getArrivalDate();
				values[i][7] = pkg.getDepartureDate();
				values[i][8] = pkg.isGoodHotel();
				values[i][9] = pkg.contains(EntertainmentType.values()[0]);
				values[i][10] = pkg.contains(EntertainmentType.values()[1]);
				values[i][11] = pkg.contains(EntertainmentType.values()[2]);
				values[i][12] = client.getUtility(pkg);
				GoodsList shoppingList = pkg.getShoppingList();
				shoppingList.subtract(ownedGoods);
				values[i][13] = shoppingList.getPrice(priceData);
			}
			
			++i;
		}
		fireTableDataChanged();
	}
	
	@Override
	public int getRowCount() {
		return 8;
	}

	@Override
	public int getColumnCount() {
		return 14;
	}
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return values[rowIndex][columnIndex];
	}

}
