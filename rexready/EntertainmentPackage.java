package rexready;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntertainmentPackage {

    private List<EntertainmentTicket> tickets = new ArrayList<EntertainmentTicket>();

    public boolean feasible(TravelPackage travelPackage) {
	Set<Integer> dates = new HashSet<Integer>();
	for (EntertainmentTicket e : tickets) {
	    if (dates.add(e.date)) {
		return false;
	    }
	    if (e.date < travelPackage.getArrivalDate() || e.date > travelPackage.getDepartureDate()) {
		return false;
	    }
	}
	return true;
    }

    /**
     * @return Boolean array with following values:
     * [0] = E1 present in package
     * [1] = E2 present in package
     * [2] = E3 present in package
     */
    public boolean[] includesEntertainmentTypes() {
	boolean includesE1 = false;
	boolean includesE2 = false;
	boolean includesE3 = false;
	for (EntertainmentTicket e : tickets) {
	    switch (e.type) {
	    case E1:
		includesE1 = true;
		break;
	    case E2:
		includesE2 = true;
		break;
	    case E3:
		includesE3 = true;
		break;
	    }
	}
	boolean[] includes = { includesE1, includesE2, includesE3 };
	return includes;
    }
}
