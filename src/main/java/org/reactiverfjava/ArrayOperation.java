package org.reactiverfjava;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * This implementation uses the ForkJoinPool class from the Java Concurrency API
 * to create multiple threads for the array operation. The ArrayOperation class
 * extends the RecursiveAction class, which provides a basic implementation of
 * the ForkJoinTask abstract class for tasks that do not return a result.
 * 
 * In this implementation, the compute method of the ArrayOperation class
 * performs the recursive operation. If the size of the subarray is less than or
 * equal to the threshold, the operation is performed on the subarray.
 * Otherwise, the subarray is divided in half and two new ArrayOperation tasks
 * are created to handle each half. The invokeAll method of the ForkJoinTask
 * class is used to execute these two tasks in parallel.
 * 
 * The performOperation method creates a ForkJoinPool object and uses it to
 * execute the ArrayOperation task. The threshold parameter determines the size
 * of the subarray below which the operation is performed sequentially instead
 * of in parallel.
 * 
 * This implementation should maximize CPU utilization by utilizing as many
 * threads as possible. However, the actual number of threads used by the
 * ForkJoinPool will depend on various factors, such as the available system
 * resources and the size of the array.
 * 
 */
@SuppressWarnings("serial")
public class ArrayOperation extends RecursiveAction {
	private int[] array;
	private int start;
	private int end;
	private int threshold;

	public ArrayOperation(int[] array, int start, int end, int threshold) {
		this.array = array;
		this.start = start;
		this.end = end;
		this.threshold = threshold;
	}

	@Override
	protected void compute() {
		if (end - start <= threshold) {
			// Perform the operation on the subarray
			for (int i = start; i < end; i++) {
				// Replace this line with your desired operation on the array element
				array[i]++;
			}
		} else {
			int mid = (start + end) / 2;
			ArrayOperation left = new ArrayOperation(array, start, mid, threshold);
			ArrayOperation right = new ArrayOperation(array, mid, end, threshold);
			invokeAll(left, right);
		}
	}

	public static void performOperation(int[] array, int threshold) {
		ForkJoinPool pool = new ForkJoinPool();
		ArrayOperation task = new ArrayOperation(array, 0, array.length, threshold);
		pool.invoke(task);
	}
}