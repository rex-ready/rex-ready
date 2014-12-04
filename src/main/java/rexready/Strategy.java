package rexready;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Strategy {
	
	private final Map<ClientPreferences, Package> packages = new LinkedHashMap<>();
	
	public Strategy() {
	}
	
	public Strategy(Strategy old) {
		this();
		
		for (Map.Entry<ClientPreferences, Package> entry : old.packages.entrySet()) {
			packages.put(entry.getKey(), new Package(entry.getValue()));
		}
	}

	public void setPackage(ClientPreferences client, Package pkg) {
		packages.put(client, pkg);
	}
	
	public int getUtility() {
		int result = 0;
		for (Map.Entry<ClientPreferences, Package> entries : packages.entrySet()) {
			ClientPreferences client = entries.getKey();
			Package pkg = entries.getValue();
			result += client.getUtility(pkg);
		}
		return result;
	}
	
	public void mutate(float mutationRate) {
		for (Package pkg : packages.values()) {
			pkg.mutate(mutationRate);
		}
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public ShoppingList getShoppingList() {
		ShoppingList result = new ShoppingList();
		for (Package pkg : packages.values()) {
			result.add(pkg.getShoppingList());
		}
		return result;
	}
	
}
