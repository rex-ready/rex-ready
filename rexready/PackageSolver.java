package rexready;

public class PackageSolver {

	/**
	 * Calculates the optimal travel package for the given client at current prices.
	 * 
	 * @param client The client to generate a travel package for.
	 * @param currentPrices The current prices of each auction. If a ticket is already owned and not assigned to another client, the corresponding price in <pre>currentPrices</pre> should be set to zero.
	 * @return A travel package that optimises client utility at current prices.
	 */
	public TravelPackage createTravelPackage(Client client, float[] currentPrices) {
		/* Not yet implemented */
		return null;
	}
	
	/**
	 * Calculates the optimal entertainment package for the given client at current prices.
	 * 
	 * @param client The client to generate a travel package for.
	 * @param travelPackage The travel package for the client. The generated entertainment package will take the start and end dates of the travel package into account.
	 * @param currentPrices The current prices of each auction. If a ticket is already owned and not assigned to another client, the corresponding price in `currentPrices` should be set to zero.
	 * @return An entertainment package that optimises client utility at current prices.
	 */
	public EntertainmentPackage createEntertainmentPackage(Client client, TravelPackage travelPackage, float[] currentPrices) {
		/* Not yet implemented */
		return null;
	}
}
