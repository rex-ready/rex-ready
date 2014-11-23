package rexready;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.LinearGradientFill;
import com.googlecode.charts4j.Plots;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

public class BasicAgent extends AgentImpl {

    private Client[] clients = new Client[8];
    boolean[] clientInTT;
    private float[] bidValues = new float[agent.getAuctionNo()];
    
    private Map<String, List<Float>> prices = new HashMap<String, List<Float>>();

    @Override
    protected void init(ArgEnumerator args) {
	System.out.println("Agent start");
	
	for(int i=0; i<TACAgent.getAuctionNo(); i++) {
	    prices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
	}
    }

    @Override
    public void gameStarted() {
	System.out.println("Game start");
	
	//Each time period:
	//Flights - Bids if price near minimum.
	//Hotels - Bid near the end of the minute.
	//Entertainment - Bid immediately.
	new Thread(new Runnable() {
	    public void run() {
		
	    }
	}).start();
	
	new Thread(new Runnable() {
	    public void run() {
		try {
		    File file = new File("neededTickets.txt");
		    if (!file.exists()) {
			file.createNewFile();
		    }

		    FileWriter fw = new FileWriter(file.getAbsoluteFile());
		    BufferedWriter bw = new BufferedWriter(fw);

		    while (agent.getGameTimeLeft() > 0) {
			Thread.sleep(15000);
			int[] neededTickets = new int[agent.getAuctionNo()];
			int index = 0;
			for (Client c : clients) {
			    Preferences p = c.getPreferences();
			    int flightAuction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, p.getArrival());
			    neededTickets[flightAuction]++;
			    
			    for(int i=p.getArrival(); i<p.getDeparture(); i++) {
				int hotelAuction;
				if(clientInTT[index]){
				    hotelAuction = agent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_GOOD_HOTEL, i);
				} else {
				    hotelAuction = agent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_CHEAP_HOTEL, i);
				}
				neededTickets[hotelAuction]++;
			    }
			    index++;
			}
			for(int i=0; i<neededTickets.length; i++) {
			    int owned = agent.getOwn(i);
			    int difference = neededTickets[i] - owned;
			    int auctionCategory = agent.getAuctionCategory(i);
			    if(auctionCategory == TACAgent.CAT_FLIGHT) {
				bw.write("Flight Tickets: " + neededTickets[i] + " - " + agent.getOwn(i));
				bw.newLine();
			    } else if(auctionCategory == TACAgent.CAT_HOTEL) {
				bw.write("Hotel Tickets: " + neededTickets[i] + " - " + agent.getOwn(i));
				bw.newLine();
			    }
			}
			bw.write("-----------------\n");
		    }
		    bw.close();
		} catch (IOException | InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	}).start();
	
	setAuctionAllocations();
	bid();
    }

    @Override
    public void gameStopped() {
	try {
	    File file = new File("finalChartURLs.txt");
	    if (!file.exists()) {
		file.createNewFile();
	    }

	    FileWriter fw = new FileWriter(file.getAbsoluteFile());
	    BufferedWriter bw = new BufferedWriter(fw);
	    for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
		bw.write(createChart(i));
		bw.newLine();
	    }
	    bw.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	
//        Image image = null;
//        try {
//            URL imageLocation = new URL(url);
//            image = ImageIO.read(imageLocation);
//        } catch (IOException e) {
//        	e.printStackTrace();
//        }
// 
//        JFrame frame = new JFrame();
//        frame.setSize(600, 450);
//        JLabel label = new JLabel(new ImageIcon(image));
//        frame.add(label);
//        frame.setVisible(true);
    }

    @Override
    public void auctionClosed(int auction) {
    }
    
    public void quoteUpdated(Quote quote) {
	int auctionID = quote.getAuction();
	int auctionCategory = agent.getAuctionCategory(auctionID);
	
	System.out.println(TACAgent.getAuctionTypeAsString(auctionID) + " -- " + quote.getAskPrice());
	prices.get(TACAgent.getAuctionTypeAsString(auctionID)).add(quote.getAskPrice());
	
	if(auctionCategory == TACAgent.CAT_FLIGHT) {
//	    System.err.println("UPDATED FLIGHT");
	    int alloc = agent.getAllocation(auctionID);
	    int ownedTickets = agent.getOwn(auctionID);
	    int allocDiff = alloc - ownedTickets;
	    if(allocDiff > 0) {
		Bid bid = new Bid(auctionID);
		bidValues[auctionID] = updateFlightPrice(allocDiff, quote);
		System.out.println("Trying to bid at: " + bidValues[auctionID]);
		bid.addBidPoint(allocDiff, bidValues[auctionID]);
		agent.submitBid(bid);
	    }
	} else if(auctionCategory == TACAgent.CAT_HOTEL) {
//	    System.err.println("UPDATED HOTEL");
	    int alloc = agent.getAllocation(auctionID);	//Number of bids allocated
	    int hypotheticalQuantityWon = quote.getHQW();
	    //If some bids were attempted in this auction and the HQW isn't every ticket bid on
	    if(alloc > 0 && quote.hasHQW(agent.getBid(auctionID)) && hypotheticalQuantityWon < alloc) {
		Bid bid = new Bid(auctionID);
		bidValues[auctionID] = updateHotelPrice(quote);
		System.out.println("Trying to bid at: " + bidValues[auctionID]);
		bid.addBidPoint(alloc, bidValues[auctionID]);
		
		agent.submitBid(bid);
	    }
	} else if(auctionCategory == TACAgent.CAT_ENTERTAINMENT) {
//	    System.err.println("UPDATED ENTERTAINMENT");
	    int alloc = agent.getAllocation(auctionID);
	    int ownedTickets = agent.getOwn(auctionID);
	    int allocDiff = alloc - ownedTickets;
	    if(allocDiff != 0) {
		Bid bid = new Bid(auctionID);
		bidValues[auctionID] = updateEntertainmentPrice(allocDiff, quote);
		System.out.println("Trying to bid at: " + bidValues[auctionID]);
		bid.addBidPoint(allocDiff, bidValues[auctionID]);
		agent.submitBid(bid);
	    }
	}
    }

    private float updateFlightPrice(int allocDiff, Quote quote) {
	System.out.println(quote.getAskPrice());
	return quote.getAskPrice();
    }

    private float updateHotelPrice(Quote quote) {
	System.out.println((quote.getAskPrice() + 50.f));
	return quote.getAskPrice() + 50.f;
    }
    
    private float updateEntertainmentPrice(int allocDiff, Quote quote) {
	System.out.println(((quote.getAskPrice() + quote.getBidPrice()) / 2.f));
	return (quote.getAskPrice() + quote.getBidPrice()) / 2.f;
    }

    /**
     * Allocate how many of flights, hotels and entertainment tickets will be
     * bid on in each auction.
     */
    private void setAuctionAllocations() {
	System.out.println("Client | Arrival Day | Departure Day | Hotel Value | Ent 1 | Ent 2 | Ent 3 \n");
	clientInTT = getHotelTypeAssignments();
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
		int hotelType = clientInTT[i] ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL;
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
	int ttCount = 5;	//The number of TT assignments
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
	return 1;
    }

    private int calculateHotelBidValue() {
	return 1;
    }

    private int calculateEntertainmentBidValue() {
	return 1;
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
    
    private String createChart(int auctionID) {
	String typeString = TACAgent.getAuctionTypeAsString(auctionID);
	
	Data data = DataUtil.scaleWithinRange(0, 1000, prices.get(typeString));
	
	Line line = Plots.newLine(data, Color.GREEN);

	LineChart chart = GCharts.newLineChart(line);
        chart.setSize(600, 450);
        chart.setTitle(typeString, Color.WHITE, 14);
        chart.setGrid(10, 10, 3, 2);

        // Defining axis info and styles
        chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, prices.get(typeString).size()));
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, 1000));
        
        AxisLabels xAxisLabel = AxisLabelsFactory.newAxisLabels("Price Changes", 50.0);
        xAxisLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.WHITE, 14, AxisTextAlignment.CENTER));
        
        AxisLabels yAxisLabel = AxisLabelsFactory.newAxisLabels("Prices", 50.0);
        yAxisLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.WHITE, 14, AxisTextAlignment.CENTER));

        // Adding axis info to chart.
        chart.addXAxisLabels(xAxisLabel);
        chart.addYAxisLabels(yAxisLabel); 

        // Defining background and chart fills.
        chart.setBackgroundFill(Fills.newSolidFill(Color.newColor("1F1D1D")));
        LinearGradientFill fill = Fills.newLinearGradientFill(0, Color.newColor("363433"), 100);
        fill.addColorAndOffset(Color.newColor("2E2B2A"), 0);
        chart.setAreaFill(fill);
        String url = chart.toURLString();
	
	return url;
    }
}
