package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link clsNetstat} の単体テストクラスです。
 */
public final class clsNetstatTest {

	/** テスト用一時ディレクトリパス */
	private Path tempDir;

	/** プロパティ管理オブジェクト */
	private clsProperties prop;

	/** netstat管理オブジェクト */
	private clsNetstat netstat;

	/**
	 * 各テスト前の初期化処理。
	 *
	 * @throws IOException 一時ディレクトリ作成失敗時
	 */
	@Before
	public void setUp() throws IOException {
		tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "NwConnHostInfo", "clsNetstatTest");
		if (!Files.exists(tempDir)) {
			Files.createDirectories(tempDir);
		}
		prop = new clsProperties();
		prop.setValue(clsProperties.PATHDOUT, tempDir.toString());
		netstat = new clsNetstat(prop);
		netstat.init();
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
	 * 文字列のエンコード・デコード処理をテストします。
	 */
	@Test
	public void testEncodeDecode() {
		String original = "192.168.1.1:80";
		String encoded = netstat.encodeStr(original);
		assertEquals("192.168.1.1_80", encoded);
		assertEquals(original, netstat.decodeStr(encoded));

		assertEquals("*", netstat.decodeStr(netstat.encodeStr("*")));
	}

	/**
	 * ループバックアドレス判定をテストします。
	 */
	@Test
	public void testIsLoopbackAddr() {
		assertTrue(netstat.isLoopbackAddr(netstat.encodeStr("127.0.0.1")));
		assertTrue(netstat.isLoopbackAddr(netstat.encodeStr("[::1]")));
		assertTrue(netstat.isLoopbackAddr(netstat.encodeStr("::1")));
		assertFalse(netstat.isLoopbackAddr(netstat.encodeStr("192.168.1.1")));
	}

	/**
	 * IPv4アドレス形式判定をテストします。
	 */
	@Test
	public void testIsIpv4Address() {
		assertTrue(netstat.isIpv4Address("192.168.0.1"));
		assertTrue(netstat.isIpv4Address("10.0.0.1/24"));
		assertFalse(netstat.isIpv4Address("999.999.999.999"));
		assertFalse(netstat.isIpv4Address("2001:db8::1"));
		assertFalse(netstat.isIpv4Address("invalid"));
		assertFalse(netstat.isIpv4Address(null));
	}

	/**
	 * TCP/UDP各種ステータス判定をテストします。
	 */
	@Test
	public void testStates() {
		assertTrue(netstat.isTcpListen("LISTEN"));
		assertTrue(netstat.isTcpListen("LISTENING"));
		assertFalse(netstat.isTcpListen("ESTABLISHED"));

		assertTrue(netstat.isTcpConnState("ESTABLISHED"));
		assertFalse(netstat.isTcpConnState("LISTEN"));

		assertTrue(netstat.isUdpListen(netstat.encodeStr("*:*")));
	}

	/**
	 * PID文字列からのアプリケーションプロパティ取得をテストします。
	 */
	@Test
	public void testGetAppProp() {
		clsAppProp winProp = netstat.getAppProp("1234");
		assertNotNull(winProp);
		assertEquals(1234, winProp.getPid());

		clsAppProp linuxProp = netstat.getAppProp("5678/my_process");
		assertNotNull(linuxProp);
	}

	/**
	 * Windows環境向けnetstat出力パースをテストします。
	 */
	@Test
	public void testParseWindowsNetstat() {
		prop.setValue(clsProperties.OS_NAME, "win");
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
	 * Linux環境向けnetstat出力パースをテストします。
	 */
	@Test
	public void testParseLinuxNetstat() {
		prop.setValue(clsProperties.OS_NAME, "linux");
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
	 * ループカウント設定・取得をテストします。
	 */
	@Test
	public void testSetGetLoopCount() {
		netstat.setLoopCount(5);
		assertEquals(5, netstat.getLoopCount());

		netstat.setLoopCount(10);
		assertEquals(10, netstat.getLoopCount());
	}

}
