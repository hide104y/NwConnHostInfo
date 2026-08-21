package tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ClsAppProp} の単体テストクラスです。
 */
public class ClsAppPropTest {

	/** テスト用作業ディレクトリ */
	private Path tempDir;

	/**
	 * テスト前処理。作業ディレクトリを作成します。
	 *
	 * @throws IOException 入出力例外
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "ClsAppPropTest");
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
	 * デフォルトコンストラクタおよび初期値のテスト。
	 */
	@Test
	public void testDefaultConstructor() {
		ClsAppProp prop = new ClsAppProp();
		assertNotNull(prop);
		assertEquals(0, prop.getPid());
		assertEquals("-", prop.getAppName());
		assertEquals("-", prop.getAppPath());
	}

	/**
	 * GetterおよびSetterの動作テスト。
	 */
	@Test
	public void testGettersAndSetters() {
		ClsAppProp prop = new ClsAppProp();
		prop.setPid(100);
		prop.setAppName("testApp");
		prop.setAppPath("/bin/testApp");

		assertEquals(100, prop.getPid());
		assertEquals("testApp", prop.getAppName());
		assertEquals("/bin/testApp", prop.getAppPath());
	}

	/**
	 * 文字列PIDのパース設定テスト。
	 */
	@Test
	public void testSetPidString() {
		ClsAppProp prop = new ClsAppProp();
		prop.setPid(" 200 ");
		assertEquals(200, prop.getPid());

		prop.setPid("invalid");
		assertEquals(0, prop.getPid());

		prop.setPid((String) null);
		assertEquals(0, prop.getPid());

		prop.setPid("");
		assertEquals(0, prop.getPid());
	}

	/**
	 * null設定時の安全性のテスト。
	 */
	@Test
	public void testNullSafety() {
		ClsAppProp prop = new ClsAppProp();
		prop.setAppName(null);
		prop.setAppPath(null);

		assertEquals("-", prop.getAppName());
		assertEquals("-", prop.getAppPath());
	}

}
