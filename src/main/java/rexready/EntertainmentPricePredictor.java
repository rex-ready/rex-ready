package rexready;

public class EntertainmentPricePredictor {
	public static final int T = 54;
	public static final float[] deltaValues = {-20, 0, 20};

	public float[] deltas = new float[12];
	public float[] previousPrices = new float[12];
	public float[] currentPrices = new float[12];
	public Plateaus priceSizePlateaus = new Plateaus(40, 60, 100, 140);
	public Plateaus priceDifferencePlateaus = new Plateaus(-20, -5, 5, 20);
	public float[] previousOtherAverage = new float[12];
	public float[] previousSameAverage = new float[12];


	public void updateDeltas(int t) {
		for (int i = 0; i < 12; i++) {
			deltas[i] = updateDeltaForAuction(i, t);
		}
		for (int i = 0; i < 12; i++) {
			previousPrices[i] = currentPrices[i];
		}
	}

	public float updateDeltaForAuction(int auctionID, int t) {
		float[] delta = new float[3];

		float price = currentPrices[auctionID];
		//float priceDifference = currentPrices[auctionID] - previousPrices[auctionID];
		float sameAverage = 0;
		int k = auctionID / 4;
		k *= 4;
		for (int j = 0; j < 4; j++)
		{
			sameAverage += currentPrices[k + j];
		}
		sameAverage /= 4.0;
		
		float otherAverage = 0;
		k = auctionID / 4;
		for (int j = 0; j < 3; j++)
		{
			otherAverage += currentPrices[k + j * 3];
		}
		otherAverage /= 3.0;
		
		float sameChange = sameAverage - previousSameAverage[auctionID];
		float otherChange = otherAverage - previousOtherAverage[auctionID];
		
		previousSameAverage[auctionID] = sameAverage;
		previousOtherAverage[auctionID] = otherAverage;
		float[] p = new float[3];
		float[] sp = new float[3];
		float[] op = new float[3];
		//float[] c = new float[3];
		float[] sc = new float[3];
		float[] oc = new float[3];

		p = priceSizePlateaus.calculateWeights(price);
		sp = priceSizePlateaus.calculateWeights(sameAverage);
		op = priceSizePlateaus.calculateWeights(otherAverage);
		//c = priceDifferencePlateaus.calculateWeights(priceDifference);
		sc = priceDifferencePlateaus.calculateWeights(sameChange);
		oc = priceDifferencePlateaus.calculateWeights(otherChange);

		//p = price, sp = average price of same entertainment across days, op = average price of all entertainment types on same day
		//c = price change, sc = price change of same entertainment, oc = price change of all entertainment types
		//f = 1.7 (because sp is expected to have more affect than op)
		float f = 1.7f;
		//currently not considering c, may want to do so
		
		//if p is not high and sp is high and sc is not negative, delta is positive
		delta[2] += ((p[0] + p[1]) + sp[2] + (sc[1] + sc[2])) / 3.0;
		//if p is not high and sp is high and sc is negative, delta is zero
		delta[1] += ((p[0] + p[1]) + sp[2] + sc[0]) / 3.0;
		//if p is low and sp is low, delta is 0
		delta[1] += (p[0] + sp[0]) / 2.0;
		//if p is high and sp is not high and sc is not positive, delta is negative
		delta[0] += (p[2] + (sp[0] + sp[1]) + (sc[0] + sc[1])) / 3.0;
		//if p is high and sp is high, delta is 0
		delta[1] += (p[2] + sp[2]) / 2.0;
		
		delta[0] *= f;
		delta[1] *= f;
		delta[2] *= f;
		
		//same rules, but with op instead of sp and oc instead of sc
		delta[2] += ((p[0] + p[1]) + op[2] + (oc[1] + oc[2])) / 3.0;
		delta[1] += ((p[0] + p[1]) + op[2] + oc[0]) / 3.0;
		delta[1] += (p[0] + op[0]) / 2.0;
		delta[0] += (p[2] + (op[0] + op[1]) + (oc[0] + oc[1])) / 3.0;
		delta[1] += (p[2] + op[2]) / 2.0;

		//normalize
		float sum = 0;
		for (int i = 0; i < 3; i++) {
			sum += delta[i];
		}
		for (int i = 0; i < 3; i++) {
			if (sum != 0)
				delta[i] /= sum;
		}

		float realValue = 0;

		for (int i = 0; i < 3; i++) {
			realValue += delta[i] * deltaValues[i];
		}

		return realValue;
	}
}

//alligator 1 -4, amusement, museum