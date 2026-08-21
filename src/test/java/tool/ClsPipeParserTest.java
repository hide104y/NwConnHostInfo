package tool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ClsPipeParser} の単体テストクラスです。
 */
public class ClsPipeParserTest {

	/** テスト用作業ディレクトリ */
	private Path tempDir;

	/**
	 * テスト前処理。作業ディレクトリを作成します。
	 *
	 * @throws IOException 入出力例外
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "ClsPipeParserTest");
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
	 * パイプ入力からの行読み取りと出力バッファへの追加テスト。
	 *
	 * @throws InterruptedException スレッド割り込み例外
	 */
	@Test
	public void testRun() throws InterruptedException {
		String testData = "line1\nline2\nline3\n";
		ByteArrayInputStream is = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
		ClsCmdStdOut stdOut = new ClsCmdStdOut();

		ClsPipeParser parser = new ClsPipeParser(is, stdOut);
		parser.start();
		parser.join();

		assertEquals(3, stdOut.getStdOutList().size());
		assertEquals("line1", stdOut.getStdOutList().get(0));
		assertEquals("line2", stdOut.getStdOutList().get(1));
		assertEquals("line3", stdOut.getStdOutList().get(2));
	}

	/**
	 * null ストリーム指定時の安全性テスト。
	 */
	@Test
	public void testRunWithNullStream() {
		ClsCmdStdOut stdOut = new ClsCmdStdOut();
		ClsPipeParser parser = new ClsPipeParser(null, stdOut);
		parser.run();
		assertEquals(0, stdOut.getStdOutList().size());
	}

}
