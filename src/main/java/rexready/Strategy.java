package rexready;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Strategy {
	
	private final Map<ClientPreferences, Package> packages = new LinkedHashMap<>();
	
	public Strategy() {
	}
	
	public Strategy(Strategy old) {
		this();
		
		for (Map.Entry<ClientPreferences, Package> entry : old.packages.entrySet()) {
			Package pkg = null;
			if (entry.getValue() != null) {
				pkg = new Package(entry.getValue());
			}
			packages.put(entry.getKey(), pkg);
		}
	}

	public void setPackage(ClientPreferences client, Package pkg) {
		packages.put(client, pkg);
	}
	
	public Map<ClientPreferences, Package> getPackages() {
		return Collections.unmodifiableMap(packages);
	}
	
	public int getUtility() {
		int result = 0;
		for (Map.Entry<ClientPreferences, Package> entries : packages.entrySet()) {
			ClientPreferences client = entries.getKey();
			Package pkg = entries.getValue();
			if (pkg != null) {
				result += client.getUtility(pkg);
			}
		}
		return result;
	}

	public float getScore(PriceData prices, GoodsList ownedGoods) {
		GoodsList shoppingList = getShoppingList();
		shoppingList.subtract(ownedGoods);
		if (!shoppingList.isFeasible(prices)) {
			return 0.f;
		}
		else {
			return getUtility() - shoppingList.getPrice(prices);
		}
	}
	
	public void mutate(Random random, float mutationRate) {
		for (Map.Entry<ClientPreferences, Package> entry : packages.entrySet()) {
			if (entry.getValue() == null) {
				if (random.nextFloat() < mutationRate) {
					entry.setValue(new Package(entry.getKey()));
				}
			}
			else {
				if (random.nextFloat() < mutationRate) {
					entry.setValue(null);
				}
				else {
					entry.getValue().mutate(random, mutationRate);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public GoodsList getShoppingList() {
		GoodsList result = new GoodsList();
		for (Package pkg : packages.values()) {
			if (pkg != null) {
				result.add(pkg.getShoppingList());
			}
		}
		return result;
	}
	
}
