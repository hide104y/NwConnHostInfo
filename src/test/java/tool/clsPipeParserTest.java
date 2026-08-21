package tool;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * {@link clsPipeParser} の単体テストクラスです。
 */
public final class clsPipeParserTest {

	/**
	 * 入力ストリームからのパイプ読み込みと格納処理をテストします。
	 *
	 * @throws InterruptedException スレッド待機中断時
	 */
	@Test
	public void testRun() throws InterruptedException {
		String testData = "line1\nline2\nline3\n";
		ByteArrayInputStream is = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
		clsCmdStdOut stdOut = new clsCmdStdOut();

		clsPipeParser parser = new clsPipeParser(is, stdOut);
		parser.start();
		parser.join();

		assertEquals(3, stdOut.getStdOutList().size());
		assertEquals("line1", stdOut.getStdOutList().get(0));
		assertEquals("line2", stdOut.getStdOutList().get(1));
		assertEquals("line3", stdOut.getStdOutList().get(2));
	}

	/**
	 * nullストリーム指定時の安全な終了をテストします。
	 */
	@Test
	public void testRunWithNullStream() {
		clsCmdStdOut stdOut = new clsCmdStdOut();
		clsPipeParser parser = new clsPipeParser(null, stdOut);
		parser.run();
		assertEquals(0, stdOut.getStdOutList().size());
	}

}
