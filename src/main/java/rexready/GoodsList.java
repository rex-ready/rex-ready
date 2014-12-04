package rexready;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class GoodsList {
	
	private Map<Good, Integer> goodAmounts = new EnumMap<>(Good.class);
	
	public GoodsList() {
		for (Good good : Good.values()) {
			goodAmounts.put(good, 0);
		}
	}
	
	public int getAmount(Good good) {
		return goodAmounts.get(good);
	}
	
	public void setAmount(Good good, int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("amount is negative");
		}
		goodAmounts.put(good, amount);
	}
	
	public void add(GoodsList other) {
		for (Good good : Good.values()) {
			setAmount(good, getAmount(good) + other.getAmount(good));
		}
	}
	
	public float getPrice(PriceData prices) {
		float result = 0.f;
		for (Good good : Good.values()) {
			result += getAmount(good) * prices.getPrice(good);
		}
		return result;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
}
