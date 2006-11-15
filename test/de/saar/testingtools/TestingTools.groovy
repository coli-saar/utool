package de.saar.testingtools;


public class TestingTools {
	public static void expectException(Class exceptionType, Closure someCode) {
		try {
			someCode();
			assert false;
		} catch(Exception e) {
			assert exceptionType.isInstance(e)
		}
	}
}