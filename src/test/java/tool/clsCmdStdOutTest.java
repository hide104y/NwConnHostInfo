package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * {@link clsCmdStdOut} の単体テストクラスです。
 */
public final class clsCmdStdOutTest {

	/**
	 * 標準出力文字列の追加および取得をテストします。
	 */
	@Test
	public void testAddAndGet() {
		clsCmdStdOut cmdStdOut = new clsCmdStdOut();
		cmdStdOut.add("Line 1");
		cmdStdOut.add("Line 2");
		cmdStdOut.add(null);

		List<String> list = cmdStdOut.getStdOutList();
		assertEquals(2, list.size());
		assertEquals("Line 1", list.get(0));
		assertEquals("Line 2", list.get(1));
	}

	/**
	 * 標準出力バッファのクリアをテストします。
	 */
	@Test
	public void testClear() {
		clsCmdStdOut cmdStdOut = new clsCmdStdOut();
		cmdStdOut.add("Sample");
		assertEquals(1, cmdStdOut.getStdOutList().size());

		cmdStdOut.clear();
		assertTrue(cmdStdOut.getStdOutList().isEmpty());
	}

	/**
	 * nullリスト設定時の安全なフォールバックをテストします。
	 */
	@Test
	public void testSetNullList() {
		clsCmdStdOut cmdStdOut = new clsCmdStdOut();
		cmdStdOut.setStdOutList(null);
		assertTrue(cmdStdOut.getStdOutList().isEmpty());
	}

}
