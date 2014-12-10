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
		goodAmounts.put(good, amount);
	}
	
	public void add(GoodsList other) {
		for (Good good : Good.values()) {
			setAmount(good, getAmount(good) + other.getAmount(good));
		}
	}
	
	public void subtract(GoodsList other) {
		for (Good good : Good.values()) {
			setAmount(good, Math.max(getAmount(good) - other.getAmount(good), 0));
		}
	}
	
	public float getPrice(PriceData prices) {
		float result = 0.f;
		for (Good good : Good.values()) {
			if (getAmount(good) > 0) {
				result += getAmount(good) * prices.getPrice(good);
			}
		}
		return result;
	}
	
	public boolean isFeasible(PriceData priceData) {
		for (Good good : Good.values()) {
			if (getAmount(good) > 0 && !priceData.isAvailable(good)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
}
