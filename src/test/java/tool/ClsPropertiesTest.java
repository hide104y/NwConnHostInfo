package tool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ClsProperties} の単体テストクラスです。
 */
public class ClsPropertiesTest {

	/** テスト用作業ディレクトリ */
	private Path tempDir;

	/**
	 * テスト前処理。作業ディレクトリを作成します。
	 *
	 * @throws IOException 入出力例外
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "ClsPropertiesTest");
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
	 * ホスト名およびIPアドレスリストのGetter/Setterテスト。
	 */
	@Test
	public void testGettersAndSetters() {
		ClsProperties prop = new ClsProperties();
		prop.setHostName("myhost");
		assertEquals("myhost", prop.getHostName());

		prop.setIpAddrList(Arrays.asList("10.0.0.1", "10.0.0.2"));
		assertEquals(2, prop.getIpAddrList().size());
	}

	/**
	 * 各種型（String, int, boolean, long, Double, Charset）のプロパティ設定・取得テスト。
	 */
	@Test
	public void testValues() {
		ClsProperties prop = new ClsProperties();
		prop.setValue("strKey", "strVal");
		prop.setValue("intKey", 123);
		prop.setValue("boolKey", true);
		prop.setValue("longKey", 9999999999L);

		assertEquals("strVal", prop.getValue("strKey", "def"));
		assertEquals(123, prop.getValue("intKey", 0));
		assertTrue(prop.getValue("boolKey", false));
		assertEquals(9999999999L, prop.getValue("longKey", 0L));
		assertEquals(Double.valueOf(1.5), prop.getValue("doubleKey", 1.5));
		assertEquals(StandardCharsets.UTF_8, prop.getValue("charsetKey", StandardCharsets.UTF_8));
	}

	/**
	 * 文字列トリム処理のテスト。
	 */
	@Test
	public void testDoTrim() {
		ClsProperties prop = new ClsProperties();
		assertEquals("abc", prop.doTrim("  abc  "));
		assertNull(prop.doTrim("   "));
		assertNull(prop.doTrim(null));
	}

	/**
	 * 数値判定処理のテスト。
	 */
	@Test
	public void testIsNumber() {
		ClsProperties prop = new ClsProperties();
		assertTrue(prop.isNumber("12345"));
		assertTrue(prop.isNumber("-50"));
		assertFalse(prop.isNumber("abc"));
		assertFalse(prop.isNumber(""));
		assertFalse(prop.isNumber(null));
	}

	/**
	 * CSVプロパティ分割・マージテスト。
	 */
	@Test
	public void testSplitMergeProp() {
		ClsProperties prop = new ClsProperties();
		assertTrue(prop.splitMergeProp("key1=val1,key2=val2", ","));
		assertEquals("val1", prop.getValue("key1", ""));
		assertEquals("val2", prop.getValue("key2", ""));
	}

	/**
	 * UNIXタイムスタンプから日時文字列への変換テスト。
	 */
	@Test
	public void testConvUnixToJst() {
		ClsProperties prop = new ClsProperties();
		prop.setValue(ClsProperties.TIMEZONE, "Asia/Tokyo");
		String formatted = prop.convUnixToJst(0L, "yyyy/MM/dd HH:mm:ss");
		assertEquals("1970/01/01 09:00:00", formatted);

		String formattedSec = prop.convUnixToJst(0, "yyyy/MM/dd HH:mm:ss");
		assertEquals("1970/01/01 09:00:00", formattedSec);
	}

	/**
	 * OS種別判定およびエンコーディング判定のテスト。
	 */
	@Test
	public void testOsDetection() {
		ClsProperties prop = new ClsProperties();
		assertNotNull(prop.getOsName());
		assertNotNull(prop.getOsShortName());

		assertEquals(ClsProperties.OS_WIN, prop.getOsId("win"));
		assertEquals(ClsProperties.OS_LINUX, prop.getOsId("linux"));
		assertEquals(ClsProperties.OS_HPUX, prop.getOsId("hpux"));
		assertEquals(ClsProperties.OS_SOLARIS, prop.getOsId("solaris"));

		assertNotNull(prop.getDefEncoding(ClsProperties.OS_WIN));
		assertNotNull(prop.getDefEncoding(ClsProperties.OS_LINUX));
		assertNotNull(prop.getDefEncoding(ClsProperties.OS_HPUX));
		assertNotNull(prop.getDefEncoding(ClsProperties.OS_SOLARIS));
	}

	/**
	 * ファイル読み書き（read, readFile, readFileToList, writeFile）テスト。
	 */
	@Test
	public void testFileOperations() {
		ClsProperties prop = new ClsProperties();
		File testFile = tempDir.resolve("test_prop.txt").toFile();

		String content = "# comment\nKeyA = ValueA\nKeyB = ValueB\n";
		assertTrue(prop.writeFile(testFile.getAbsolutePath(), content, false));
		assertTrue(prop.isExist(testFile.getAbsolutePath()));

		assertTrue(prop.read(testFile.getAbsolutePath(), "UTF-8"));
		assertEquals("ValueA", prop.getValue("KeyA", ""));
		assertEquals("ValueB", prop.getValue("KeyB", ""));

		List<String> lines = prop.readFileToList(testFile.getAbsolutePath());
		assertEquals(2, lines.size());

		String rawContent = prop.readFile(testFile.getAbsolutePath());
		assertTrue(rawContent.contains("KeyA = ValueA"));
	}

	/**
	 * コマンド実行結果文字列化テスト。
	 */
	@Test
	public void testExecToString() {
		ClsProperties prop = new ClsProperties();
		String out = prop.execToString("hostname");
		assertNotNull(out);
	}

	/**
	 * デフォルト出力ファイル名生成テスト。
	 */
	@Test
	public void testDefaultName() {
		ClsProperties prop = new ClsProperties();
		String defName = prop.getDefaultName();
		assertTrue(defName.startsWith("NwConnHostInfo_"));
	}

}
