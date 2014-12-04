package rexready;

public class Optimiser {
	
	public static void main(String[] args) {
		int generation = 0;
		Strategy strategy = new Strategy();
		strategy.setPackage(new ClientPreferences(3, 4, 81, 65, 87, 119), new Package());
		strategy.setPackage(new ClientPreferences(1, 2, 91, 139, 23, 107), new Package());
		strategy.setPackage(new ClientPreferences(1, 2, 69, 177, 22, 189), new Package());
		strategy.setPackage(new ClientPreferences(3, 5, 68, 14, 175, 25), new Package());
		strategy.setPackage(new ClientPreferences(1, 5, 113, 3, 104, 101), new Package());
		strategy.setPackage(new ClientPreferences(2, 3, 144, 153, 154, 108), new Package());
		strategy.setPackage(new ClientPreferences(1, 4, 74, 25, 38, 66), new Package());
		strategy.setPackage(new ClientPreferences(4, 5, 87, 111, 16, 130), new Package());
		
		System.out.println("Generation,Utility");
		
		while (true) {
			Strategy newStrategy = new Strategy(strategy);
			newStrategy.mutate(0.1f);
			if (newStrategy.getUtility() > strategy.getUtility()) {
				strategy = newStrategy;
				System.out.println(generation + "," + strategy.getUtility());
			}
			++generation;
		}
	}
	
}
