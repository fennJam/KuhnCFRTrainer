import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

public class KuhnTrainer {
	public static final int PASS = 0, BET = 1, NUM_ACTIONS = 2;
	public static final Random random = new Random();
	public TreeMap<String, Node> nodeMap = new TreeMap<String, Node>();

	public static void main(String[] args) {
		int iterations = 10000000;
		new KuhnTrainer().train(iterations);
	}

	public void train(int iterations) {
		int[] cards = { 1, 2, 3 };
		double util = 0;
		for (int i = 0; i < iterations; i++) {
			cards = shuffleCards(cards);
			util += cfr(cards, "", 1, 1);
		}
		System.out.println("Average game value: " + util / iterations);
		for (Node n : nodeMap.values())
			System.out.println(n);

	}

	public int[] shuffleCards(int[] cards) {
		for (int c1 = cards.length - 1; c1 > 0; c1--) {
			int c2 = random.nextInt(c1 + 1);
			int tmp = cards[c1];
			cards[c1] = cards[c2];
			cards[c2] = tmp;
		}
		return cards;
	}

	private double cfr(int[] cards, String history, double p0, double p1) {
		int plays = history.length(); 
		int player = plays % 2; 
		int opponent = 1 - player;
		
		if (plays > 1) {
			boolean terminalPass = history.charAt(plays - 1) == 'p';
			boolean doubleBet = history.substring(plays - 2, plays).equals("bb");
			boolean isPlayerCardHigher = cards[player] > cards[opponent];
			if (terminalPass) {
				if (history.equals("pp")) {
					return isPlayerCardHigher ? 1 : -1;
				} else
					return 1;
			} else if (doubleBet){
				return isPlayerCardHigher ? 1 : -1;
			}
		}
		String infoSet = cards[player] + history;
		Node node = getInformationSetNode(infoSet);
		return recursiveCFR( player, node, history, cards, p0, p1);
		}

	// In discerning a terminal state, we first check to see of both players
	// have had at least one action.
	// Given that, we check for the two conditions for a terminal state: a
	// terminal pass after the first action, or a double bet. If there’s a
	// terminal pass, then a double terminal pass awards a chip to the player
	// with the higher card. Otherwise, it’s a single pass after a bet and the
	// player betting wins a chip. If it’s not a terminal pass, but a two
	// consecutive bets have occurred, the player with the higher card gets two
	// chips. Otherwise, the state isn’t terminal and computation continues:

//	private int returnPayOffForTerminalStates(int plays, String history, int[] cards, int player, int opponent) {
//		if (plays > 1) {
//			boolean terminalPass = history.charAt(plays - 1) == 'p';
//			boolean doubleBet = history.substring(plays - 2, plays).equals("bb");
//			boolean isPlayerCardHigher = cards[player] > cards[opponent];
//			if (terminalPass) {
//				if (history.equals("pp")) {
//					return isPlayerCardHigher ? 1 : -1;
//				} else
//					return 1;
//			} else if (doubleBet){
//				return isPlayerCardHigher ? 2 : -2;
//			}
//		}
//
//	}

	private Node getInformationSetNode(String infoSet) {
		Node node = nodeMap.get(infoSet);
		if (node == null) {
			node = new Node();
			node.infoSet = infoSet;
			nodeMap.put(infoSet, node);
		}
		return node;
	}

	private double recursiveCFR(int player, Node node, String history, int[] cards, double p0, double p1) {
		double[] strategy = node.getStrategy(player == 0 ? p0 : p1);
		double[] util = new double[NUM_ACTIONS];
		double nodeUtil = 0;
		for (int a = 0; a < NUM_ACTIONS; a++) {
			String nextHistory = history + (a == 0 ? "p" : "b");
			util[a] = player == 0 ? -cfr(cards, nextHistory, p0 * strategy[a], p1)
					: -cfr(cards, nextHistory, p0, p1 * strategy[a]);
			nodeUtil += strategy[a] * util[a];
			double regret = util[a] - nodeUtil;
			node.regretSum[a] += (player == 0 ? p1 : p0) * regret;
		}
		return nodeUtil;
	}

	public class Node {
		String infoSet;
		double[] regretSum = new double[NUM_ACTIONS], strategy = new double[NUM_ACTIONS],
				strategySum = new double[NUM_ACTIONS];

		private double[] getStrategy(double realizationWeight) {
			double normalizingSum = 0;
			for (int a = 0; a < NUM_ACTIONS; a++) {
				strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0;
				normalizingSum += strategy[a];
			}
			for (int a = 0; a < NUM_ACTIONS; a++) {
				if (normalizingSum > 0)
					strategy[a] /= normalizingSum;
				else
					strategy[a] = 1.0 / NUM_ACTIONS;
				strategySum[a] += realizationWeight * strategy[a];
			}
			return strategy;
		}

		public double[] getAverageStrategy() {
			double[] avgStrategy = new double[NUM_ACTIONS];
			double normalizingSum = 0;
			for (int a = 0; a < NUM_ACTIONS; a++)
				normalizingSum += strategySum[a];
			for (int a = 0; a < NUM_ACTIONS; a++)
				if (normalizingSum > 0)
					avgStrategy[a] = strategySum[a] / normalizingSum;
				else
					avgStrategy[a] = 1.0 / NUM_ACTIONS;
			return avgStrategy;
		}

		public String toString() {
			return String.format("%4s: %s", infoSet, Arrays.toString(getAverageStrategy()));
		}
	}
}
