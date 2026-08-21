package tool;

/**
 * プロセスおよびアプリケーション情報のプロパティを保持するデータクラスです。
 */
public final class clsAppProp {

	/** デフォルトPID値 */
	private static final int DEFAULT_PID = 0;

	/** デフォルト文字列値 */
	private static final String DEFAULT_VALUE = "-";

	/** プロセスID */
	private int pid = DEFAULT_PID;

	/** アプリケーション名 */
	private String appName = DEFAULT_VALUE;

	/** アプリケーションパス */
	private String appPath = DEFAULT_VALUE;

	/**
	 * デフォルトコンストラクタ。
	 *
	 * <pre>
	 * clsAppProp prop = new clsAppProp();
	 * </pre>
	 */
	public clsAppProp() {
	}

	/**
	 * プロセスID (PID) を設定します。
	 *
	 * <pre>
	 * clsAppProp prop = new clsAppProp();
	 * prop.setPid(1234);
	 * </pre>
	 *
	 * @param value プロセスID
	 */
	public void setPid(final int value) {
		this.pid = value;
	}

	/**
	 * 文字列形式のプロセスIDを解析して設定します。
	 *
	 * <pre>
	 * clsAppProp prop = new clsAppProp();
	 * prop.setPid("1234");
	 * </pre>
	 *
	 * @param pidStr プロセスID文字列
	 */
	public void setPid(final String pidStr) {
		if (pidStr == null || pidStr.trim().isEmpty()) {
			this.pid = DEFAULT_PID;
			return;
		}
		try {
			this.pid = Integer.parseInt(pidStr.trim());
		} catch (NumberFormatException e) {
			// ignore
			this.pid = DEFAULT_PID;
		}
	}

	/**
	 * アプリケーション名を設定します。
	 *
	 * <pre>
	 * clsAppProp prop = new clsAppProp();
	 * prop.setAppName("java.exe");
	 * </pre>
	 *
	 * @param value アプリケーション名
	 */
	public void setAppName(final String value) {
		this.appName = (value != null) ? value : DEFAULT_VALUE;
	}

	/**
	 * アプリケーション実行パスを設定します。
	 *
	 * <pre>
	 * clsAppProp prop = new clsAppProp();
	 * prop.setAppPath("C:\\Program Files\\Java\\bin\\java.exe");
	 * </pre>
	 *
	 * @param value アプリケーションパス
	 */
	public void setAppPath(final String value) {
		this.appPath = (value != null) ? value : DEFAULT_VALUE;
	}

	/**
	 * プロセスID (PID) を取得します。
	 *
	 * <pre>
	 * int pid = prop.getPid();
	 * </pre>
	 *
	 * @return プロセスID
	 */
	public int getPid() {
		return this.pid;
	}

	/**
	 * アプリケーション名を取得します。
	 *
	 * <pre>
	 * String name = prop.getAppName();
	 * </pre>
	 *
	 * @return アプリケーション名
	 */
	public String getAppName() {
		return this.appName;
	}

	/**
	 * アプリケーション実行パスを取得します。
	 *
	 * <pre>
	 * String path = prop.getAppPath();
	 * </pre>
	 *
	 * @return アプリケーションパス
	 */
	public String getAppPath() {
		return this.appPath;
	}

}
