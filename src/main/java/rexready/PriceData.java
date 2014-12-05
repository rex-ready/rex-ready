package rexready;

import java.util.EnumMap;
import java.util.Map;

public class PriceData {
	
	private Map<Good, Float> prices = new EnumMap<>(Good.class);
	private Map<Good, Boolean> availability = new EnumMap<>(Good.class);
	
	public PriceData() {
		for (Good good : Good.values()) {
			prices.put(good, 0.f);
			availability.put(good, false);
		}
	}
	
	public float getPrice(Good good) {
		return prices.get(good);
	}
	
	public void setPrice(Good good, float price) {
		prices.put(good, price);
	}
	
	public boolean isAvailable(Good good) {
		return availability.get(good);
	}
	
	public void setAvailable(Good good, boolean available) {
		this.availability.put(good, available);
	}
	
}
