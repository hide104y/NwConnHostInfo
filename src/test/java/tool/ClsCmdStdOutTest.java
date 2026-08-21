package tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ClsCmdStdOut} の単体テストクラスです。
 */
public class ClsCmdStdOutTest {

	/** テスト用作業ディレクトリ */
	private Path tempDir;

	/**
	 * テスト前処理。作業ディレクトリを作成します。
	 *
	 * @throws IOException 入出力例外
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "ClsCmdStdOutTest");
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
	 * 行文字列の追加と取得テスト。
	 */
	@Test
	public void testAddAndGet() {
		ClsCmdStdOut cmdStdOut = new ClsCmdStdOut();
		cmdStdOut.add("Line 1");
		cmdStdOut.add("Line 2");
		cmdStdOut.add(null);

		List<String> list = cmdStdOut.getStdOutList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals("Line 1", list.get(0));
		assertEquals("Line 2", list.get(1));
	}

	/**
	 * クリア処理のテスト。
	 */
	@Test
	public void testClear() {
		ClsCmdStdOut cmdStdOut = new ClsCmdStdOut();
		cmdStdOut.add("Sample");
		assertEquals(1, cmdStdOut.getStdOutList().size());

		cmdStdOut.clear();
		assertTrue(cmdStdOut.getStdOutList().isEmpty());
	}

	/**
	 * リストの一括設定テスト。
	 */
	@Test
	public void testSetStdOutList() {
		ClsCmdStdOut cmdStdOut = new ClsCmdStdOut();
		cmdStdOut.setStdOutList(Arrays.asList("A", "B", "C"));
		assertEquals(3, cmdStdOut.getStdOutList().size());

		cmdStdOut.setStdOutList(null);
		assertTrue(cmdStdOut.getStdOutList().isEmpty());
	}

}
