package rexready;

public class TravelPackage {

    private int arrivalDate, departureDate;
    private boolean tampaTowers;

    public TravelPackage(int arrivalDate, int departureDate, boolean tampaTowers) {
	this.arrivalDate = arrivalDate;
	this.departureDate = departureDate;
	this.tampaTowers = tampaTowers;
    }

    public int getArrivalDate() {
        return arrivalDate;
    }

    public int getDepartureDate() {
        return departureDate;
    }

    public boolean includesTampaTowers() {
        return tampaTowers;
    }
}
