package AG.SZZ;

public class SumPartiallyThread implements Runnable {

	long from, to, totalSum;

	@Override
	public void run() {
		totalSum = getTotalSumFromTo();
		System.out.println("Computed from " + from + " to " + to);
	}

	public SumPartiallyThread(long from, long to) {
		this.from = from;
		this.to = to;
	}

	public long getTotalSumFromTo() {
		long total = 0;
		for (long i = from; i <= to; i++)
			total += i;

		return total;
	}
}
