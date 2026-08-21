package org.junit;

/**
 * JUnit 互換のアサーションクラスです。
 */
public class Assert {

	protected Assert() {
	}

	public static void assertTrue(String message, boolean condition) {
		if (!condition) {
			fail(message);
		}
	}

	public static void assertTrue(boolean condition) {
		assertTrue(null, condition);
	}

	public static void assertFalse(String message, boolean condition) {
		assertTrue(message, !condition);
	}

	public static void assertFalse(boolean condition) {
		assertFalse(null, condition);
	}

	public static void fail(String message) {
		if (message == null) {
			throw new AssertionError();
		}
		throw new AssertionError(message);
	}

	public static void fail() {
		fail(null);
	}

	public static void assertEquals(String message, Object expected, Object actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected != null && expected.equals(actual)) {
			return;
		}
		failNotEquals(message, expected, actual);
	}

	public static void assertEquals(Object expected, Object actual) {
		assertEquals(null, expected, actual);
	}

	public static void assertEquals(String message, long expected, long actual) {
		if (expected != actual) {
			failNotEquals(message, Long.valueOf(expected), Long.valueOf(actual));
		}
	}

	public static void assertEquals(long expected, long actual) {
		assertEquals(null, expected, actual);
	}

	public static void assertEquals(String message, double expected, double actual, double delta) {
		if (Double.compare(expected, actual) != 0) {
			if (Math.abs(expected - actual) > delta) {
				failNotEquals(message, Double.valueOf(expected), Double.valueOf(actual));
			}
		}
	}

	public static void assertEquals(double expected, double actual, double delta) {
		assertEquals(null, expected, actual, delta);
	}

	public static void assertNotNull(String message, Object object) {
		assertTrue(message, object != null);
	}

	public static void assertNotNull(Object object) {
		assertNotNull(null, object);
	}

	public static void assertNull(String message, Object object) {
		assertTrue(message, object == null);
	}

	public static void assertNull(Object object) {
		assertNull(null, object);
	}

	private static void failNotEquals(String message, Object expected, Object actual) {
		String formatted = "";
		if (message != null && !message.isEmpty()) {
			formatted = message + " ";
		}
		fail(formatted + "expected:<" + expected + "> but was:<" + actual + ">");
	}
}
