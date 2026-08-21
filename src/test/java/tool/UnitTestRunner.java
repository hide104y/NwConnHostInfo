package tool;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit ランナーがない環境でも各単体テストを実行・検証できるテスト実行メインクラスです。
 */
public class UnitTestRunner {

	/**
	 * 単体テスト群をリフレクション経由で実行します。
	 *
	 * @param args コマンドライン引数
	 */
	public static void main(String[] args) {
		Class<?>[] testClasses = new Class<?>[]{
				ClsAppPropTest.class,
				ClsCmdExecTest.class,
				ClsCmdStdOutTest.class,
				ClsNetstatTest.class,
				ClsPipeParserTest.class,
				ClsPropertiesTest.class,
				NwConnHostInfoTest.class
		};

		int totalTests = 0;
		int passedTests = 0;
		int failedTests = 0;
		List<String> failureMessages = new ArrayList<>();

		System.out.println("================================================================");
		System.out.println("  NwConnHostInfo Unit Test Suite Execution");
		System.out.println("================================================================");

		for (Class<?> clazz : testClasses) {
			System.out.println("\nRunning tests in: " + clazz.getSimpleName());

			Method beforeMethod = null;
			Method afterMethod = null;
			List<Method> testMethods = new ArrayList<>();

			for (Method m : clazz.getDeclaredMethods()) {
				if (m.isAnnotationPresent(Before.class)) {
					beforeMethod = m;
				} else if (m.isAnnotationPresent(After.class)) {
					afterMethod = m;
				} else if (m.isAnnotationPresent(Test.class)) {
					testMethods.add(m);
				}
			}

			for (Method testMethod : testMethods) {
				totalTests++;
				Object instance = null;
				try {
					instance = clazz.newInstance();
					if (beforeMethod != null) {
						beforeMethod.invoke(instance);
					}
					testMethod.invoke(instance);
					System.out.println("  [PASS] " + testMethod.getName());
					passedTests++;
				} catch (Throwable t) {
					failedTests++;
					Throwable cause = (t.getCause() != null ? t.getCause() : t);
					String msg = clazz.getSimpleName() + "." + testMethod.getName() + " -> " + cause.toString();
					failureMessages.add(msg);
					System.err.println("  [FAIL] " + testMethod.getName() + " : " + cause.getMessage());
					cause.printStackTrace(System.err);
				} finally {
					if (instance != null && afterMethod != null) {
						try {
							afterMethod.invoke(instance);
						} catch (Throwable ignored) {
							// ignore
						}
					}
				}
			}
		}

		System.out.println("\n================================================================");
		System.out.println(String.format("Tests run: %d, Passed: %d, Failures: %d", totalTests, passedTests, failedTests));
		System.out.println("================================================================");

		if (failedTests > 0) {
			System.err.println("\nFailed tests summary:");
			for (String failure : failureMessages) {
				System.err.println("  - " + failure);
			}
			System.exit(1);
		} else {
			System.out.println("\nAll unit tests passed successfully!");
		}
	}
}
