package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link clsProperties} の単体テストクラスです。
 */
public final class clsPropertiesTest {

	/** テスト用一時ディレクトリパス */
	private Path tempDir;

	/**
	 * 各テスト前の初期化処理。
	 *
	 * @throws IOException 一時ディレクトリ作成失敗時
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "clsPropertiesTest");
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
	 * ホスト名・IPアドレスリストのゲッター・セッターをテストします。
	 */
	@Test
	public void testGettersAndSetters() {
		clsProperties prop = new clsProperties();
		prop.setHostName("myhost");
		assertEquals("myhost", prop.getHostName());

		prop.setIpAddrList(Arrays.asList("10.0.0.1", "10.0.0.2"));
		assertEquals(2, prop.getIpAddrList().size());
	}

	/**
	 * 各種データ型に対するプロパティ値の読み書きをテストします。
	 */
	@Test
	public void testValues() {
		clsProperties prop = new clsProperties();
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
	 * 文字列トリムおよび空文字判定をテストします。
	 */
	@Test
	public void testDoTrim() {
		clsProperties prop = new clsProperties();
		assertEquals("abc", prop.doTrim("  abc  "));
		assertNull(prop.doTrim("   "));
		assertNull(prop.doTrim(null));
	}

	/**
	 * 数値形式文字列判定をテストします。
	 */
	@Test
	public void testIsNumber() {
		clsProperties prop = new clsProperties();
		assertTrue(prop.isNumber("12345"));
		assertTrue(prop.isNumber("-50"));
		assertFalse(prop.isNumber("abc"));
		assertFalse(prop.isNumber(""));
		assertFalse(prop.isNumber(null));
	}

	/**
	 * CSV形式文字列からのプロパティ分割・マージをテストします。
	 */
	@Test
	public void testSplitMergeProp() {
		clsProperties prop = new clsProperties();
		assertTrue(prop.splitMergeProp("key1=val1,key2=val2", ","));
		assertEquals("val1", prop.getValue("key1", ""));
		assertEquals("val2", prop.getValue("key2", ""));
	}

	/**
	 * UNIXタイムスタンプからJST日時文字列への変換をテストします。
	 */
	@Test
	public void testConvUnixToJst() {
		clsProperties prop = new clsProperties();
		prop.setValue(clsProperties.TIMEZONE, "Asia/Tokyo");
		String formatted = prop.convUnixToJst(0L, "yyyy/MM/dd HH:mm:ss");
		assertEquals("1970/01/01 09:00:00", formatted);

		String formattedSec = prop.convUnixToJst(0, "yyyy/MM/dd HH:mm:ss");
		assertEquals("1970/01/01 09:00:00", formattedSec);
	}

	/**
	 * OS種別の自動判定・エンコーディング取得をテストします。
	 */
	@Test
	public void testOsDetection() {
		clsProperties prop = new clsProperties();
		assertNotNull(prop.getOsName());
		assertNotNull(prop.getOsShortName());

		assertEquals(clsProperties.OS_WIN, prop.getOsId("win"));
		assertEquals(clsProperties.OS_LINUX, prop.getOsId("linux"));
		assertEquals(clsProperties.OS_HPUX, prop.getOsId("hpux"));
		assertEquals(clsProperties.OS_SOLARIS, prop.getOsId("solaris"));

		assertNotNull(prop.getDefEncoding(clsProperties.OS_WIN));
		assertNotNull(prop.getDefEncoding(clsProperties.OS_LINUX));
		assertNotNull(prop.getDefEncoding(clsProperties.OS_HPUX));
		assertNotNull(prop.getDefEncoding(clsProperties.OS_SOLARIS));
	}

	/**
	 * ファイル読み書き関連処理をテストします。
	 */
	@Test
	public void testFileOperations() {
		clsProperties prop = new clsProperties();
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
	 * コマンド実行結果の文字列化をテストします。
	 */
	@Test
	public void testExecToString() {
		clsProperties prop = new clsProperties();
		String out = prop.execToString("hostname");
		assertNotNull(out);
	}

	/**
	 * デフォルト出力ファイル名生成をテストします。
	 */
	@Test
	public void testDefaultName() {
		clsProperties prop = new clsProperties();
		String defName = prop.getDefaultName();
		assertTrue(defName.startsWith("NwConnHostInfo_"));
	}

}
