package tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * アプリケーションの設定情報やプロパティファイルの読み書き、OS判定を管理するクラスです。
 */
public final class clsProperties {

	/** Windows環境判定フラグ */
	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");

	/** ログレベル: DEBUG */
	public static final int LVL_DEBUG = -1;

	/** ログレベル: INFO */
	public static final int LVL_INFO = 0;

	/** ログレベル: WARN */
	public static final int LVL_WARN = 10;

	/** ログレベル: ERROR */
	public static final int LVL_ERROR = 20;

	/** ログレベル: FATAL */
	public static final int LVL_FATAL = 30;

	/** メッセージステータス: OK */
	public static final int MSG_OK = 0;

	/** メッセージステータス: SKIP */
	public static final int MSG_SKIP = 10;

	/** メッセージステータス: ERROR */
	public static final int MSG_ERROR = 20;

	/** ブロック種別: NONE */
	public static final int BLOCK_NONE = 0;

	/** ブロック種別: TCP */
	public static final int BLOCK_TCP = 1;

	/** ブロック種別: UDP */
	public static final int BLOCK_UDP = 2;

	/** ブロック種別: VALID */
	public static final int BLOCK_VALID = 3;

	/** OS識別子: その他 */
	public static final int OS_OTHER = 0;

	/** OS識別子: Windows */
	public static final int OS_WIN = 1;

	/** OS識別子: Linux */
	public static final int OS_LINUX = 2;

	/** OS識別子: HP-UX */
	public static final int OS_HPUX = 3;

	/** OS識別子: Solaris */
	public static final int OS_SOLARIS = 4;

	/** デフォルト冗長ログレベル */
	public static final int DEFAULT_VERBOSE = 0;

	/** デフォルトトレースログレベル */
	public static final int DEFAULT_IS_TRACE_LOG = 0;

	/** デフォルトタイムアウト秒数 */
	public static final int DEFAULT_TIMEOUT = 30;

	/** デフォルトタイムゾーン */
	public static final String DEFAULT_TIMEZONE = "Asia/Tokyo";

	/** デフォルト最大ループ回数 */
	public static final int DEFAULT_MAX_LOOP_COUNT = 1;

	/** デフォルト実行継続秒数 */
	public static final int DEFAULT_EXECUTION_TIME_SEC = 0;

	/** デフォルトスリープ秒数 */
	public static final int DEFAULT_SLEEP_SEC = 30;

	/** デフォルトタイムアウト秒数 */
	public static final int DEFAULT_TIMEOUT_SEC = 60;

	/** デフォルト実行コマンド */
	public static final String DEFAULT_COMMAND = "netstat";

	/** デフォルトコマンド引数 */
	public static final String DEFAULT_ARGUMENT = "-an";

	/** Windows向けPID取得コマンド引数 */
	public static final String DEFAULT_WIN_PID_ARGUMENT = "-ano";

	/** Linux向けPID取得コマンド引数 */
	public static final String DEFAULT_LINUX_PID_ARGUMENT = "-anp | awk '/^tcp|^udp/'";

	/** TCPブロック判定正規表現 */
	public static final String DEFAULT_TCP_BLOCK_REGEX = "^TCP: IPv[46]\\s*$";

	/** UDPブロック判定正規表現 */
	public static final String DEFAULT_UDP_BLOCK_REGEX = "^UDP: IPv[46]\\s*$";

	/** SCTPブロック判定正規表現 */
	public static final String DEFAULT_SCTP_BLOCK_REGEX = "^SCTP:\\s*$";

	/** Windows向けTCP接続抽出正規表現 */
	public static final String WIN_TCP_CONN_REGEX = "^\\s*TCP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w*)\\s*.*$";

	/** Windows向けUDP接続抽出正規表現 */
	public static final String WIN_UDP_CONN_REGEX = "^\\s*UDP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s*.*$";

	/** Linux向けTCP接続抽出正規表現 */
	public static final String LINUX_TCP_CONN_REGEX = "^\\s*tcp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w*)\\s*.*$";

	/** Linux向けUDP接続抽出正規表現 */
	public static final String LINUX_UDP_CONN_REGEX = "^\\s*udp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s*.*$";

	/** HP-UX向けTCP接続抽出正規表現 */
	public static final String HPUX_TCP_CONN_REGEX = "^\\s*tcp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s+(\\w*)\\s*.*$";

	/** HP-UX向けUDP接続抽出正規表現 */
	public static final String HPUX_UDP_CONN_REGEX = "^\\s*udp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s*.*$";

	/** Windows向けTCP接続抽出正規表現(PID付き) */
	public static final String WIN_TCP_CONN_PID_REGEX = "^\\s*TCP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w+)\\s+(\\w*)\\s*$";

	/** Windows向けUDP接続抽出正規表現(PID付き) */
	public static final String WIN_UDP_CONN_PID_REGEX = "^\\s*UDP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w*)\\s*$";

	/** Linux向けTCP接続抽出正規表現(PID付き) */
	public static final String LINUX_TCP_CONN_PID_REGEX = "^\\s*tcp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w+)\\s+(.*)\\s*$";

	/** Linux向けUDP接続抽出正規表現(PID付き) */
	public static final String LINUX_UDP_CONN_PID_REGEX = "^\\s*udp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(.*)\\s*$";

	/** Solaris向けTCP接続抽出正規表現 */
	public static final String SOL_TCP_CONN_REGEX = "^\\s*([^\\s]+)\\.([^\\s\\.]+)\\s+([^\\s]+)\\.([^\\s\\.]+)\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+([^\\s\\.]+)\\s*\\.*$";

	/** Solaris向けUDP接続抽出正規表現 */
	public static final String SOL_UDP_CONN_REGEX = "^\\s*([^\\s]+)\\.([^\\s\\.]+)\\s+([^\\s]+)\\s+([^\\s\\.]+)\\s+([^\\s]+)\\s*\\.*$";

	/** Solaris向けTCP LISTEN抽出正規表現 */
	public static final String SOL_TCP_LSTN_REGEX = SOL_TCP_CONN_REGEX;

	/** Solaris向けUDP LISTEN抽出正規表現 */
	public static final String SOL_UDP_LSTN_REGEX = "^\\s*([^\\s]+)\\.([^\\s\\.]+)\\s+([^\\s\\.]+)\\s*\\.*$";

	/** デフォルトOS名 */
	public static final String DEFAULT_OS_NAME = "win";

	/** デフォルト動作モード */
	public static final String DEFAULT_MODE = "exe";

	/** デフォルトファイルパースディレクトリ */
	public static final String DEFAULT_FILE_PARSE_DIR = "." + File.separator;

	/** デフォルトファイルパース対象名正規表現 */
	public static final String DEFAULT_FILE_PARSE_NAME = "netstat.*\\.txt";

	/** デフォルト高位ポート:ANY変換フラグ */
	public static final boolean DEFAULT_IS_PORT_ANY = true;

	/** プロパティキー: 冗長レベル */
	public static final String VERBOSE = "Verbose";

	/** プロパティキー: トレースログ出力 */
	public static final String IS_TRACE_LOG = "IsTraceLog";

	/** プロパティキー: 未定義キー警告 */
	public static final String IS_WARN_IF_KEY_NOT_FOUND = "IsWarnIfKeyNotFound";

	/** プロパティキー: 設定ファイルパス */
	public static final String PATHFCONF = "PathFConf";

	/** プロパティキー: 出力ディレクトリパス */
	public static final String PATHDOUT = "PathDOut";

	/** プロパティキー: リモートホストログ出力ディレクトリパス */
	public static final String PATHDRHOSTLOG = "PathDRHostLog";

	/** プロパティキー: リモートホストログ対象IP定義ファイルパス */
	public static final String PATHFRHOSTLOG_I_IP = "PathFRHostLogIncIpAddrs";

	/** プロパティキー: リモートホストログ除外IP定義ファイルパス */
	public static final String PATHFRHOSTLOG_X_IP = "PathFRHostLogExcIpAddrs";

	/** プロパティキー: 出力ファイル名 */
	public static final String OUTPUT_FILE_NAME = "OutputFileName";

	/** プロパティキー: 入力ファイル文字エンコーディング */
	public static final String INPUT_FILE_ENCODING = "InputFileEncoding";

	/** プロパティキー: 出力ファイル文字エンコーディング */
	public static final String OUTPUT_FILE_ENCODING = "OutputFileEncoding";

	/** プロパティキー: 設定ファイル文字エンコーディング */
	public static final String CNF_FILE_ENCODING = "ConfigFileEncoding";

	/** プロパティキー: タイムゾーン */
	public static final String TIMEZONE = "TimeZone";

	/** プロパティキー: 最大ループ回数 */
	public static final String MAX_LOOP_COUNT = "MaxLoopCount";

	/** プロパティキー: 実行継続秒数 */
	public static final String EXECUTION_TIME_SEC = "ExecutionTimeSec";

	/** プロパティキー: スリープ秒数 */
	public static final String SLEEP_SEC = "SleepSec";

	/** プロパティキー: タイムアウト秒数 */
	public static final String TIMEOUT = "Timeout";

	/** プロパティキー: 実行コマンド */
	public static final String COMMAND = "Command";

	/** プロパティキー: コマンド引数 */
	public static final String ARGUMENT = "Argument";

	/** プロパティキー: TCP接続抽出正規表現 */
	public static final String TCP_CONN_REGEX = "TcpConnRegex";

	/** プロパティキー: UDP接続抽出正規表現 */
	public static final String UDP_CONN_REGEX = "UdpConnRegex";

	/** プロパティキー: TCP LISTEN抽出正規表現 */
	public static final String TCP_LSTN_REGEX = "TcpLstnRegex";

	/** プロパティキー: UDP LISTEN抽出正規表現 */
	public static final String UDP_LSTN_REGEX = "UdpLstnRegex";

	/** プロパティキー: OS名 */
	public static final String OS_NAME = "OsName";

	/** プロパティキー: OS識別子ID */
	public static final String OS_ID = "OsId";

	/** プロパティキー: 動作モード */
	public static final String MODE = "Mode";

	/** プロパティキー: ファイルパースディレクトリパス */
	public static final String FILE_PARSE_DIR = "FileParseDri";

	/** プロパティキー: ファイルパース対象ファイル名 */
	public static final String FILE_PARSE_NAME = "FileParseName";

	/** プロパティキー: PID取得フラグ */
	public static final String IS_PID = "IsPid";

	/** プロパティキー: 高位ポート:ANY変換フラグ */
	public static final String IS_PORT_ANY = "IsPortAny";

	/** プロパティキー: TCPブロック正規表現 */
	public static final String TCP_BLOCK_REGEX = "TcpBlockRegex";

	/** プロパティキー: UDPブロック正規表現 */
	public static final String UDP_BLOCK_REGEX = "UdpBlockRegex";

	/** プロパティキー: SCTPブロック正規表現 */
	public static final String SCTP_BLOCK_REGEX = "SctpBlockRegex";

	/** プロパティキー: ホストIPアドレスCSV */
	public static final String HOST_IP_ADDR_CSV = "HostIPAddrCsv";

	/** プロパティキー: 開始時刻 (ミリ秒) */
	public static final String START_TIME_MSEC = "StartTimeMiliSec";

	/** ホストのIPアドレス一覧リスト */
	private List<String> ipAddrList = new ArrayList<>();

	/** プロパティキー・値マップ */
	private volatile LinkedHashMap<String, String> propMap = new LinkedHashMap<>();

	/** 未定義キー参照時の警告出力フラグ */
	private boolean isWarnIfKeyNotFound = false;

	/** 自マシンのホスト名 */
	private String hostName = "localhost";

	/**
	 * デフォルトコンストラクタ。自マシンのホスト名およびIPアドレスを取得して初期化します。
	 *
	 * <pre>
	 * clsProperties prop = new clsProperties();
	 * </pre>
	 */
	public clsProperties() {
		String name = execToString("hostname");
		if (name == null || name.isEmpty()) {
			try {
				name = InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {
				// ignore
				name = "localhost";
			}
		}
		this.hostName = (name != null && !name.trim().isEmpty()) ? name.trim().split("[\\s\\.]")[0] : "localhost";
		this.fetchIpAddrs(this.hostName);
	}

	/**
	 * ホストのIPアドレス一覧リストを設定します。
	 *
	 * <pre>
	 * clsProperties prop = new clsProperties();
	 * prop.setIpAddrList(Arrays.asList("192.168.1.1"));
	 * </pre>
	 *
	 * @param list IPアドレスのリスト
	 */
	public void setIpAddrList(final List<String> list) {
		this.ipAddrList = (list != null) ? list : new ArrayList<>();
	}

	/**
	 * ホスト名を設定します。
	 *
	 * <pre>
	 * clsProperties prop = new clsProperties();
	 * prop.setHostName("myserver");
	 * </pre>
	 *
	 * @param name ホスト名
	 */
	public void setHostName(final String name) {
		this.hostName = (name != null) ? name : "localhost";
	}

	/**
	 * ホストのIPアドレス一覧リストを取得します。
	 *
	 * <pre>
	 * List&lt;String&gt; ips = prop.getIpAddrList();
	 * </pre>
	 *
	 * @return IPアドレスリスト
	 */
	public List<String> getIpAddrList() {
		return this.ipAddrList;
	}

	/**
	 * 自マシンのホスト名を取得します。
	 *
	 * <pre>
	 * String host = prop.getHostName();
	 * </pre>
	 *
	 * @return ホスト名
	 */
	public String getHostName() {
		return this.hostName;
	}

	/**
	 * 指定されたパスにファイルが存在するか確認します。
	 *
	 * <pre>
	 * boolean exists = prop.isExist("config.properties");
	 * </pre>
	 *
	 * @param filePath 確認対象のファイルパス
	 * @return 存在すれば true、存在しなければ false
	 */
	public boolean isExist(final String filePath) {
		if (filePath != null && !filePath.trim().isEmpty()) {
			File path = new File(filePath);
			return path.exists();
		}
		return false;
	}

	/**
	 * 指定キーの文字列プロパティ値を取得します。
	 *
	 * <pre>
	 * String val = prop.getValue("Command", "netstat");
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合のデフォルト値
	 * @return プロパティ値
	 */
	public String getValue(final String key, final String defaultValue) {
		String value = String.valueOf(defaultValue);
		if (key != null && !key.isEmpty() && propMap.containsKey(key)) {
			value = propMap.get(key);
		} else {
			if (isWarnIfKeyNotFound && !propMap.containsKey(key)) {
				System.out.println("★★★ NOT FOUND KEY ★★★ : " + key);
			}
		}
		if ("null".equalsIgnoreCase(value)) {
			value = null;
		}
		return value;
	}

	/**
	 * 指定キーの boolean プロパティ値を取得します。
	 *
	 * <pre>
	 * boolean isPid = prop.getValue("IsPid", false);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return boolean値
	 */
	public boolean getValue(final String key, final boolean defaultValue) {
		boolean retVal = defaultValue;
		String value = getValue(key, String.valueOf(defaultValue));
		if ("true".equalsIgnoreCase(value)) {
			retVal = true;
		} else if ("false".equalsIgnoreCase(value)) {
			retVal = false;
		}
		return retVal;
	}

	/**
	 * 指定キーの int プロパティ値を取得します。
	 *
	 * <pre>
	 * int timeout = prop.getValue("Timeout", 30);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return int値
	 */
	public int getValue(final String key, final int defaultValue) {
		int retVal = defaultValue;
		try {
			retVal = Integer.parseInt(getValue(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException ex) {
			// ignore
			retVal = defaultValue;
		}
		return retVal;
	}

	/**
	 * 指定キーの long プロパティ値を取得します。
	 *
	 * <pre>
	 * long startTime = prop.getValue("StartTimeMiliSec", 0L);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return long値
	 */
	public long getValue(final String key, final long defaultValue) {
		long retVal = defaultValue;
		try {
			retVal = Long.parseLong(getValue(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException ex) {
			// ignore
			retVal = defaultValue;
		}
		return retVal;
	}

	/**
	 * 指定キーの Double プロパティ値を取得します。
	 *
	 * <pre>
	 * Double val = prop.getValue("Rate", 1.0);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return Double値
	 */
	public Double getValue(final String key, final Double defaultValue) {
		Double retVal = defaultValue;
		try {
			retVal = Double.parseDouble(getValue(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException ex) {
			// ignore
			retVal = defaultValue;
		}
		return retVal;
	}

	/**
	 * 指定キーの Charset プロパティ値を取得します。
	 *
	 * <pre>
	 * Charset cs = prop.getValue("OutputFileEncoding", Charset.forName("UTF-8"));
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルトのCharset
	 * @return Charsetオブジェクト
	 */
	public Charset getValue(final String key, final Charset defaultValue) {
		Charset retVal = defaultValue;
		String value = getValue(key, "");
		if (value == null || value.isEmpty()) {
			retVal = defaultValue;
		} else {
			try {
				retVal = Charset.forName(value);
			} catch (Exception ex) {
				// ignore
				retVal = defaultValue;
			}
		}
		return retVal;
	}

	/**
	 * boolean プロパティ値を設定します。
	 *
	 * <pre>
	 * prop.setValue("IsPid", true);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定値
	 */
	public void setValue(final String key, final boolean val) {
		propMap.put(key, val ? "true" : "false");
	}

	/**
	 * int プロパティ値を設定します。
	 *
	 * <pre>
	 * prop.setValue("Timeout", 60);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定値
	 */
	public void setValue(final String key, final int val) {
		propMap.put(key, String.valueOf(val));
	}

	/**
	 * long プロパティ値を設定します。
	 *
	 * <pre>
	 * prop.setValue("StartTimeMiliSec", System.currentTimeMillis());
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定値
	 */
	public void setValue(final String key, final long val) {
		propMap.put(key, String.valueOf(val));
	}

	/**
	 * String プロパティ値を設定します。
	 *
	 * <pre>
	 * prop.setValue("Command", "netstat");
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定値
	 */
	public void setValue(final String key, final String val) {
		propMap.put(key, val);
	}

	/**
	 * プロパティファイルを指定文字コードで読み込みます。
	 *
	 * <pre>
	 * boolean ok = prop.read("config.properties", "UTF-8");
	 * </pre>
	 *
	 * @param filePath ファイルパス
	 * @param encodingName 文字コード名
	 * @return 読み込み成功時は true、失敗時は false
	 */
	public boolean read(final String filePath, final String encodingName) {
		propMap.clear();
		String commentLineRegex = "^\\s*#.*";
		String keyValLineRegex = "^\\s*([\\w_\\-]+)\\s*=\\s*(.+)\\s*$";
		Pattern pttrn = Pattern.compile(keyValLineRegex);
		boolean isOk = true;

		try (FileInputStream fis = new FileInputStream(filePath);
			 InputStreamReader isr = new InputStreamReader(fis, Charset.forName(encodingName));
			 BufferedReader br = new BufferedReader(isr)) {
			String line = "";
			int i = 0;
			while ((line = br.readLine()) != null) {
				if (!line.matches(commentLineRegex)) {
					Matcher matcher = pttrn.matcher(line);
					if (matcher.find()) {
						String key = "" + matcher.group(1);
						String val = "" + matcher.group(2);
						if (!propMap.containsKey(key)) {
							if (0 < getValue(clsProperties.IS_TRACE_LOG, 0)) {
								System.out.println("CONF[" + String.format("%03d", i + 1) + "] PROPERTIES = " + key + " = " + val);
							}
							propMap.put(key, val);
						}
						if (clsProperties.IS_WARN_IF_KEY_NOT_FOUND.equals(key)) {
							if ("true".equalsIgnoreCase(val)) {
								isWarnIfKeyNotFound = true;
							}
						}
					}
				}
				i++;
			}
			if (0 < getValue(clsProperties.IS_TRACE_LOG, 0)) {
				System.out.println("_isWarnIfKeyNotFound = " + isWarnIfKeyNotFound);
			}
		} catch (IOException ioex) {
			isOk = false;
			System.err.println("IOEXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (Exception ex) {
			isOk = false;
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return isOk;
	}

	/**
	 * 保持しているプロパティ一覧を標準出力に表示します。
	 *
	 * <pre>
	 * prop.list();
	 * </pre>
	 */
	public void list() {
		propMap.forEach((k, v) -> System.out.println("# " + k + " = " + v));
	}

	/**
	 * ミリ秒単位のUNIXタイムスタンプを指定フォーマットの日時文字列に変換します。
	 *
	 * <pre>
	 * String dateStr = prop.convUnixToJst(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss");
	 * </pre>
	 *
	 * @param millisec ミリ秒UNIX時間
	 * @param formatStr 日時書式文字列
	 * @return フォーマット済み日時文字列
	 */
	public String convUnixToJst(final long millisec, final String formatStr) {
		String timeZoneStr = getValue(clsProperties.TIMEZONE, clsProperties.DEFAULT_TIMEZONE);
		try {
			ZoneId zoneId = ZoneId.of(timeZoneStr);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatStr).withZone(zoneId);
			return formatter.format(Instant.ofEpochMilli(millisec));
		} catch (Exception e) {
			// ignore fallback to SimpleDateFormat
			Date dt = new Date(millisec);
			TimeZone tz = TimeZone.getTimeZone(timeZoneStr);
			SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
			sdf.setTimeZone(tz);
			return sdf.format(dt);
		}
	}

	/**
	 * 秒単位のUNIXタイムスタンプを指定フォーマットの日時文字列に変換します。
	 *
	 * <pre>
	 * String dateStr = prop.convUnixToJst(1710000000, "yyyy/MM/dd HH:mm:ss");
	 * </pre>
	 *
	 * @param unixTime 秒単位UNIX時間
	 * @param formatStr 日時書式文字列
	 * @return フォーマット済み日時文字列
	 */
	public String convUnixToJst(final int unixTime, final String formatStr) {
		long lngMillisec = unixTime * 1000L;
		return convUnixToJst(lngMillisec, formatStr);
	}

	/**
	 * 文字列のトリム処理を行い、空文字の場合は null を返却します。
	 *
	 * <pre>
	 * String trimmed = prop.doTrim("  abc  ");
	 * </pre>
	 *
	 * @param str トリム対象文字列
	 * @return トリム後文字列（空文字時は null）
	 */
	public String doTrim(final String str) {
		if (str != null) {
			String trimmed = str.trim();
			if (!trimmed.isEmpty()) {
				return trimmed;
			}
		}
		return null;
	}

	/**
	 * カンマ等の区切り文字で渡された key=val 形式の文字列群を分解してプロパティにマージします。
	 *
	 * <pre>
	 * prop.splitMergeProp("key1=val1,key2=val2", ",");
	 * </pre>
	 *
	 * @param csv CSV形式プロパティ文字列
	 * @param delimiter 区切り文字
	 * @return 正常終了時は true、エラー時は false
	 */
	public boolean splitMergeProp(final String csv, final String delimiter) {
		if (csv == null || delimiter == null) {
			return false;
		}
		boolean isOk = true;
		try {
			String[] elem = csv.split(delimiter);
			int i = 0;
			for (String header : elem) {
				String[] keyValuePair = header.split("=");
				if (2 <= keyValuePair.length) {
					String key = keyValuePair[0].trim();
					String val = keyValuePair[1].trim();
					if (0 < getValue(clsProperties.IS_TRACE_LOG, 0)) {
						System.out.println("CONF[" + String.format("%03d", i + 1) + "] OPTIONS = " + key + ": " + val);
					}
					propMap.put(key, val);
					i++;
				}
			}
		} catch (Exception ex) {
			isOk = false;
			if (0 < getValue(clsProperties.IS_TRACE_LOG, 0)) {
				ex.printStackTrace();
			}
		}
		return isOk;
	}

	/**
	 * OS名を取得します。
	 *
	 * <pre>
	 * String osName = prop.getOsName();
	 * </pre>
	 *
	 * @return OS名文字列
	 */
	public String getOsName() {
		String retVal = "";
		try {
			retVal = getValue(clsProperties.OS_NAME, System.getProperty("os.name"));
		} catch (Exception e) {
			// ignore
			retVal = "";
		}
		return retVal;
	}

	/**
	 * OS判定用の短縮名（win, linux, hpux, solaris）を取得します。
	 *
	 * <pre>
	 * String osShort = prop.getOsShortName();
	 * </pre>
	 *
	 * @return OS短縮名
	 */
	public String getOsShortName() {
		String osName = getOsName().toLowerCase();
		String retVal = "";
		if (osName != null && !osName.isEmpty()) {
			if (osName.contains("win")) {
				retVal = "win";
			} else if (osName.contains("linux")) {
				retVal = "linux";
			} else if (osName.contains("hp-ux") || osName.contains("hpux")) {
				retVal = "hpux";
			} else if (osName.contains("solaris") || osName.contains("sunos")) {
				retVal = "solaris";
			}
		}
		return retVal;
	}

	/**
	 * OS短縮名からOS識別子IDを取得します。
	 *
	 * <pre>
	 * int osId = prop.getOsId("win");
	 * </pre>
	 *
	 * @param osName OS短縮名
	 * @return OS識別子ID（OS_WIN, OS_LINUX, OS_HPUX, OS_SOLARIS等）
	 */
	public int getOsId(final String osName) {
		int retVal = OS_WIN;
		if ("win".equals(osName)) {
			retVal = clsProperties.OS_WIN;
		} else if ("linux".equals(osName)) {
			retVal = clsProperties.OS_LINUX;
		} else if ("hpux".equals(osName)) {
			retVal = clsProperties.OS_HPUX;
		} else if ("solaris".equals(osName)) {
			retVal = clsProperties.OS_SOLARIS;
		}
		return retVal;
	}

	/**
	 * OSや設定に応じた netstat 実行コマンド引数を取得します。
	 *
	 * <pre>
	 * String args = prop.getCommandArgs();
	 * </pre>
	 *
	 * @return コマンド引数文字列
	 */
	public String getCommandArgs() {
		String retVal = getValue(clsProperties.ARGUMENT, clsProperties.DEFAULT_ARGUMENT);
		if (getValue(clsProperties.IS_PID, false)) {
			switch (getOsId(getValue(OS_NAME, getOsShortName()))) {
				case clsProperties.OS_WIN:
					retVal = getValue(clsProperties.ARGUMENT, clsProperties.DEFAULT_WIN_PID_ARGUMENT);
					break;
				case clsProperties.OS_LINUX:
					retVal = getValue(clsProperties.ARGUMENT, clsProperties.DEFAULT_LINUX_PID_ARGUMENT);
					break;
				default:
					break;
			}
		}
		return retVal;
	}

	/**
	 * OSに応じたTCP接続情報抽出用正規表現を取得します。
	 *
	 * <pre>
	 * String regex = prop.getTcpConnRegex();
	 * </pre>
	 *
	 * @return TCP抽出正規表現
	 */
	public String getTcpConnRegex() {
		String retVal = "";
		String shortName = getOsShortName();
		if ("win".equals(shortName)) {
			if (getValue(clsProperties.IS_PID, false)) {
				retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.WIN_TCP_CONN_PID_REGEX);
			} else {
				retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.WIN_TCP_CONN_REGEX);
			}
		} else if ("hpux".equals(shortName)) {
			retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.HPUX_TCP_CONN_REGEX);
		} else if ("solaris".equals(shortName)) {
			retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.SOL_TCP_CONN_REGEX);
		} else {
			if (getValue(clsProperties.IS_PID, false)) {
				retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.LINUX_TCP_CONN_PID_REGEX);
			} else {
				retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.LINUX_TCP_CONN_REGEX);
			}
		}
		return retVal;
	}

	/**
	 * OSに応じたUDP接続情報抽出用正規表現を取得します。
	 *
	 * <pre>
	 * String regex = prop.getUdpConnRegex();
	 * </pre>
	 *
	 * @return UDP抽出正規表現
	 */
	public String getUdpConnRegex() {
		String retVal = "";
		String shortName = getOsShortName();
		if ("win".equals(shortName)) {
			if (getValue(clsProperties.IS_PID, false)) {
				retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.WIN_UDP_CONN_PID_REGEX);
			} else {
				retVal = getValue(clsProperties.UDP_CONN_REGEX, clsProperties.WIN_UDP_CONN_REGEX);
			}
		} else if ("hpux".equals(shortName)) {
			retVal = getValue(clsProperties.UDP_CONN_REGEX, clsProperties.HPUX_UDP_CONN_REGEX);
		} else if ("solaris".equals(shortName)) {
			retVal = getValue(clsProperties.UDP_CONN_REGEX, clsProperties.SOL_UDP_CONN_REGEX);
		} else {
			if (getValue(clsProperties.IS_PID, false)) {
				retVal = getValue(clsProperties.TCP_CONN_REGEX, clsProperties.LINUX_UDP_CONN_PID_REGEX);
			} else {
				retVal = getValue(clsProperties.UDP_CONN_REGEX, clsProperties.LINUX_UDP_CONN_REGEX);
			}
		}
		return retVal;
	}

	/**
	 * 指定コマンドを実行し、標準出力を文字列として取得します。
	 *
	 * <pre>
	 * String out = prop.execToString("hostname");
	 * </pre>
	 *
	 * @param cmd 実行するコマンド文字列
	 * @return コマンド実行結果文字列
	 */
	public String execToString(final String cmd) {
		String retVal = "";
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A")) {
			retVal = s.hasNext() ? s.next() : "";
		} catch (Exception e) {
			// ignore
			retVal = "";
		}
		return retVal;
	}

	/**
	 * 指定ホスト名に関連付けられた全IPアドレスを取得して内部リストへ設定します。
	 *
	 * <pre>
	 * prop.fetchIpAddrs("localhost");
	 * </pre>
	 *
	 * @param host ホスト名
	 */
	public void fetchIpAddrs(final String host) {
		ipAddrList.clear();
		String targetHost = (host != null && !host.trim().isEmpty()) ? host : "localhost";
		try {
			InetAddress[] ia = InetAddress.getAllByName(targetHost);
			if (ia != null) {
				ipAddrList = Arrays.stream(ia)
						.map(addr -> addr.getHostAddress().trim())
						.filter(ip -> !ip.isEmpty())
						.collect(Collectors.toList());
			}
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * デフォルトの出力ファイル名を生成して返却します。
	 *
	 * <pre>
	 * String fileName = prop.getDefaultName();
	 * </pre>
	 *
	 * @return デフォルト出力ファイル名
	 */
	public String getDefaultName() {
		return "NwConnHostInfo_" + this.hostName + "." + convUnixToJst(getValue(clsProperties.START_TIME_MSEC, System.currentTimeMillis()), "yyyyMMdd.HHmmss") + ".txt";
	}

	/**
	 * OS IDに応じたデフォルト文字エンコーディング名を取得します。
	 *
	 * <pre>
	 * String enc = prop.getDefEncoding(clsProperties.OS_WIN);
	 * </pre>
	 *
	 * @param osId OS識別子ID
	 * @return エンコーディング名（UTF-8, MS932, EUC-JP等）
	 */
	public String getDefEncoding(final int osId) {
		String retVal = "UTF-8";
		switch (osId) {
			case OS_WIN:
				retVal = "MS932";
				break;
			case OS_HPUX:
			case OS_SOLARIS:
				retVal = "EUC-JP";
				break;
			default:
				break;
		}
		return retVal;
	}

	/**
	 * 指定ファイルの内容を文字列として全行読み込みます。
	 *
	 * <pre>
	 * String content = prop.readFile("data.txt");
	 * </pre>
	 *
	 * @param filePath ファイルパス
	 * @return ファイル内容文字列
	 */
	public String readFile(final String filePath) {
		StringBuilder sb = new StringBuilder();
		String enc = getValue(CNF_FILE_ENCODING, getDefEncoding(getOsId(getValue(OS_NAME, getOsShortName()))));
		try (FileInputStream fis = new FileInputStream(filePath);
			 InputStreamReader isr = new InputStreamReader(fis, enc);
			 BufferedReader br = new BufferedReader(isr)) {
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException ioex) {
			System.err.println("IOEXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (Exception ex) {
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * 指定ファイルの内容をコメント行（#始まり）を除外して行リストとして読み込みます。
	 *
	 * <pre>
	 * List&lt;String&gt; lines = prop.readFileToList("ip_list.txt");
	 * </pre>
	 *
	 * @param filePath ファイルパス
	 * @return 行文字列リスト
	 */
	public List<String> readFileToList(final String filePath) {
		List<String> bodyList = new ArrayList<>();
		String commentLineRegex = "^\\s*#.*";
		String enc = getValue(clsProperties.CNF_FILE_ENCODING, getDefEncoding(getOsId(getValue(OS_NAME, getOsShortName()))));
		try (FileInputStream fis = new FileInputStream(filePath);
			 InputStreamReader isr = new InputStreamReader(fis, enc);
			 BufferedReader br = new BufferedReader(isr)) {
			String line = null;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty() && !line.matches(commentLineRegex)) {
					bodyList.add(line);
				}
			}
		} catch (IOException ioex) {
			System.err.println("IOEXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (Exception ex) {
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return bodyList;
	}

	/**
	 * 指定ファイルにメッセージ文字列を出力・書き込みます。
	 *
	 * <pre>
	 * boolean ok = prop.writeFile("output.log", "message", true);
	 * </pre>
	 *
	 * @param filePath 出力先ファイルパス
	 * @param message 出力メッセージ
	 * @param isAppend 追記する場合は true、新規作成・上書きは false
	 * @return 正常終了時は true、エラー時は false
	 */
	public boolean writeFile(final String filePath, final String message, final boolean isAppend) {
		boolean isOk = true;
		String enc = getValue(clsProperties.OUTPUT_FILE_ENCODING, getDefEncoding(getOsId(getValue(OS_NAME, getOsShortName()))));
		try (FileOutputStream fos = new FileOutputStream(filePath, isAppend);
			 OutputStreamWriter osw = new OutputStreamWriter(fos, enc);
			 BufferedWriter bw = new BufferedWriter(osw)) {
			if (message != null) {
				bw.write(message);
			}
		} catch (IOException ioex) {
			isOk = false;
			System.err.println("IOEXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (Exception ex) {
			isOk = false;
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return isOk;
	}

	/**
	 * 文字列が整数数値としてパース可能か判定します。
	 *
	 * <pre>
	 * boolean isNum = prop.isNumber("12345");
	 * </pre>
	 *
	 * @param str 判定対象文字列
	 * @return 数値の場合は true、それ以外は false
	 */
	public boolean isNumber(final String str) {
		if (str == null || str.trim().isEmpty()) {
			return false;
		}
		try {
			Integer.parseInt(str.trim());
			return true;
		} catch (NumberFormatException e) {
			// ignore
			return false;
		}
	}

}
