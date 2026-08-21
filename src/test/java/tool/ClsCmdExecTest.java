package tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ClsCmdExec} の単体テストクラスです。
 */
public class ClsCmdExecTest {

	/** テスト用作業ディレクトリ */
	private Path tempDir;

	/**
	 * テスト前処理。作業ディレクトリを作成します。
	 *
	 * @throws IOException 入出力例外
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "ClsCmdExecTest");
		if (!Files.exists(tempDir)) {
			Files.createDirectories(tempDir);
		}
	}

	/**
	 * テスト後処理。作業ディレクトリをクリーンアップします。
	 */
	@After
	public void tearDown() {
		if (tempDir != null && Files.exists(tempDir)) {
			deleteDir(tempDir.toFile());
		}
	}

	/**
	 * ディレクトリを再帰的に削除します。
	 *
	 * @param dir 削除対象ディレクトリ
	 */
	private void deleteDir(File dir) {
		if (dir != null && dir.exists()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) {
						deleteDir(f);
					} else {
						f.delete();
					}
				}
			}
			dir.delete();
		}
	}

	/**
	 * Watchdogおよび標準出力格納オブジェクトの設定テスト。
	 */
	@Test
	public void testGetAndSetWatchdog() {
		ClsCmdExec exec = new ClsCmdExec();
		exec.setWatchdog(10);

		ClsCmdStdOut stdOut = new ClsCmdStdOut();
		exec.setCmdStdOut(stdOut);
		assertEquals(stdOut, exec.getCmdStdOut());
	}

	/**
	 * コマンド実行テスト（echoコマンド）。
	 */
	@Test
	public void testExecuteEcho() {
		ClsCmdExec exec = new ClsCmdExec();
		ClsCmdStdOut stdOut = new ClsCmdStdOut();
		exec.setCmdStdOut(stdOut);

		int ret;
		if (ClsProperties.IS_WINDOWS) {
			ret = exec.execute("cmd.exe", new String[]{"/c", "echo", "HelloTest"});
		} else {
			ret = exec.execute("echo", new String[]{"HelloTest"});
		}

		assertEquals(0, ret);
		assertNotNull(stdOut.getStdOutList());

		boolean found = false;
		for (String line : stdOut.getStdOutList()) {
			if (line.contains("HelloTest")) {
				found = true;
				break;
			}
		}
		assertTrue("Output should contain HelloTest", found);
	}

	/**
	 * 引数なしまたは空引数でのコマンド実行テスト。
	 */
	@Test
	public void testExecuteWithEmptyArgs() {
		ClsCmdExec exec = new ClsCmdExec();
		ClsCmdStdOut stdOut = new ClsCmdStdOut();
		exec.setCmdStdOut(stdOut);

		int ret;
		if (ClsProperties.IS_WINDOWS) {
			ret = exec.execute("cmd.exe", new String[]{"/c", "echo"});
		} else {
			ret = exec.execute("echo", new String[]{""});
		}
		assertEquals(0, ret);
	}

	/**
	 * 終了処理（terminate）のテスト。
	 */
	@Test
	public void testTerminate() {
		ClsCmdExec exec = new ClsCmdExec();
		exec.terminate();
	}

}
