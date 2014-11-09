package rexready;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

public class BasicAgent extends AgentImpl {

    private Client[] clients = new Client[8];
    private int[] bidValues = new int[agent.getAuctionNo()];

    @Override
    protected void init(ArgEnumerator args) {
	System.out.println("Agent start");
    }

    @Override
    public void gameStarted() {
	System.out.println("Game start");
	setAuctionAllocations();
	bid();
    }

    @Override
    public void gameStopped() {
    }

    @Override
    public void auctionClosed(int auction) {
    }

    /**
     * Allocate how many of flights, hotels and entertainment tickets will be
     * bid on in each auction.
     */
    private void setAuctionAllocations() {
	System.out.println("Client | Arrival Day | Departure Day | Hotel Value | Ent 1 | Ent 2 | Ent 3 \n");
	boolean[] inTT = getHotelTypeAssignments();
	for (int i = 0; i < 8; i++) {
	    int arrivalPreference = agent.getClientPreference(i, TACAgent.ARRIVAL);
	    int departurePreference = agent.getClientPreference(i, TACAgent.DEPARTURE);
	    int hotelValue = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
	    int[] eValues = new int[3];
	    eValues[0] = agent.getClientPreference(i, TACAgent.E1);
	    eValues[1] = agent.getClientPreference(i, TACAgent.E2);
	    eValues[2] = agent.getClientPreference(i, TACAgent.E3);
	    System.out.format("%6d | %11d | %13d | %11d | %5d | %5d | %5d\n", i, arrivalPreference, departurePreference, hotelValue, eValues[0],
		    eValues[1], eValues[2]);
	    clients[i] = new Client(arrivalPreference, departurePreference, hotelValue, eValues[0], eValues[1], eValues[2]);
	    Client c = clients[i];
	    Preferences preferences = c.getPreferences();

	    // Allocate flight ticket bids.
	    int[] flightDates = pickFlightDates(preferences);
	    // Obtain the auction ID for the flight tickets on the preferred
	    // day.
	    int auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flightDates[0]);
	    // Increment the number of tickets required from that auction.
	    agent.setAllocation(auction, agent.getAllocation(auction) + 1);
	    auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, flightDates[1]);
	    agent.setAllocation(auction, agent.getAllocation(auction) + 1);

	    // Allocate hotel room bids.
	    for (int j = flightDates[0]; j < flightDates[1]; j++) {
		int hotelType = inTT[i] ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL;
		auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, hotelType, j);
		agent.setAllocation(auction, agent.getAllocation(auction) + 1);
	    }

	    // Allocate entertainment ticket bids.
	    for (int e = 1; e < 4; e++) {
		auction = bestEntDay(flightDates[0], flightDates[1], e);
		agent.setAllocation(auction, agent.getAllocation(auction) + 1);
	    }
	}
    }

    private boolean[] getHotelTypeAssignments() {
	boolean[] inTampa = new boolean[8];
	Map<Integer, Integer> hotelValues = new HashMap<Integer, Integer>();
	for (int i = 0; i < 8; i++) {
	    hotelValues.put(agent.getClientPreference(i, TACAgent.HOTEL_VALUE), i);
	}
	Map<Integer, Integer> sortedHotelValues = new TreeMap<Integer, Integer>(hotelValues);
	
	ArrayList<Integer> topCustomers = new ArrayList<Integer>(sortedHotelValues.values());
	Collections.reverse(topCustomers);
	int ttCount = 4;	//The number of TT assignments
	for(int i=0; i<ttCount; i++) {
	    inTampa[topCustomers.get(i)] = true;
	}

	return inTampa;
    }
    
    private int bestEntDay(int inFlight, int outFlight, int type) {
	for (int i = inFlight; i < outFlight; i++) {
	    int auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, i);
	    if (agent.getAllocation(auction) < agent.getOwn(auction)) {
		return auction;
	    }
	}
	// If no left, just take the first...
	return agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, inFlight);
    }

    private int calculateFlightBidValue() {
	return 1000;
    }

    private int calculateHotelBidValue() {
	return 200;
    }

    private int calculateEntertainmentBidValue() {
	return 0;
    }

    private void bid() {
	for (int i = 0; i < agent.getAuctionNo(); i++) {
	    // Amount of tickets allocated as required minus the amount already
	    // owned = Amount still needed.
	    int amountRequired = agent.getAllocation(i) - agent.getOwn(i);

	    int bidValue = 0;
	    switch (agent.getAuctionCategory(i)) {
	    case TACAgent.CAT_FLIGHT:
		bidValue = calculateFlightBidValue();
		break;
	    case TACAgent.CAT_HOTEL:
		bidValue = calculateHotelBidValue();
		break;
	    case TACAgent.CAT_ENTERTAINMENT:
		bidValue = calculateEntertainmentBidValue();
		break;
	    }

	    Bid bid = new Bid(i);
	    bid.addBidPoint(amountRequired, bidValue);
	    agent.submitBid(bid);
	}
    }

    /**
     * Pick arrival and return date based on balance between incorrect date
     * penalties, ticket prices and entertainment bonuses.
     * 
     * @return [0] = Arrival, [1] = Departure.
     */
    private int[] pickFlightDates(Preferences preferences) {
	int[] ret = { preferences.getArrival(), preferences.getDeparture() };
	return ret;
    }

    @Override
    public void bidUpdated(Bid bid) {
	System.err.println("Bid " + bid.getID() + " updated.");
	System.err.println("Auction: " + bid.getAuction());
	System.err.println("State: " + bid.getProcessingStateAsString());
    }

    @Override
    public void bidRejected(Bid bid) {
	System.err.println("Bid " + bid.getID() + " rejected.");
    }

    @Override
    public void bidError(Bid bid, int error) {
	System.err.println("Bid " + bid.getID() + " error. - " + agent.commandStatusToString(error));
    }
}
