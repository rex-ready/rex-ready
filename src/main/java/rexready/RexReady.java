package rexready;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

public class RexReady extends AgentImpl {
	
	private FlightPricePredictor flightPredictor = new FlightPricePredictor();
	private HotelPricePredictor hotelPredictor = new HotelPricePredictor();
	private EntertainmentPricePredictor entertainmentPredictor = new EntertainmentPricePredictor();
	private Optimiser optimiser = new Optimiser();

	@Override
	protected void init(ArgEnumerator args) {
		System.out.println("init");
	}

	@Override
	public void gameStarted() {
		System.out.println("gameStarted");
		System.out.println("Client | Arrival Day | Departure Day | Hotel Value | Ent 1 | Ent 2 | Ent 3 \n");
		for (int i = 0; i < 8; i++) {
			int arrivalPreference = agent.getClientPreference(i, TACAgent.ARRIVAL);
			int departurePreference = agent.getClientPreference(i, TACAgent.DEPARTURE);
			int hotelValue = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
			int[] eValues = new int[3];
			eValues[0] = agent.getClientPreference(i, TACAgent.E1);
			eValues[1] = agent.getClientPreference(i, TACAgent.E2);
			eValues[2] = agent.getClientPreference(i, TACAgent.E3);
			System.out.format("%6d | %11d | %13d | %11d | %5d | %5d | %5d\n", i, arrivalPreference, departurePreference, hotelValue, eValues[0], eValues[1], eValues[2]);
			optimiser.addClient(new ClientPreferences(arrivalPreference, departurePreference, hotelValue, eValues[0], eValues[1], eValues[2]));
		}
	}

	@Override
	public void gameStopped() {
		System.out.println("gameStopped");
	}
	
	@Override
	public void quoteUpdated(Quote quote) {
		System.out.println("quoteUpdated");
		System.out.println(agent.getGameTime());
		
		int t = (int) Math.ceil(agent.getGameTime() / 10000.f);
		
		switch (TACAgent.getAuctionCategory(quote.getAuction())) {
		case TACAgent.CAT_FLIGHT:
			flightPredictor.previousPrices[quote.getAuction()] = flightPredictor.currentPrices[quote.getAuction()];
			flightPredictor.currentPrices[quote.getAuction()] = quote.getAskPrice();
			flightPredictor.updateProbabilityDistribution(quote.getAuction(), t);
			break;
		case TACAgent.CAT_HOTEL:
			hotelPredictor.previousPrices[quote.getAuction() - 8] = hotelPredictor.currentPrices[quote.getAuction() - 8];
			hotelPredictor.currentPrices[quote.getAuction() - 8] = quote.getAskPrice();
			break;
		case TACAgent.CAT_ENTERTAINMENT:
			entertainmentPredictor.previousPrices[quote.getAuction() - 16] = entertainmentPredictor.currentPrices[quote.getAuction() - 16];
			entertainmentPredictor.currentPrices[quote.getAuction() - 16] = quote.getAskPrice();
			break;
		}
		
		if (quote.getAuction() == 7) {
			hotelPredictor.updateDeltas(t);
			entertainmentPredictor.updateDeltas(t);
			PriceData priceData = new PriceData();
			for (int i = 0; i < TACAgent.getAuctionNo(); ++i) {
				switch (TACAgent.getAuctionCategory(i)) {
				case TACAgent.CAT_FLIGHT:
					priceData.setPrice(Good.values()[i], flightPredictor.getProbableMinimumPrice(i, (int) (agent.getGameTime() / 10000.f), agent.getQuote(i).getAskPrice()));
					break;
				case TACAgent.CAT_HOTEL:
					System.out.println("Delta for hotel " + i + "=" + hotelPredictor.deltas[i - 8]);
					if (agent.getQuote(i).isAuctionClosed()) {
						priceData.setPrice(Good.values()[i], Float.MAX_VALUE);
					}
					else {
						priceData.setPrice(Good.values()[i], agent.getQuote(i).getAskPrice() + hotelPredictor.deltas[i - 8]);
					}
					break;
				case TACAgent.CAT_ENTERTAINMENT:
					System.out.println("Delta for entertainment " + i + "=" + entertainmentPredictor.deltas[i - 16]);
					priceData.setPrice(Good.values()[i], agent.getQuote(i).getAskPrice() + entertainmentPredictor.deltas[i - 16]);
					break;
				}
			}
			for (Good good : Good.values()) {
				System.out.println("Predicted price of " + good.name() + " is " + priceData.getPrice(good));
			}
			GoodsList ownedGoods = new GoodsList();
			for (Good good : Good.values()) {
				ownedGoods.setAmount(good, agent.getOwn(good.ordinal()));
			}
			Strategy strategy = optimiser.optimise(priceData, ownedGoods, 5000, 0.2f);
			System.out.println(strategy);
			System.out.println("Projected score: " + strategy.getScore(priceData, ownedGoods));
			agent.clearAllocation();
			// TODO: Clear old bids
			for (Good good : Good.values()) {
				agent.setAllocation(good.ordinal(), strategy.getShoppingList().getAmount(good));
				System.out.println("Allocating " + strategy.getShoppingList().getAmount(good) + " " + good.name() + "s");
			}
			System.out.println("Bid now");
			
			if (agent.getGameTime() > 30000) {
				// FLIGHT BIDDING
				float maxThreshold = 100.f;
				float minThreshold = 30.f;
				float threshold = minThreshold + ((agent.getGameTime() / (1.f * agent.getGameLength())) * (maxThreshold - minThreshold));
				
				for (int i = 0; i < 8; ++i) {
					int alloc = agent.getAllocation(i);
					int ownedTickets = agent.getOwn(i);
					int probablyOwnedTickets = agent.getProbablyOwn(i);
					
					int diff = alloc - ownedTickets - probablyOwnedTickets;
					
					if((agent.getQuote(i).getAskPrice()-priceData.getPrice(Good.values()[i])) < threshold) {
						if(diff > 0) {
							float bidPrice = agent.getQuote(i).getAskPrice() + 50;
							System.out.println("Bid on flight " + i + " for " + bidPrice);
							Bid bid = new Bid(i);
							bid.addBidPoint(diff, bidPrice);
							agent.submitBid(bid);
						}
					}
				}
				// END FLIGHT BIDDING
				
				// ENTERTAINMENT BIDDING
				for (int i = 16; i < 28; ++i) {
					int alloc = agent.getAllocation(i);
					int ownedTickets = agent.getOwn(i);
					int probablyOwnedTickets = agent.getProbablyOwn(i);
					int diff = alloc - ownedTickets - probablyOwnedTickets;
					if (diff > 0) {
						float bidPrice = agent.getQuote(i).getAskPrice();
						System.out.println("Bid on entertainment " + i + " for " + bidPrice);
						Bid bid = new Bid(i);
						bid.addBidPoint(diff, bidPrice);
						agent.submitBid(bid);
					}
					if (diff < 0) {
						float sellPrice = Math.max(agent.getQuote(i).getAskPrice() - 10, 60);
						System.out.println("Selling unneeded entertainment " + i + " for " + sellPrice);
						Bid bid = new Bid(i);
						bid.addBidPoint(diff, sellPrice);
						agent.submitBid(bid);
					}
				}
				// END ENTERTAINMENT BIDDING
				
				if (agent.getGameTime() % 60000 > 50000) {
					System.out.println("Bid for hotels now");
					for (int i = 8; i < 16; ++i) {
						int alloc = agent.getAllocation(i);
						int ownedTickets = agent.getOwn(i);
						int probablyOwnedTickets = agent.getProbablyOwn(i);
						int diff = alloc - ownedTickets - probablyOwnedTickets;
						if (diff > 0) {
							float bidPrice = priceData.getPrice(Good.values()[i]);
							System.out.println("Bid on hotel " + i + " for " + bidPrice);
							Bid bid = new Bid(i);
							bid.addBidPoint(diff, bidPrice);
							agent.submitBid(bid);
						}
					}
				}
			}
		}
	}

	@Override
	public void bidUpdated(Bid bid) {
		System.out.println("bidUpdated: " + bid.getAuction() + " (" + bid.getProcessingStateAsString() + ")");
	}

	@Override
	public void bidRejected(Bid bid) {
		System.out.println("bidRejected: " + bid.getAuction() + " (" + bid.getRejectReasonAsString() + ")");
	}

	@Override
	public void bidError(Bid bid, int error) {
		System.out.println("bidError: " + bid.getAuction() + " (" + agent.commandStatusToString(error) + ")");
	}

	@Override
	public void auctionClosed(int auction) {
		System.out.println("auctionClosed: " + auction);
		System.out.println(agent.getGameTime());
		if (TACAgent.getAuctionCategory(auction) == TACAgent.CAT_HOTEL) {
			int hotelID = auction - 8;
			hotelPredictor.closed[hotelID] = (int) Math.ceil(agent.getGameTime() / 10000.f);
		}
	}
	
}
