package rexready;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

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

public class BasicAgent extends AgentImpl {

    private Client[] clients = new Client[8];
    boolean[] clientInTT;
    private float[] bidValues = new float[TACAgent.getAuctionNo()];
    
    private Map<String, List<Float>> prices = new HashMap<String, List<Float>>();
    private Map<String, List<Float>> predictedMinimumFlightPrices = new HashMap<String, List<Float>>();
    
    private JFrame graphFrame;
    private JLabel graphLabel;
    
    private FlightPricePredictor flightPredictor = new FlightPricePredictor();
    
    @Override
    protected void init(ArgEnumerator args) {
	System.out.println("Agent start");
	
	for(int i=0; i<TACAgent.getAuctionNo(); i++) {
	    prices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
	    if(i < 8) {
		predictedMinimumFlightPrices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
	    }
	}
    }

    @Override
    public void gameStarted() {
	System.out.println("Game start");
	
	graphFrame = new JFrame();
        graphFrame.setSize(600, 450);
        graphFrame.setVisible(true);
        graphLabel = new JLabel();
        graphFrame.add(graphLabel);
	
	final int bidInterval = 1000;
	//Each bid interval:
	//Flights - Update bids if price near minimum.
	//Hotels - Update bids if near the end of the minute.
	//Entertainment - Update bids immediately.
	//Flights
	new Thread(new Runnable() {
	    public void run() {
		while(agent.getGameTimeLeft() > 0) {
		    //If price is near its' predicted minimum
		    //Update bids		    
		    try {
			Thread.sleep(bidInterval);
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}
	    }
	}).start();
	
	//Hotels
	new Thread(new Runnable() {
	    public void run() {
		boolean hotelsUpdated = false;
		while(agent.getGameTimeLeft() > 0) {
		    //If in the last 10 seconds of a bidding cycle
		    if((agent.getGameTime() % 60000) > 50000) {
			if(hotelsUpdated) {
			    continue;
			}
			//Update hotel bids
			hotelsUpdated = true;
		    } else {
			hotelsUpdated = false;
		    }
		    try {
			Thread.sleep(bidInterval);
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}
	    }
	}).start();
	
	//Entertainment
	new Thread(new Runnable() {
	    public void run() {
		//Update bids
		try {
		    Thread.sleep(bidInterval);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	}).start();
	
//	new Thread(new Runnable() {
//	    public void run() {
//		try {
//		    File file = new File("neededTickets.txt");
//		    if (!file.exists()) {
//			file.createNewFile();
//		    }
//
//		    FileWriter fw = new FileWriter(file.getAbsoluteFile());
//		    BufferedWriter bw = new BufferedWriter(fw);
//
//		    while (agent.getGameTimeLeft() > 0) {
//			Thread.sleep(15000);
//			int[] neededTickets = new int[agent.getAuctionNo()];
//			int index = 0;
//			for (Client c : clients) {
//			    Preferences p = c.getPreferences();
//			    int flightAuction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, p.getArrival());
//			    neededTickets[flightAuction]++;
//			    
//			    for(int i=p.getArrival(); i<p.getDeparture(); i++) {
//				int hotelAuction;
//				if(clientInTT[index]){
//				    hotelAuction = agent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_GOOD_HOTEL, i);
//				} else {
//				    hotelAuction = agent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_CHEAP_HOTEL, i);
//				}
//				neededTickets[hotelAuction]++;
//			    }
//			    index++;
//			}
//			for(int i=0; i<neededTickets.length; i++) {
//			    int owned = agent.getOwn(i);
//			    int difference = neededTickets[i] - owned;
//			    int auctionCategory = agent.getAuctionCategory(i);
//			    if(auctionCategory == TACAgent.CAT_FLIGHT) {
//				bw.write("Flight Tickets: " + neededTickets[i] + " - " + agent.getOwn(i));
//				bw.newLine();
//			    } else if(auctionCategory == TACAgent.CAT_HOTEL) {
//				bw.write("Hotel Tickets: " + neededTickets[i] + " - " + agent.getOwn(i));
//				bw.newLine();
//			    }
//			}
//			bw.write("-----------------\n");
//		    }
//		    bw.close();
//		} catch (IOException | InterruptedException e) {
//		    e.printStackTrace();
//		}
//	    }
//	}).start();
	
	setAuctionAllocations();
	bid();
    }

    @Override
    public void gameStopped() {
	try {
	    DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");
	    Date date = new Date();
	    
	    File chartsFile = new File(dateFormat.format(date) + ".txt");
	    if (!chartsFile.exists()) {
		chartsFile.createNewFile();
	    }
	    File priceFile = new File("prices.txt");
	    if (!priceFile.exists()) {
		priceFile.createNewFile();
	    }

	    FileWriter fwCharts = new FileWriter(chartsFile.getAbsoluteFile());
	    BufferedWriter bwCharts = new BufferedWriter(fwCharts);
	    FileWriter fwPrices = new FileWriter(priceFile.getAbsoluteFile());
	    BufferedWriter bwPrices = new BufferedWriter(fwPrices);
	    for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
		List<Float> priceList = prices.get(TACAgent.getAuctionTypeAsString(i));
		if(TACAgent.getAuctionCategory(i) == TACAgent.CAT_FLIGHT) {
		    String url = createFlightPredictionChart(i, predictedMinimumFlightPrices.get(TACAgent.getAuctionTypeAsString(i)));
		    bwCharts.write(url);
		} else {
		    bwCharts.write(createChart(i));
		}
		bwCharts.newLine();
		
		bwPrices.write("Auction " + i);
		bwPrices.newLine();
		for(Float f : priceList) {
		    bwPrices.write(f + " ");
		}
		bwPrices.newLine();
	    }
	    bwCharts.close();
	    bwPrices.close();
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
	int auctionCategory = TACAgent.getAuctionCategory(auctionID);
	
	System.out.println(TACAgent.getAuctionTypeAsString(auctionID) + " -- " + quote.getAskPrice());
	prices.get(TACAgent.getAuctionTypeAsString(auctionID)).add(quote.getAskPrice());
	
	if(auctionCategory == TACAgent.CAT_FLIGHT) {
	    int t = (int) Math.ceil(agent.getGameTime() / 10000.0);
	    int flightID = auctionID;
	    if (flightPredictor.previousPrices[flightID] == 0)
	    {
		flightPredictor.previousPrices[flightID] = quote.getAskPrice();
	    }
	    else
	    {
		flightPredictor.previousPrices[flightID] = flightPredictor.currentPrices[flightID];
	    }
	    flightPredictor.currentPrices[flightID] = quote.getAskPrice();
	    flightPredictor.updateProbabilityDistribution(flightID, t);
	    float predictedMinPrice = flightPredictor.getProbableMinimumPrice(flightID, t, quote.getAskPrice());
	    System.out.println(flightPredictor.probabilityDistributions[auctionID][0]);
	    System.out.format("Current price, %f,predictedMinimum %f\n", quote.getAskPrice(), predictedMinPrice);
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
	    
	    predictedMinimumFlightPrices.get(TACAgent.getAuctionTypeAsString(auctionID)).add(predictedMinPrice);
	    
	    int flightGraphID = 0;
	    if (auctionID == flightGraphID) {		
//		String url = createFixedWidthChart(flightGraphID, 54);
		String url = createFlightPredictionChart(flightGraphID, predictedMinimumFlightPrices.get(TACAgent.getAuctionTypeAsString(auctionID)));
		Image image = null;
		try {
		    URL imageLocation = new URL(url);
		    image = ImageIO.read(imageLocation);
		} catch (IOException e) {
		    e.printStackTrace();
		}
		graphLabel.setIcon(new ImageIcon(image));
		graphFrame.repaint();
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
	    int auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flightDates[0]);
	    // Increment the number of tickets required from that auction.
	    agent.setAllocation(auction, agent.getAllocation(auction) + 1);
	    auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, flightDates[1]);
	    agent.setAllocation(auction, agent.getAllocation(auction) + 1);

	    // Allocate hotel room bids.
	    for (int j = flightDates[0]; j < flightDates[1]; j++) {
		int hotelType = clientInTT[i] ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL;
		auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, hotelType, j);
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
	    int auction = TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, i);
	    if (agent.getAllocation(auction) < agent.getOwn(auction)) {
		return auction;
	    }
	}
	// If no left, just take the first...
	return TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, inFlight);
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
	for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
	    // Amount of tickets allocated as required minus the amount already
	    // owned = Amount still needed.
	    int amountRequired = agent.getAllocation(i) - agent.getOwn(i);

	    int bidValue = 0;
	    switch (TACAgent.getAuctionCategory(i)) {
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
    
    private String createFixedWidthChart(int auctionID, int width) {
	String typeString = TACAgent.getAuctionTypeAsString(auctionID);
	
	List<Float> priceValues = new ArrayList<Float>(prices.get(typeString));
	float lastValue = priceValues.get(priceValues.size() - 1);
	for(int i=priceValues.size(); i<width; i++) {
	    priceValues.add(lastValue);
	}
	
	return createChart(priceValues, typeString);
    }
    
    private String createChart(int auctionID) {
	String typeString = TACAgent.getAuctionTypeAsString(auctionID);
	
	List<Float> priceValues = new ArrayList<Float>(prices.get(typeString));
	
	return createChart(priceValues, typeString);
    }
    
    private String createChart(List<Float> priceValues, String typeString) {	
	Data data = DataUtil.scaleWithinRange(0, 1000, priceValues);
	Line line = Plots.newLine(data, Color.GREEN);

	LineChart chart = GCharts.newLineChart(line);
        chart.setSize(550, 400);
        chart.setTitle(typeString, Color.WHITE, 14);
        chart.setGrid(10, 10, 3, 2);

        // Defining axis info and styles
        chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, priceValues.size()));
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
    
    private String createFlightPredictionChart(int auctionID, List<Float> predictions) {
	String typeString = TACAgent.getAuctionTypeAsString(auctionID);
	
	List<Float> priceValues = new ArrayList<Float>(prices.get(typeString));
	float lastPriceValue = priceValues.get(priceValues.size() - 1);
	for(int i=priceValues.size(); i<54; i++) {
	    priceValues.add(lastPriceValue);
	}
	List<Float> predictionList = new ArrayList<Float>(predictions);
	float lastPredictionValue = predictionList.get(predictionList.size() - 1);
	for(int i=predictionList.size(); i<54; i++) {
	    predictionList.add(lastPredictionValue);
	}
	
	Data priceData = DataUtil.scaleWithinRange(0, 1000, priceValues);
	Data predictionData = DataUtil.scaleWithinRange(0, 1000, predictionList);
	Line priceLine = Plots.newLine(priceData, Color.GREEN);
	Line predictionLine = Plots.newLine(predictionData, Color.RED);

	LineChart chart = GCharts.newLineChart(priceLine, predictionLine);
        chart.setSize(550, 400);
        chart.setTitle(typeString, Color.WHITE, 14);
        chart.setGrid(10, 10, 3, 2);

        // Defining axis info and styles
        chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, priceValues.size()));
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
