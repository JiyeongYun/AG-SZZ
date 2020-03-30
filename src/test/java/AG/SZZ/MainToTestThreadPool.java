package AG.SZZ;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainToTestThreadPool {

	public static void main(String[] args) {
		MainToTestThreadPool runner = new MainToTestThreadPool();
		runner.doMyJob();
	}

	private void doMyJob() {
		long to = 10000000;
		ArrayList<SumPartiallyThread> sumRunners = new ArrayList<SumPartiallyThread>();

		int numOfCoresInMyCPU = Runtime.getRuntime().availableProcessors();
		System.out.println("The number of cores of my system: " + numOfCoresInMyCPU);
		
		ExecutorService executor = Executors.newFixedThreadPool(numOfCoresInMyCPU);

		/* Let a thread compute a sub-sum.
		 * sumRunners.add(new SumPartiallyThread(1,1000000));
		 * sumRunners.add(new SumPartiallyThread(1000001,2000000));
		 * sumRunners.add(new SumPartiallyThread(2000001,3000000));
		 * sumRunners.add(new SumPartiallyThread(3000001,4000000));
		 * sumRunners.add(new SumPartiallyThread(4000001,5000000));
		 * sumRunners.add(new SumPartiallyThread(5000001,6000000));
		 * sumRunners.add(new SumPartiallyThread(6000001,7000000));
		 * sumRunners.add(new SumPartiallyThread(7000001,8000000));
		 * sumRunners.add(new SumPartiallyThread(8000001,9000000));
		 * sumRunners.add(new SumPartiallyThread(9000001,10000000));*/
		for(long i=0; i<to/1000000; i++) {
			Runnable worker = new SumPartiallyThread((i*1000000)+1, (i+1)*1000000);
			executor.execute(worker);
			sumRunners.add((SumPartiallyThread)worker);
		}
		
		executor.shutdown(); // no new tasks will be accepted.
		
		while (!executor.isTerminated()) {
        }

		long grandTotal = 0;
		for(SumPartiallyThread runner:sumRunners) {
			grandTotal += runner.totalSum;
		}
		

		System.out.println("Grand Total = " + grandTotal);

	}

}