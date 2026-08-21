package tool;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * {@link clsAppProp} の単体テストクラスです。
 */
public final class clsAppPropTest {

	/**
	 * ゲッターおよびセッターの基本動作をテストします。
	 */
	@Test
	public void testGettersAndSetters() {
		clsAppProp prop = new clsAppProp();
		prop.setPid(100);
		prop.setAppName("testApp");
		prop.setAppPath("/bin/testApp");

		assertEquals(100, prop.getPid());
		assertEquals("testApp", prop.getAppName());
		assertEquals("/bin/testApp", prop.getAppPath());
	}

	/**
	 * 文字列形式PIDの解析設定をテストします。
	 */
	@Test
	public void testSetPidString() {
		clsAppProp prop = new clsAppProp();
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
	 * null設定時のフォールバック動作をテストします。
	 */
	@Test
	public void testNullSafety() {
		clsAppProp prop = new clsAppProp();
		prop.setAppName(null);
		prop.setAppPath(null);

		assertEquals("-", prop.getAppName());
		assertEquals("-", prop.getAppPath());
	}

}
