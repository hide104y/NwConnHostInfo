package tool;

/**
 * プロセスおよびアプリケーション情報のプロパティを保持するデータモデルクラスです。
 * <p>
 * ポートやネットワーク接続に関連付けられたプロセスID (PID)、プロセス名 (appName)、
 * 実行ファイルのフルパス (appPath) をカプセル化して管理します。
 * </p>
 *
 * <p>使用例:</p>
 * <pre>
 * ClsAppProp app = new ClsAppProp();
 * app.setPid(1024);
 * app.setAppName("httpd");
 * app.setAppPath("/usr/sbin/httpd");
 * System.out.println("PID: " + app.getPid() + ", Name: " + app.getAppName());
 * </pre>
 */
public class ClsAppProp {

	/** プロセスID (デフォルト: 0) */
	private int pid = 0;

	/** アプリケーション名 (デフォルト: "-") */
	private String appName = "-";

	/** アプリケーション実行パス (デフォルト: "-") */
	private String appPath = "-";

	/**
	 * デフォルトコンストラクタです。
	 * <p>
	 * 初期値として PID に 0、appName および appPath に "-" を設定します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * </pre>
	 */
	public ClsAppProp() {
	}

	/**
	 * プロセスID (PID) を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * int pid = prop.getPid();
	 * </pre>
	 *
	 * @return 設定されているプロセスID
	 */
	public int getPid() {
		return this.pid;
	}

	/**
	 * プロセスID (PID) を数値で設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * prop.setPid(1234);
	 * </pre>
	 *
	 * @param pid 設定するプロセスID (PID)
	 */
	public void setPid(int pid) {
		this.pid = pid;
	}

	/**
	 * 文字列形式のプロセスIDを解析して設定します。
	 * <p>
	 * 引数が null、空文字、または数値として不正な場合は 0 が設定されます。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * prop.setPid("1234");
	 * </pre>
	 *
	 * @param pidStr プロセスID文字列 (例: "1234")
	 */
	public void setPid(String pidStr) {
		if (pidStr == null || pidStr.trim().isEmpty()) {
			this.pid = 0;
			return;
		}
		try {
			this.pid = Integer.parseInt(pidStr.trim());
		} catch (NumberFormatException e) {
			this.pid = 0;
		}
	}

	/**
	 * アプリケーション名を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * String name = prop.getAppName();
	 * </pre>
	 *
	 * @return 設定されているアプリケーション名
	 */
	public String getAppName() {
		return this.appName;
	}

	/**
	 * アプリケーション名を設定します。
	 * <p>
	 * 引数が null の場合はデフォルト値 "-" が設定されます。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * prop.setAppName("java.exe");
	 * </pre>
	 *
	 * @param appName 設定するアプリケーション名
	 */
	public void setAppName(String appName) {
		this.appName = (appName != null ? appName : "-");
	}

	/**
	 * アプリケーション実行パスを取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * String path = prop.getAppPath();
	 * </pre>
	 *
	 * @return 設定されているアプリケーション実行パス
	 */
	public String getAppPath() {
		return this.appPath;
	}

	/**
	 * アプリケーション実行パスを設定します。
	 * <p>
	 * 引数が null の場合はデフォルト値 "-" が設定されます。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp prop = new ClsAppProp();
	 * prop.setAppPath("C:\\Program Files\\Java\\bin\\java.exe");
	 * </pre>
	 *
	 * @param appPath 設定するアプリケーション実行パス
	 */
	public void setAppPath(String appPath) {
		this.appPath = (appPath != null ? appPath : "-");
	}

}
