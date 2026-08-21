package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * {@link clsCmdExec} の単体テストクラスです。
 */
public final class clsCmdExecTest {

	/**
	 * ウォッチドッグ設定および標準出力設定のテストを行います。
	 */
	@Test
	public void testGetAndSetWatchdog() {
		clsCmdExec exec = new clsCmdExec();
		exec.setWatchdog(10);

		clsCmdStdOut stdOut = new clsCmdStdOut();
		exec.setCmdStdOut(stdOut);
		assertEquals(stdOut, exec.getCmdStdOut());
	}

	/**
	 * echoコマンド実行と標準出力捕捉のテストを行います。
	 */
	@Test
	public void testExecuteEcho() {
		clsCmdExec exec = new clsCmdExec();
		clsCmdStdOut stdOut = new clsCmdStdOut();
		exec.setCmdStdOut(stdOut);

		int ret;
		if (clsProperties.IS_WINDOWS) {
			ret = exec.execute("cmd.exe", new String[]{"/c", "echo", "HelloTest"});
		} else {
			ret = exec.execute("echo", new String[]{"HelloTest"});
		}

		assertEquals(0, ret);
		assertNotNull(stdOut.getStdOutList());
		assertTrue(stdOut.getStdOutList().stream().anyMatch(line -> line.contains("HelloTest")));
	}

	/**
	 * プロセス終了処理のテストを行います。
	 */
	@Test
	public void testTerminate() {
		clsCmdExec exec = new clsCmdExec();
		exec.terminate();
	}

}
