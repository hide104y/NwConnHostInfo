package tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link NwConnHostInfo} の単体テストクラスです。
 */
public class NwConnHostInfoTest {

	/** テスト用作業ディレクトリ */
	private Path tempDir;

	/**
	 * テスト前処理。作業ディレクトリを作成します。
	 *
	 * @throws IOException 入出力例外
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "NwConnHostInfoTest");
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
	 * ファイルパースモードの動作テスト。
	 *
	 * @throws IOException 入出力例外
	 */
	@Test
	public void testFileParseMode() throws IOException {
		File sampleDir = tempDir.resolve("parse_dir").toFile();
		sampleDir.mkdirs();

		File sampleFile = new File(sampleDir, "netstat_sample.txt");
		String sampleNetstat =
				"  TCP    0.0.0.0:135            0.0.0.0:0              LISTENING\n" +
				"  TCP    192.168.1.10:135       192.168.1.50:50000     ESTABLISHED\n" +
				"  UDP    0.0.0.0:500            *:*                    \n";
		ClsProperties prop = new ClsProperties();
		prop.writeFile(sampleFile.getAbsolutePath(), sampleNetstat, false);

		File outDir = tempDir.resolve("out_dir").toFile();
		outDir.mkdirs();

		String[] args = new String[]{
				"-m", "file",
				"--dir", sampleDir.getAbsolutePath(),
				"--file", "netstat.*\\.txt",
				"-o", outDir.getAbsolutePath(),
				"--os", "win",
				"-v", "1"
		};

		new NwConnHostInfo(args, false);
		assertTrue(outDir.list() != null && outDir.list().length > 0);
	}

	/**
	 * ヘルプオプション (-h) の動作テスト。
	 */
	@Test
	public void testHelpOptions() {
		new NwConnHostInfo(new String[]{"-h"}, false);
	}

	/**
	 * サンプル設定表示オプション (--show-sample-config) の動作テスト。
	 */
	@Test
	public void testShowSampleConfig() {
		new NwConnHostInfo(new String[]{"--show-sample-config"}, false);
	}

	/**
	 * 自ホストIPアドレス表示オプション (--show-ipaddr) の動作テスト。
	 */
	@Test
	public void testShowIpAddr() {
		new NwConnHostInfo(new String[]{"--show-ipaddr"}, false);
	}

}
