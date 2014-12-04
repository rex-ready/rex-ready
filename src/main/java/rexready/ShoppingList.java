package rexready;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class ShoppingList {
	
	private Map<Good, Integer> goodAmounts = new EnumMap<>(Good.class);
	
	public ShoppingList() {
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
	
	public void add(ShoppingList other) {
		for (Good good : Good.values()) {
			setAmount(good, getAmount(good) + other.getAmount(good));
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
}
