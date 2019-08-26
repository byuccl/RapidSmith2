package edu.byu.ece.rapidSmith.util;

import java.util.List;

public class Sorting {

	/**
	 * The main method that implements quick sort. Based on the implementation found at:
	 * https://www.geeksforgeeks.org/quick-sort/.
	 * @param arr array to be sorted
	 * @param low starting index
	 * @param high ending index
	 * @param <T>
	 */
	public static <T extends Comparable<T>> void quickSort(List<T> arr, int low, int high)
	{
		if (low < high) {
			// arr[partitioningIndex] is now at the right place
			int partitioningIndex = partition(arr, low, high);

			// Recursively sort elements before partition and after partition
			quickSort(arr, low, partitioningIndex-1);
			quickSort(arr, partitioningIndex+1, high);
		}
	}

	/**
	 * The pivot method for quick sort. Based on the implementation found at: https://www.geeksforgeeks.org/quick-sort/.
	 * This method takes last element as pivot, places the pivot element at its correct position in sorted array, and
	 * places all smaller (smaller than pivot) to left of pivot and all greater elements to right of pivot
	 * @param arr
	 * @param low
	 * @param high
	 * @param <T>
	 * @return
	 */
	private static <T extends Comparable<T>> int partition(List<T> arr, int low, int high) {
		T pivot = arr.get(high);
		int i = (low-1); // index of smaller element
		for (int j=low; j<high; j++) {
			// If current element is smaller than or
			// equal to pivot
			if (arr.get(j).compareTo(pivot) > 0) {
				i++;

				// swap arr[i] and arr[j]
				T temp = arr.get(i);
				arr.set(i, arr.get(j));
				arr.set(j, temp);
			}
		}

		// swap arr[i+1] and arr[high] (or pivot)
		T temp = arr.get(i+1);
		arr.set(i+1, arr.get(high));
		arr.set(high, temp);
		return i+1;
	}

}
