package tool;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ClsNetstat} の単体テストクラスです。
 */
public class ClsNetstatTest {

	/** テスト用作業ディレクトリ */
	private Path tempDir;
	private ClsProperties prop;
	private ClsNetstat netstat;

	/**
	 * テスト前処理。作業ディレクトリを作成し、ClsNetstatを初期化します。
	 *
	 * @throws IOException 入出力例外
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "ClsNetstatTest");
		if (!Files.exists(tempDir)) {
			Files.createDirectories(tempDir);
		}
		prop = new ClsProperties();
		prop.setValue(ClsProperties.PATHDOUT, tempDir.toString());
		netstat = new ClsNetstat(prop);
		netstat.init();
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
	 * 特殊文字エンコード・デコードのテスト。
	 */
	@Test
	public void testEncodeDecode() {
		String original = "192.168.1.1:80";
		String encoded = netstat.encodeStr(original);
		assertEquals("192.168.1.1_80", encoded);
		assertEquals(original, netstat.decodeStr(encoded));

		assertEquals("*", netstat.decodeStr(netstat.encodeStr("*")));
		assertEquals("", netstat.encodeStr(""));
		assertEquals("", netstat.decodeStr(""));
	}

	/**
	 * ループバックアドレス判定テスト。
	 */
	@Test
	public void testIsLoopbackAddr() {
		assertTrue(netstat.isLoopbackAddr(netstat.encodeStr("127.0.0.1")));
		assertTrue(netstat.isLoopbackAddr(netstat.encodeStr("[::1]")));
		assertTrue(netstat.isLoopbackAddr(netstat.encodeStr("::1")));
		assertFalse(netstat.isLoopbackAddr(netstat.encodeStr("192.168.1.1")));
		assertFalse(netstat.isLoopbackAddr(null));
	}

	/**
	 * IPv4アドレス形式判定テスト。
	 */
	@Test
	public void testIsIpv4Address() {
		assertTrue(netstat.isIpv4Address("192.168.0.1"));
		assertTrue(netstat.isIpv4Address("10.0.0.1/24"));
		assertFalse(netstat.isIpv4Address("999.999.999.999"));
		assertFalse(netstat.isIpv4Address("2001:db8::1"));
		assertFalse(netstat.isIpv4Address("invalid"));
		assertFalse(netstat.isIpv4Address(null));
		assertFalse(netstat.isIpv4Address(""));
	}

	/**
	 * ソケットステータス判定テスト。
	 */
	@Test
	public void testStates() {
		assertTrue(netstat.isTcpListen("LISTEN"));
		assertTrue(netstat.isTcpListen("LISTENING"));
		assertFalse(netstat.isTcpListen("ESTABLISHED"));
		assertFalse(netstat.isTcpListen(null));

		assertTrue(netstat.isTcpConnState("ESTABLISHED"));
		assertFalse(netstat.isTcpConnState("LISTEN"));
		assertFalse(netstat.isTcpConnState(null));

		assertTrue(netstat.isUdpListen(netstat.encodeStr("*:*")));
		assertFalse(netstat.isUdpListen(null));
		assertTrue(netstat.isUdpConnState("192.168.1.1_53"));
	}

	/**
	 * PID文字列からのアプリケーション情報取得テスト。
	 */
	@Test
	public void testGetAppProp() {
		ClsAppProp winProp = netstat.getAppProp("1234");
		assertNotNull(winProp);
		assertEquals(1234, winProp.getPid());

		ClsAppProp linuxProp = netstat.getAppProp("5678/my_process");
		assertNotNull(linuxProp);

		ClsAppProp nullProp = netstat.getAppProp(null);
		assertNotNull(nullProp);
		assertEquals(0, nullProp.getPid());
	}

	/**
	 * Windows 形式 netstat 出力のパースおよび集計テスト。
	 */
	@Test
	public void testParseWindowsNetstat() {
		prop.setValue(ClsProperties.OS_NAME, "win");
		netstat.init();

		netstat.setStdOutList(Arrays.asList(
				"  TCP    0.0.0.0:80             0.0.0.0:0              LISTENING",
				"  TCP    192.168.1.50:80        192.168.1.100:54321    ESTABLISHED",
				"  UDP    0.0.0.0:53             *:*                    "
		));

		netstat.getListenPorts();
		netstat.getConnList();
		netstat.showList();
	}

	/**
	 * Linux 形式 netstat 出力のパースおよび集計テスト。
	 */
	@Test
	public void testParseLinuxNetstat() {
		prop.setValue(ClsProperties.OS_NAME, "linux");
		netstat.init();

		netstat.setStdOutList(Arrays.asList(
				"tcp        0      0 0.0.0.0:8080            0.0.0.0:*               LISTEN",
				"tcp        0      0 192.168.1.50:8080       192.168.1.200:43210     ESTABLISHED",
				"udp        0      0 0.0.0.0:123             0.0.0.0:*               "
		));

		netstat.getListenPorts();
		netstat.getConnList();
		netstat.showList();
	}

	/**
	 * HP-UX 形式 netstat 出力のパーステスト。
	 */
	@Test
	public void testParseHpuxNetstat() {
		prop.setValue(ClsProperties.OS_NAME, "hpux");
		netstat.init();

		netstat.setStdOutList(Arrays.asList(
				"tcp        0      0  *.80                   *.*                    LISTEN",
				"tcp        0      0  192.168.1.50.80        192.168.1.100.54321    ESTABLISHED",
				"udp        0      0  *.53                   *.*                    "
		));

		netstat.getListenPorts();
		netstat.getConnList();
		netstat.showList();
	}

	/**
	 * ファイル読み込みテスト。
	 *
	 * @throws IOException 入出力例外
	 */
	@Test
	public void testReadFile() throws IOException {
		File sampleFile = tempDir.resolve("sample_netstat.txt").toFile();
		String sampleContent = "  TCP    0.0.0.0:135            0.0.0.0:0              LISTENING\n";
		prop.writeFile(sampleFile.getAbsolutePath(), sampleContent, false);

		int ret = netstat.readFile(sampleFile, Charset.forName("UTF-8"));
		assertEquals(0, ret);
		assertEquals(1, netstat.getStdOutList().size());
	}

	/**
	 * ループカウント設定・取得テスト。
	 */
	@Test
	public void testSetGetLoopCount() {
		netstat.setLoopCount(5);
		assertEquals(5, netstat.getLoopCount());

		netstat.setLoopCount(10);
		assertEquals(10, netstat.getLoopCount());
	}

}
