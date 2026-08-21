package tool;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link NwConnHostInfo} の単体テストクラスです。
 */
public final class NwConnHostInfoTest {

	/** テスト用一時ディレクトリパス */
	private Path tempDir;

	/**
	 * 各テスト前の初期化処理。
	 *
	 * @throws IOException 一時ディレクトリ作成失敗時
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "NwConnHostInfoTest");
		if (!Files.exists(tempDir)) {
			Files.createDirectories(tempDir);
		}
	}

	/**
	 * 各テスト後のクリーンアップ処理。
	 *
	 * @throws IOException 一時ディレクトリ削除失敗時
	 */
	@After
	public void tearDown() throws IOException {
		if (Files.exists(tempDir)) {
			Files.walk(tempDir)
					.map(Path::toFile)
					.sorted(Comparator.reverseOrder())
					.forEach(File::delete);
		}
	}

	/**
	 * ファイルパースモードの動作をテストします。
	 *
	 * @throws IOException テストファイル作成失敗時
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
		Files.write(sampleFile.toPath(), sampleNetstat.getBytes(StandardCharsets.UTF_8));

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
		String[] outFiles = outDir.list();
		assertNotNull(outFiles);
		assertTrue(0 < outFiles.length);
	}

	/**
	 * ヘルプ表示オプションをテストします。
	 */
	@Test
	public void testHelpOptions() {
		new NwConnHostInfo(new String[]{"-h"}, false);
	}

	/**
	 * サンプル設定表示オプションをテストします。
	 */
	@Test
	public void testShowSampleConfig() {
		new NwConnHostInfo(new String[]{"--show-sample-config"}, false);
	}

	/**
	 * ホストIPアドレス表示オプションをテストします。
	 */
	@Test
	public void testShowIpAddr() {
		new NwConnHostInfo(new String[]{"--show-ipaddr"}, false);
	}

}
