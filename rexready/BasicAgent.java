package rexready;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

public class BasicAgent extends AgentImpl {
    
    private Client[] clients = new Client[8];
    private int[] bidValues = new int[agent.getAuctionNo()];
    
    @Override
    protected void init(ArgEnumerator args) {
	for(int i=0; i<8; i++) {
	    int arrivalPreference = agent.getClientPreference(i, TACAgent.ARRIVAL);
	    int departurePreference = agent.getClientPreference(i, TACAgent.DEPARTURE);
	    int hotelValue = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
	    int e1Value = agent.getClientPreference(i, TACAgent.E1);
	    int e2Value = agent.getClientPreference(i, TACAgent.E2);
	    int e3Value = agent.getClientPreference(i, TACAgent.E3);
	    
	    clients[i] = new Client(arrivalPreference, departurePreference, hotelValue, e1Value, e2Value, e3Value);
	}
    }

    @Override
    public void gameStarted() {
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
     * Allocate how many of flights, hotels and entertainment tickets will be bid on in each auction.
     */
    private void setAuctionAllocations() {
	for(int i=0; i<8; i++) {
	    Client c = clients[i];
	    Preferences preferences = c.getPreferences();
	    
	    //Allocate flight ticket bids.
	    int[] flightDates = pickFlightDates(preferences);
	    // Obtain the auction ID for the flight tickets on the preferred day.
	    int auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flightDates[0]);
	    // Increment the number of tickets required from that auction.
	    agent.setAllocation(auction, agent.getAllocation(auction) + 1);
	    auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, flightDates[1]);
	    agent.setAllocation(auction, agent.getAllocation(auction) + 1);
	    
	    //Allocate hotel room bids.
	    for (int j = flightDates[0]; j < flightDates[1]; j++) {
		int hotelType = inTampaTowers(preferences) ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL;
		auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, hotelType, j);
		agent.setAllocation(auction, agent.getAllocation(auction) + 1);
	    }
	    
	    //Allocate entertainment ticket bids.
	    
	}
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
	for(int i=0; i<agent.getAuctionNo(); i++) {
	    //Amount of tickets allocated as required minus the amount already owned = Amount still needed.
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
     * Calculate whether or not it's worth the effort to pursue rooms in Tampa Towers.
     */
    private boolean inTampaTowers(Preferences preferences) {
	int hotelBonus = preferences.getHotelValue();	
	return hotelBonus > 50;
    }
    
    /**
     * Pick arrival and return date based on balance between incorrect date penalties, ticket prices and entertainment bonuses.
     * @return	[0] = Arrival, [1] = Departure.
     */
    private int[] pickFlightDates(Preferences preferences) {
	int[] ret = {preferences.getArrival(), preferences.getDeparture()};
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
