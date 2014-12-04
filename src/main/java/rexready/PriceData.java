package rexready;

import java.util.EnumMap;
import java.util.Map;

public class PriceData {
	
	private Map<Good, Float> prices = new EnumMap<>(Good.class);
	
	public PriceData() {
		for (Good good : Good.values()) {
			prices.put(good, 0.f);
		}
	}
	
	public float getPrice(Good good) {
		return prices.get(good);
	}
	
	public void setPrice(Good good, float price) {
		prices.put(good, price);
	}
	
}
