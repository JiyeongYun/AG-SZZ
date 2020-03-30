package AG.SZZ;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainToMimicJoin {

	public static void main(String[] args) {
		MainToMimicJoin runner = new MainToMimicJoin();
		runner.doMyJob();
	}

	private void doMyJob() {
		long to = 10000000;
		ArrayList<SumPartiallyThread> sumRunners = new ArrayList<SumPartiallyThread>();

		int numOfCoresInMyCPU = Runtime.getRuntime().availableProcessors();
		System.out.println("The number of cores of my system: " + numOfCoresInMyCPU);

		ExecutorService executor = Executors.newFixedThreadPool(numOfCoresInMyCPU);

		ArrayList<Callable<Object>> calls = new ArrayList<Callable<Object>>();
		for (long i = 0; i < to / 1000000; i++) {
			Runnable worker = new SumPartiallyThread((i * 1000000) + 1, (i + 1) * 1000000);
			sumRunners.add((SumPartiallyThread) worker);
			calls.add(Executors.callable(worker));
		}

		try {
			executor.invokeAll(calls); // This line will be terminated after all threads are terminated.
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		executor.shutdown();

		long grandTotal = 0;
		for (SumPartiallyThread runner : sumRunners) {
			grandTotal += runner.totalSum;
		}

		System.out.println("Grand Total = " + grandTotal);
	}

}