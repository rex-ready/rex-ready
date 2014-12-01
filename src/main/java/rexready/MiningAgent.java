package rexready;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.util.ArgEnumerator;

public class MiningAgent extends AgentImpl {

	// private Map<String, List<Float>> prices = new HashMap<String,
	// List<Float>>();

	@Override
	protected void init(ArgEnumerator args) {
		// for(int i=0; i<TACAgent.getAuctionNo(); i++) {
		// prices.put(TACAgent.getAuctionTypeAsString(i), new
		// ArrayList<Float>());
		// }
	}

	@Override
	public void gameStarted() {
	}

	@Override
	public void gameStopped() {
		// System.err.println("Game is over.");
		// try {
		// File file = new File("prices" + System.currentTimeMillis() + ".txt");
		// if (!file.exists()) {
		// file.createNewFile();
		// }
		//
		// FileWriter fw = new FileWriter(file.getAbsoluteFile());
		// BufferedWriter bw = new BufferedWriter(fw);
		// for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
		// List<Float> priceList =
		// prices.get(TACAgent.getAuctionTypeAsString(i));
		// for(Float f : priceList) {
		// bw.write(f + ",");
		// }
		// bw.newLine();
		// }
		// bw.close();
		// System.err.println("Written prices to file.");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// System.err.println("DONE. IT'S ALL DONE.");
	}

	public void quoteUpdated(Quote quote) {
		int auctionID = quote.getAuction();
		// if(agent.getAuctionCategory(auctionID) == TACAgent.CAT_ENTERTAINMENT)
		// {
		System.out.println(auctionID + " " + quote.getAskPrice() + " " + quote.getBidPrice());
		// }else{
		// System.err.println(auctionID + " " + quote.getAskPrice());
		// }
	}

	@Override
	public void bidUpdated(Bid bid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void bidRejected(Bid bid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void bidError(Bid bid, int error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void auctionClosed(int auction) {
		// TODO Auto-generated method stub

	}

}
