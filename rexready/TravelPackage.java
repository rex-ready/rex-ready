package rexready;

public class TravelPackage {

    private int arrivalDate, departureDate, expectedUtility;
    private boolean tampaTowers;

    public TravelPackage(int arrivalDate, int departureDate, int expectedUtility, boolean tampaTowers) {
	this.arrivalDate = arrivalDate;
	this.departureDate = departureDate;
	this.expectedUtility = expectedUtility;
	this.tampaTowers = tampaTowers;
    }

    public int getArrivalDate() {
        return arrivalDate;
    }

    public int getDepartureDate() {
        return departureDate;
    }
    
    public int getExpectedUtility() {
    	return expectedUtility;
    }

    public boolean includesTampaTowers() {
        return tampaTowers;
    }
}
