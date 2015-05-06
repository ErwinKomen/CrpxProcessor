/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.ru.util;

/**
 * Utilities to do with JVM memory.
 */
public class MemoryUtil {
	/** Handle to interface with the Java VM environment */
	private static Runtime runtime = Runtime.getRuntime();

	/**
	 * Returns the amount of memory that can still be allocated before we get the OutOfMemory
	 * exception.
	 *
	 * @return the amount of memory that can still be allocated
	 */
	public static long getFree() {
		return runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory());
	}

	/**
	 * Test program.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		System.out.println("free memory: " + freeMemory / 1024);
		System.out.println("allocated memory: " + allocatedMemory / 1024);
		System.out.println("max memory: " + maxMemory / 1024);
		System.out.println("total free memory: " + (freeMemory + (maxMemory - allocatedMemory))
				/ 1024);
	}

}