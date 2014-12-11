package rexready;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Optimiser {
	
	private Set<ClientPreferences> clients = new HashSet<ClientPreferences>();
	private Strategy strategy = new Strategy();
	
	public void addClient(ClientPreferences client) {
		clients.add(client);
		strategy.setPackage(client, new Package(client));
	}
	
	public Strategy optimise(PriceData prices, GoodsList ownedGoods, long timeout, float mutationRate) {
		Random r = new Random();
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < startTime + timeout) {
			Strategy newStrategy = new Strategy(strategy);
			for (int i = 0; i < 10; ++i) {
				newStrategy.mutate(r, mutationRate);
				if (newStrategy.getScore(prices, ownedGoods) > strategy.getScore(prices, ownedGoods)) {
					strategy = newStrategy;
					break;
				}
			}
		}
		return strategy;
	}
	
	public static void main(String[] args) {
		Optimiser optimiser = new Optimiser();
		optimiser.addClient(new ClientPreferences(3, 4, 81, 65, 87, 119));
		optimiser.addClient(new ClientPreferences(1, 2, 91, 139, 23, 107));
		optimiser.addClient(new ClientPreferences(1, 2, 69, 177, 22, 189));
		optimiser.addClient(new ClientPreferences(3, 5, 68, 14, 175, 25));
		optimiser.addClient(new ClientPreferences(1, 5, 113, 3, 104, 101));
		optimiser.addClient(new ClientPreferences(2, 3, 144, 153, 154, 108));
		optimiser.addClient(new ClientPreferences(1, 4, 74, 25, 38, 66));
		optimiser.addClient(new ClientPreferences(4, 5, 87, 111, 16, 130));
		
		PriceData prices = new PriceData();
		prices.setPrice(Good.INFLIGHT_1, 323.f);
		prices.setPrice(Good.INFLIGHT_2, 408.f);
		prices.setPrice(Good.INFLIGHT_3, 209.f);
		prices.setPrice(Good.INFLIGHT_4, 342.f);
		prices.setPrice(Good.OUTFLIGHT_2, 356.f);
		prices.setPrice(Good.OUTFLIGHT_3, 380.f);
		prices.setPrice(Good.OUTFLIGHT_4, 354.f);
		prices.setPrice(Good.OUTFLIGHT_5, 454.f);
		prices.setPrice(Good.CHEAP_HOTEL_1, 88.f);
		prices.setPrice(Good.CHEAP_HOTEL_2, 348.f);
		prices.setPrice(Good.CHEAP_HOTEL_3, 100.f);
		prices.setPrice(Good.CHEAP_HOTEL_4, 94.f);
		prices.setPrice(Good.GOOD_HOTEL_1, 100.f);
		prices.setPrice(Good.GOOD_HOTEL_2, 282.f);
		prices.setPrice(Good.GOOD_HOTEL_3, 100.f);
		prices.setPrice(Good.GOOD_HOTEL_4, 100.f);
		prices.setPrice(Good.ALLIGATOR_1, 96.3048f);
		prices.setPrice(Good.ALLIGATOR_2, 102.1501f);
		prices.setPrice(Good.ALLIGATOR_3, 90.8555f);
		prices.setPrice(Good.ALLIGATOR_4, 56.3069f);
		prices.setPrice(Good.AMUSEMENT_1, 10.f);
		prices.setPrice(Good.AMUSEMENT_2, 85.2731f);
		prices.setPrice(Good.AMUSEMENT_3, 65.3964f);
		prices.setPrice(Good.AMUSEMENT_4, 72.7169f);
		prices.setPrice(Good.MUSEUM_1, 110.3333f);
		prices.setPrice(Good.MUSEUM_2, 105.3714f);
		prices.setPrice(Good.MUSEUM_3, 66.3018f);
		prices.setPrice(Good.MUSEUM_4, 60.2232f);
		
		GoodsList ownedGoods = new GoodsList();
		
		long startTime = System.currentTimeMillis();
		
		Strategy strategy = optimiser.optimise(prices, ownedGoods, 1000, 0.2f);
		System.out.println(strategy);
		System.out.println("Score: " + strategy.getScore(prices, ownedGoods));
		System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + "ms");
	}
	
}
