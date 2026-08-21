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
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * アプリケーションの設定情報、プロパティファイルの読み書き、OS判定および環境情報の管理を行うクラスです。
 * <p>
 * コマンドライン引数や設定ファイル (INI/プロパティ形式) から読み込まれたキーと値のペアを保持し、
 * 型変換 (String, boolean, int, long, Double, Charset) を伴うプロパティ値の取得、
 * 各OS（Windows, Linux, HP-UX, Solaris）に応じた正規表現やコマンド引数の自動判別を提供します。
 * </p>
 *
 * <p>使用例:</p>
 * <pre>
 * ClsProperties prop = new ClsProperties();
 * prop.read("config.conf", "UTF-8");
 * String cmd = prop.getValue(ClsProperties.COMMAND, "netstat");
 * int timeout = prop.getValue(ClsProperties.TIMEOUT, 30);
 * System.out.println("Command: " + cmd + ", Timeout: " + timeout);
 * </pre>
 */
public class ClsProperties {

	/** 実行環境がWindows系OSであるかどうかの真偽値 */
	public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");

	/** ログレベル: デバッグ (-1) */
	public static final int LVL_DEBUG = -1;
	/** ログレベル: 情報 (0) */
	public static final int LVL_INFO = 0;
	/** ログレベル: 警告 (10) */
	public static final int LVL_WARN = 10;
	/** ログレベル: エラー (20) */
	public static final int LVL_ERROR = 20;
	/** ログレベル: 致命的 (30) */
	public static final int LVL_FATAL = 30;

	/** メッセージコード: OK (0) */
	public static final int MSG_OK = 0;
	/** メッセージコード: スキップ (10) */
	public static final int MSG_SKIP = 10;
	/** メッセージコード: エラー (20) */
	public static final int MSG_ERROR = 20;

	/** ブロック種別: なし (0) */
	public static final int BLOCK_NONE = 0;
	/** ブロック種別: TCPブロック (1) */
	public static final int BLOCK_TCP = 1;
	/** ブロック種別: UDPブロック (2) */
	public static final int BLOCK_UDP = 2;
	/** ブロック種別: 有効ブロック (3) */
	public static final int BLOCK_VALID = 3;

	/** OS種別ID: その他 (0) */
	public static final int OS_OTHER = 0;
	/** OS種別ID: Windows (1) */
	public static final int OS_WIN = 1;
	/** OS種別ID: Linux (2) */
	public static final int OS_LINUX = 2;
	/** OS種別ID: HP-UX (3) */
	public static final int OS_HPUX = 3;
	/** OS種別ID: Solaris (4) */
	public static final int OS_SOLARIS = 4;

	/** 冗長レベルデフォルト値: 0 */
	public static final int DEFAULT_VERBOSE = 0;
	/** トレースログ出力デフォルト値: 0 */
	public static final int DEFAULT_IS_TRACE_LOG = 0;
	/** タイムアウト秒デフォルト値: 30 */
	public static final int DEFAULT_TIMEOUT = 30;
	/** タイムゾーンデフォルト値: "Asia/Tokyo" */
	public static final String DEFAULT_TIMEZONE = "Asia/Tokyo";
	/** ループ実行回数デフォルト値: 1 */
	public static final int DEFAULT_MAX_LOOP_COUNT = 1;
	/** 実行時間（秒）デフォルト値: 0 */
	public static final int DEFAULT_EXECUTION_TIME_SEC = 0;
	/** ループ間スリープ秒デフォルト値: 30 */
	public static final int DEFAULT_SLEEP_SEC = 30;
	/** タイムアウト秒デフォルト値: 60 */
	public static final int DEFAULT_TIMEOUT_SEC = 60;
	/** 実行コマンドデフォルト値: "netstat" */
	public static final String DEFAULT_COMMAND = "netstat";
	/** netstatコマンド引数デフォルト値: "-an" */
	public static final String DEFAULT_ARGUMENT = "-an";
	/** Windows用PID取得引数デフォルト値: "-ano" */
	public static final String DEFAULT_WIN_PID_ARGUMENT = "-ano";
	/** Linux用PID取得引数デフォルト値: "-anp | awk '/^tcp|^udp/'" */
	public static final String DEFAULT_LINUX_PID_ARGUMENT = "-anp | awk '/^tcp|^udp/'";

	/** Solaris用TCPブロック行抽出正規表現デフォルト値 */
	public static final String DEFAULT_TCP_BLOCK_REGEX = "^TCP: IPv[46]\\s*$";
	/** Solaris用UDPブロック行抽出正規表現デフォルト値 */
	public static final String DEFAULT_UDP_BLOCK_REGEX = "^UDP: IPv[46]\\s*$";
	/** Solaris用SCTPブロック行抽出正規表現デフォルト値 */
	public static final String DEFAULT_SCTP_BLOCK_REGEX = "^SCTP:\\s*$";

	/** Windows用TCP接続行正規表現 */
	public static final String WIN_TCP_CONN_REGEX = "^\\s*TCP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w*)\\s*.*$";
	/** Windows用UDP接続行正規表現 */
	public static final String WIN_UDP_CONN_REGEX = "^\\s*UDP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s*.*$";
	/** Linux用TCP接続行正規表現 */
	public static final String LINUX_TCP_CONN_REGEX = "^\\s*tcp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w*)\\s*.*$";
	/** Linux用UDP接続行正規表現 */
	public static final String LINUX_UDP_CONN_REGEX = "^\\s*udp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s*.*$";
	/** HP-UX用TCP接続行正規表現 */
	public static final String HPUX_TCP_CONN_REGEX = "^\\s*tcp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s+(\\w*)\\s*.*$";
	/** HP-UX用UDP接続行正規表現 */
	public static final String HPUX_UDP_CONN_REGEX = "^\\s*udp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s+([^\\s]+)\\.([^\\s:\\.]+)\\s*.*$";

	/** Windows用PID付きTCP接続行正規表現 */
	public static final String WIN_TCP_CONN_PID_REGEX = "^\\s*TCP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w+)\\s+(\\w*)\\s*$";
	/** Windows用PID付きUDP接続行正規表現 */
	public static final String WIN_UDP_CONN_PID_REGEX = "^\\s*UDP\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w*)\\s*$";
	/** Linux用PID付きTCP接続行正規表現 */
	public static final String LINUX_TCP_CONN_PID_REGEX = "^\\s*tcp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(\\w+)\\s+(.*)\\s*$";
	/** Linux用PID付きUDP接続行正規表現 */
	public static final String LINUX_UDP_CONN_PID_REGEX = "^\\s*udp\\d*\\s+\\d+\\s+\\d+\\s+([^\\s]+):([^\\s:]+)\\s+([^\\s]+):([^\\s:]+)\\s+(.*)\\s*$";

	/** Solaris用TCP接続行正規表現 */
	public static final String SOL_TCP_CONN_REGEX = "^\\s*([^\\s]+)\\.([^\\s\\.]+)\\s+([^\\s]+)\\.([^\\s\\.]+)\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+([^\\s\\.]+)\\s*\\.*$";
	/** Solaris用UDP接続行正規表現 */
	public static final String SOL_UDP_CONN_REGEX = "^\\s*([^\\s]+)\\.([^\\s\\.]+)\\s+([^\\s]+)\\s+([^\\s\\.]+)\\s+([^\\s]+)\\s*\\.*$";
	/** Solaris用TCP LISTEN行正規表現 */
	public static final String SOL_TCP_LSTN_REGEX = SOL_TCP_CONN_REGEX;
	/** Solaris用UDP LISTEN行正規表現 */
	public static final String SOL_UDP_LSTN_REGEX = "^\\s*([^\\s]+)\\.([^\\s\\.]+)\\s+([^\\s\\.]+)\\s*\\.*$";

	/** OS名デフォルト値: "win" */
	public static final String DEFAULT_OS_NAME = "win";
	/** 動作モードデフォルト値: "exe" */
	public static final String DEFAULT_MODE = "exe";
	/** ファイルパースディレクトリデフォルト値: ".\\" */
	public static final String DEFAULT_FILE_PARSE_DIR = "." + File.separator;
	/** ファイルパースファイル名パターンデフォルト値: "netstat.*\\.txt" */
	public static final String DEFAULT_FILE_PARSE_NAME = "netstat.*\\.txt";
	/** ポート番号ANY変換フラグデフォルト値: true */
	public static final boolean DEFAULT_IS_PORT_ANY = true;

	/** 設定キー: 冗長ログ出力レベル (Verbose) */
	public static final String VERBOSE = "Verbose";
	/** 設定キー: トレースログ出力レベル (IsTraceLog) */
	public static final String IS_TRACE_LOG = "IsTraceLog";
	/** 設定キー: 未定義キー存在時の警告フラグ (IsWarnIfKeyNotFound) */
	public static final String IS_WARN_IF_KEY_NOT_FOUND = "IsWarnIfKeyNotFound";
	/** 設定キー: 設定ファイルパス (PathFConf) */
	public static final String PATHFCONF = "PathFConf";
	/** 設定キー: 結果出力ディレクトリ (PathDOut) */
	public static final String PATHDOUT = "PathDOut";
	/** 設定キー: リモートホスト別ログ出力ディレクトリ (PathDRHostLog) */
	public static final String PATHDRHOSTLOG = "PathDRHostLog";
	/** 設定キー: リモートホストログ対象IPリストファイルパス (PathFRHostLogIncIpAddrs) */
	public static final String PATHFRHOSTLOG_I_IP = "PathFRHostLogIncIpAddrs";
	/** 設定キー: リモートホストログ除外IPリストファイルパス (PathFRHostLogExcIpAddrs) */
	public static final String PATHFRHOSTLOG_X_IP = "PathFRHostLogExcIpAddrs";
	/** 設定キー: 出力ファイル名 (OutputFileName) */
	public static final String OUTPUT_FILE_NAME = "OutputFileName";
	/** 設定キー: 入力ファイル文字コード (InputFileEncoding) */
	public static final String INPUT_FILE_ENCODING = "InputFileEncoding";
	/** 設定キー: 出力ファイル文字コード (OutputFileEncoding) */
	public static final String OUTPUT_FILE_ENCODING = "OutputFileEncoding";
	/** 設定キー: 設定ファイル文字コード (ConfigFileEncoding) */
	public static final String CNF_FILE_ENCODING = "ConfigFileEncoding";
	/** 設定キー: タイムゾーン (TimeZone) */
	public static final String TIMEZONE = "TimeZone";
	/** 設定キー: ループ実行回数 (MaxLoopCount) */
	public static final String MAX_LOOP_COUNT = "MaxLoopCount";
	/** 設定キー: 実行時間（秒） (ExecutionTimeSec) */
	public static final String EXECUTION_TIME_SEC = "ExecutionTimeSec";
	/** 設定キー: ループ間スリープ秒 (SleepSec) */
	public static final String SLEEP_SEC = "SleepSec";
	/** 設定キー: タイムアウト秒 (Timeout) */
	public static final String TIMEOUT = "Timeout";
	/** 設定キー: 実行コマンド (Command) */
	public static final String COMMAND = "Command";
	/** 設定キー: 実行コマンド引数 (Argument) */
	public static final String ARGUMENT = "Argument";
	/** 設定キー: TCP接続行抽出正規表現 (TcpConnRegex) */
	public static final String TCP_CONN_REGEX = "TcpConnRegex";
	/** 設定キー: UDP接続行抽出正規表現 (UdpConnRegex) */
	public static final String UDP_CONN_REGEX = "UdpConnRegex";
	/** 設定キー: TCP LISTEN行抽出正規表現 (TcpLstnRegex) */
	public static final String TCP_LSTN_REGEX = "TcpLstnRegex";
	/** 設定キー: UDP LISTEN行抽出正規表現 (UdpLstnRegex) */
	public static final String UDP_LSTN_REGEX = "UdpLstnRegex";
	/** 設定キー: OS名 (OsName) */
	public static final String OS_NAME = "OsName";
	/** 設定キー: OS種別ID (OsId) */
	public static final String OS_ID = "OsId";
	/** 設定キー: 動作モード (Mode) */
	public static final String MODE = "Mode";
	/** 設定キー: ファイルパースディレクトリ (FileParseDri) */
	public static final String FILE_PARSE_DIR = "FileParseDri";
	/** 設定キー: ファイルパース対象ファイル名 (FileParseName) */
	public static final String FILE_PARSE_NAME = "FileParseName";
	/** 設定キー: PID取得モードフラグ (IsPid) */
	public static final String IS_PID = "IsPid";
	/** 設定キー: ポート番号ANY変換フラグ (IsPortAny) */
	public static final String IS_PORT_ANY = "IsPortAny";
	/** 設定キー: TCPブロック抽出正規表現 (TcpBlockRegex) */
	public static final String TCP_BLOCK_REGEX = "TcpBlockRegex";
	/** 設定キー: UDPブロック抽出正規表現 (UdpBlockRegex) */
	public static final String UDP_BLOCK_REGEX = "UdpBlockRegex";
	/** 設定キー: SCTPブロック抽出正規表現 (SctpBlockRegex) */
	public static final String SCTP_BLOCK_REGEX = "SctpBlockRegex";
	/** 設定キー: 自ホストIPアドレスCSV (HostIPAddrCsv) */
	public static final String HOST_IP_ADDR_CSV = "HostIPAddrCsv";
	/** 設定キー: 実行開始時刻ミリ秒 (StartTimeMiliSec) */
	public static final String START_TIME_MSEC = "StartTimeMiliSec";

	/** 自ホストのIPアドレス一覧リスト */
	private List<String> ipAddrList = new ArrayList<>();

	/** プロパティキーと値のマップ */
	private volatile Map<String, String> propMap = new LinkedHashMap<>();

	/** 未定義キー参照時の警告出力フラグ */
	private boolean isWarnIfKeyNotFound = false;

	/** 自ホストのホスト名 */
	private String hostName = "localhost";

	/**
	 * デフォルトコンストラクタです。
	 * <p>
	 * 自マシンのホスト名および全ネットワークインターフェースのIPアドレスを自動検知して初期化します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * System.out.println("Hostname: " + prop.getHostName());
	 * </pre>
	 */
	public ClsProperties() {
		String name = execToString("hostname");
		if (name == null || name.isEmpty()) {
			try {
				name = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				name = "localhost";
			}
		}
		this.hostName = (name != null && !name.trim().isEmpty()) ? name.trim().split("[\\s\\.]")[0] : "localhost";
		this.fetchIpAddrs(this.hostName);
	}

	/**
	 * ホストのIPアドレス一覧リストを取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * List&lt;String&gt; ips = prop.getIpAddrList();
	 * for (String ip : ips) {
	 *     System.out.println("IP: " + ip);
	 * }
	 * </pre>
	 *
	 * @return IPアドレス文字列のリスト
	 */
	public List<String> getIpAddrList() {
		return this.ipAddrList;
	}

	/**
	 * ホストのIPアドレス一覧リストを設定します。
	 * <p>
	 * 引数が null の場合は空の ArrayList が設定されます。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * List&lt;String&gt; ips = new ArrayList&lt;String&gt;();
	 * ips.add("192.168.1.10");
	 * prop.setIpAddrList(ips);
	 * </pre>
	 *
	 * @param list 設定するIPアドレス文字列のリスト
	 */
	public void setIpAddrList(List<String> list) {
		this.ipAddrList = (list != null ? list : new ArrayList<String>());
	}

	/**
	 * 自マシンのホスト名を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String host = prop.getHostName();
	 * </pre>
	 *
	 * @return ホスト名文字列
	 */
	public String getHostName() {
		return this.hostName;
	}

	/**
	 * ホスト名を設定します。
	 * <p>
	 * 引数が null の場合はデフォルト値 "localhost" が設定されます。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.setHostName("myserver01");
	 * </pre>
	 *
	 * @param name 設定するホスト名
	 */
	public void setHostName(String name) {
		this.hostName = (name != null ? name : "localhost");
	}

	/**
	 * 指定されたパスにファイルが存在するか確認します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * if (prop.isExist("config.conf")) {
	 *     prop.read("config.conf", "UTF-8");
	 * }
	 * </pre>
	 *
	 * @param filePath 確認対象のファイルパス
	 * @return ファイルが存在する場合は true、存在しないまたはパスが無効な場合は false
	 */
	public boolean isExist(String filePath) {
		if (filePath != null && !filePath.trim().isEmpty()) {
			File path = new File(filePath);
			return path.exists();
		}
		return false;
	}

	/**
	 * 指定キーの文字列プロパティ値を取得します。
	 * <p>
	 * キーが存在しない場合は指定されたデフォルト値を返却します。
	 * 値が "null" (大文字小文字不問) の場合は null を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String val = prop.getValue("Command", "netstat");
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合のデフォルト値
	 * @return プロパティ値文字列
	 */
	public String getValue(String key, String defaultValue) {
		String value = String.valueOf(defaultValue);
		if (key != null && !key.isEmpty() && propMap.containsKey(key)) {
			value = propMap.get(key);
		} else {
			if (isWarnIfKeyNotFound && (key == null || !propMap.containsKey(key))) {
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
	 * <p>
	 * 値が "true" の場合は true、"false" の場合は false、それ以外は defaultValue を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * boolean isPid = prop.getValue(ClsProperties.IS_PID, false);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return boolean値
	 */
	public boolean getValue(String key, boolean defaultValue) {
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
	 * <p>
	 * パースに失敗した場合は defaultValue を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * int count = prop.getValue(ClsProperties.MAX_LOOP_COUNT, 1);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return int値
	 */
	public int getValue(String key, int defaultValue) {
		int retVal = defaultValue;
		try {
			retVal = Integer.parseInt(getValue(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException ex) {
			retVal = defaultValue;
		}
		return retVal;
	}

	/**
	 * 指定キーの long プロパティ値を取得します。
	 * <p>
	 * パースに失敗した場合は defaultValue を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * long startTime = prop.getValue(ClsProperties.START_TIME_MSEC, 0L);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return long値
	 */
	public long getValue(String key, long defaultValue) {
		long retVal = defaultValue;
		try {
			retVal = Long.parseLong(getValue(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException ex) {
			retVal = defaultValue;
		}
		return retVal;
	}

	/**
	 * 指定キーの Double プロパティ値を取得します。
	 * <p>
	 * パースに失敗した場合は defaultValue を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * Double rate = prop.getValue("Rate", 1.0);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルト値
	 * @return Double値
	 */
	public Double getValue(String key, Double defaultValue) {
		Double retVal = defaultValue;
		try {
			retVal = Double.parseDouble(getValue(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException | NullPointerException ex) {
			retVal = defaultValue;
		}
		return retVal;
	}

	/**
	 * 指定キーの Charset プロパティ値を取得します。
	 * <p>
	 * 設定値から Charset を生成できない場合は defaultValue を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * Charset cs = prop.getValue(ClsProperties.OUTPUT_FILE_ENCODING, Charset.forName("UTF-8"));
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param defaultValue デフォルトのCharset
	 * @return Charsetオブジェクト
	 */
	public Charset getValue(String key, Charset defaultValue) {
		Charset retVal = defaultValue;
		String value = getValue(key, "");
		if (value == null || value.isEmpty()) {
			retVal = defaultValue;
		} else {
			try {
				retVal = Charset.forName(value);
			} catch (IllegalArgumentException ex) {
				retVal = defaultValue;
			}
		}
		return retVal;
	}

	/**
	 * boolean 型のプロパティ値を設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.setValue(ClsProperties.IS_PID, true);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定する真偽値
	 */
	public void setValue(String key, boolean val) {
		propMap.put(key, (val ? "true" : "false"));
	}

	/**
	 * int 型のプロパティ値を設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.setValue(ClsProperties.TIMEOUT, 60);
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定する整数値
	 */
	public void setValue(String key, int val) {
		propMap.put(key, String.valueOf(val));
	}

	/**
	 * long 型のプロパティ値を設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.setValue(ClsProperties.START_TIME_MSEC, System.currentTimeMillis());
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定するlong値
	 */
	public void setValue(String key, long val) {
		propMap.put(key, String.valueOf(val));
	}

	/**
	 * String 型のプロパティ値を設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.setValue(ClsProperties.COMMAND, "netstat");
	 * </pre>
	 *
	 * @param key プロパティキー
	 * @param val 設定する文字列値
	 */
	public void setValue(String key, String val) {
		propMap.put(key, val);
	}

	/**
	 * プロパティファイルを指定文字コードで読み込み、内部マップに格納します。
	 * <p>
	 * コメント行（#始まり）を除外し、"key = value" 形式の行を解析します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * boolean success = prop.read("config.conf", "UTF-8");
	 * </pre>
	 *
	 * @param filePath 読み込み対象ファイルパス
	 * @param encodingName 文字コード名 (例: "UTF-8", "MS932")
	 * @return 読み込み成功時は true、失敗時は false
	 */
	public boolean read(String filePath, String encodingName) {
		propMap.clear();
		String commentLineRegex = "^\\s*#.*";
		String keyValLineRegex = "^\\s*([\\w_\\-]+)\\s*=\\s*(.+)\\s*$";
		Pattern pttrn = Pattern.compile(keyValLineRegex);
		boolean isOk = true;

		try (FileInputStream fis = new FileInputStream(filePath);
			 InputStreamReader isr = new InputStreamReader(fis, Charset.forName(encodingName));
			 BufferedReader br = new BufferedReader(isr)) {
			String line;
			int i = 0;
			while ((line = br.readLine()) != null) {
				if (!line.matches(commentLineRegex)) {
					Matcher matcher = pttrn.matcher(line);
					if (matcher.find()) {
						String key = matcher.group(1);
						String val = matcher.group(2);
						if (!propMap.containsKey(key)) {
							if (getValue(ClsProperties.IS_TRACE_LOG, 0) > 0) {
								System.out.println("CONF[" + String.format("%03d", i + 1) + "] PROPERTIES = " + key + " = " + val);
							}
							propMap.put(key, val);
						}
						if (ClsProperties.IS_WARN_IF_KEY_NOT_FOUND.equals(key)) {
							if ("true".equalsIgnoreCase(val)) {
								isWarnIfKeyNotFound = true;
							}
						}
					}
				}
				i++;
			}
			if (getValue(ClsProperties.IS_TRACE_LOG, 0) > 0) {
				System.out.println("_isWarnIfKeyNotFound = " + isWarnIfKeyNotFound);
			}
		} catch (IOException ioex) {
			isOk = false;
			System.err.println("IOEXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (RuntimeException ex) {
			isOk = false;
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return isOk;
	}

	/**
	 * 保持している全プロパティのキーと値を標準出力へフォーマット表示します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.list();
	 * </pre>
	 */
	public void list() {
		for (Map.Entry<String, String> entry : propMap.entrySet()) {
			System.out.println("# " + entry.getKey() + " = " + entry.getValue());
		}
	}

	/**
	 * ミリ秒単位の UNIX タイムスタンプを指定フォーマットの日時文字列に変換します。
	 * <p>
	 * 設定されたタイムゾーン（デフォルト: Asia/Tokyo）に基づいてフォーマットします。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String dateStr = prop.convUnixToJst(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss");
	 * </pre>
	 *
	 * @param millisec ミリ秒単位のUNIXエポックミリ秒
	 * @param formatStr 日時書式パターン (例: "yyyy/MM/dd HH:mm:ss")
	 * @return フォーマットされた日時文字列
	 */
	public String convUnixToJst(long millisec, String formatStr) {
		String timeZoneStr = getValue(ClsProperties.TIMEZONE, ClsProperties.DEFAULT_TIMEZONE);
		Date dt = new Date(millisec);
		TimeZone tz = TimeZone.getTimeZone(timeZoneStr);
		SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
		sdf.setTimeZone(tz);
		return sdf.format(dt);
	}

	/**
	 * 秒単位の UNIX タイムスタンプを指定フォーマットの日時文字列に変換します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String dateStr = prop.convUnixToJst(1710000000, "yyyy/MM/dd HH:mm:ss");
	 * </pre>
	 *
	 * @param unixTime 秒単位のUNIXエポック秒
	 * @param formatStr 日時書式パターン (例: "yyyy/MM/dd HH:mm:ss")
	 * @return フォーマットされた日時文字列
	 */
	public String convUnixToJst(int unixTime, String formatStr) {
		long lngMillisec = unixTime * 1000L;
		return convUnixToJst(lngMillisec, formatStr);
	}

	/**
	 * 文字列の前後の空白を除去し、空文字になった場合は null を返却します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String res = prop.doTrim("  hello  "); // "hello"
	 * String nullRes = prop.doTrim("   "); // null
	 * </pre>
	 *
	 * @param str トリム対象の文字列
	 * @return トリム後文字列（空文字時は null、引数が null の場合は null）
	 */
	public String doTrim(String str) {
		if (str != null) {
			String trimmed = str.trim();
			if (!trimmed.isEmpty()) {
				return trimmed;
			}
		}
		return null;
	}

	/**
	 * 区切り文字で渡された key=value 形式の文字列群を分解し、プロパティマップにマージします。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.splitMergeProp("Timeout=60,Verbose=2", ",");
	 * </pre>
	 *
	 * @param csv key=val 形式の文字列群 (例: "Timeout=60,Verbose=2")
	 * @param delimiter 分割用の正規表現区切り文字 (例: "[,|]")
	 * @return 正常にマージ完了した場合は true、失敗時は false
	 */
	public boolean splitMergeProp(String csv, String delimiter) {
		if (csv == null || delimiter == null) {
			return false;
		}
		boolean isOk = true;
		try {
			String[] elem = csv.split(delimiter);
			int i = 0;
			for (String header : elem) {
				String[] keyValuePair = header.split("=");
				if (keyValuePair.length >= 2) {
					String key = keyValuePair[0].trim();
					String val = keyValuePair[1].trim();
					if (getValue(ClsProperties.IS_TRACE_LOG, 0) > 0) {
						System.out.println("CONF[" + String.format("%03d", i + 1) + "] OPTIONS = " + key + ": " + val);
					}
					propMap.put(key, val);
					i++;
				}
			}
		} catch (RuntimeException ex) {
			isOk = false;
			if (getValue(ClsProperties.IS_TRACE_LOG, 0) > 0) {
				ex.printStackTrace();
			}
		}
		return isOk;
	}

	/**
	 * OS名文字列を取得します。
	 * <p>
	 * プロパティに {@link #OS_NAME} が設定されている場合はその値を、
	 * 未設定時は System.getProperty("os.name") の値を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String os = prop.getOsName();
	 * </pre>
	 *
	 * @return OS名文字列
	 */
	public String getOsName() {
		String retVal = "";
		try {
			retVal = getValue(ClsProperties.OS_NAME, System.getProperty("os.name"));
		} catch (RuntimeException e) {
			retVal = "";
		}
		return retVal;
	}

	/**
	 * OS種別の判定用短縮名（"win", "linux", "hpux", "solaris"）を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String shortName = prop.getOsShortName(); // "win"
	 * </pre>
	 *
	 * @return OS短縮名文字列
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
	 * OS短縮名から OS識別子ID (整数) を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * int id = prop.getOsId("linux"); // ClsProperties.OS_LINUX (2)
	 * </pre>
	 *
	 * @param osName OS短縮名 ("win", "linux", "hpux", "solaris")
	 * @return OS識別子ID ({@link #OS_WIN}, {@link #OS_LINUX}, {@link #OS_HPUX}, {@link #OS_SOLARIS})
	 */
	public int getOsId(String osName) {
		int retVal = OS_WIN;
		if ("win".equals(osName)) {
			retVal = ClsProperties.OS_WIN;
		} else if ("linux".equals(osName)) {
			retVal = ClsProperties.OS_LINUX;
		} else if ("hpux".equals(osName)) {
			retVal = ClsProperties.OS_HPUX;
		} else if ("solaris".equals(osName)) {
			retVal = ClsProperties.OS_SOLARIS;
		}
		return retVal;
	}

	/**
	 * 実行環境のOSおよび設定に応じた netstat コマンド引数文字列を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String args = prop.getCommandArgs(); // "-an" または "-ano"
	 * </pre>
	 *
	 * @return netstat コマンド引数文字列
	 */
	public String getCommandArgs() {
		String retVal = getValue(ClsProperties.ARGUMENT, ClsProperties.DEFAULT_ARGUMENT);
		if (getValue(ClsProperties.IS_PID, false)) {
			switch (getOsId(getValue(OS_NAME, getOsShortName()))) {
				case ClsProperties.OS_WIN:
					retVal = getValue(ClsProperties.ARGUMENT, ClsProperties.DEFAULT_WIN_PID_ARGUMENT);
					break;
				case ClsProperties.OS_LINUX:
					retVal = getValue(ClsProperties.ARGUMENT, ClsProperties.DEFAULT_LINUX_PID_ARGUMENT);
					break;
				default:
					break;
			}
		}
		return retVal;
	}

	/**
	 * 実行環境のOSに応じた TCP 接続行抽出用正規表現を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String regex = prop.getTcpConnRegex();
	 * </pre>
	 *
	 * @return TCP 接続行抽出用正規表現文字列
	 */
	public String getTcpConnRegex() {
		String retVal = "";
		String shortName = getOsShortName();
		if ("win".equals(shortName)) {
			if (getValue(ClsProperties.IS_PID, false)) {
				retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.WIN_TCP_CONN_PID_REGEX);
			} else {
				retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.WIN_TCP_CONN_REGEX);
			}
		} else if ("hpux".equals(shortName)) {
			retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.HPUX_TCP_CONN_REGEX);
		} else if ("solaris".equals(shortName)) {
			retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.SOL_TCP_CONN_REGEX);
		} else {
			if (getValue(ClsProperties.IS_PID, false)) {
				retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.LINUX_TCP_CONN_PID_REGEX);
			} else {
				retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.LINUX_TCP_CONN_REGEX);
			}
		}
		return retVal;
	}

	/**
	 * 実行環境のOSに応じた UDP 接続行抽出用正規表現を取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String regex = prop.getUdpConnRegex();
	 * </pre>
	 *
	 * @return UDP 接続行抽出用正規表現文字列
	 */
	public String getUdpConnRegex() {
		String retVal = "";
		String shortName = getOsShortName();
		if ("win".equals(shortName)) {
			if (getValue(ClsProperties.IS_PID, false)) {
				retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.WIN_UDP_CONN_PID_REGEX);
			} else {
				retVal = getValue(ClsProperties.UDP_CONN_REGEX, ClsProperties.WIN_UDP_CONN_REGEX);
			}
		} else if ("hpux".equals(shortName)) {
			retVal = getValue(ClsProperties.UDP_CONN_REGEX, ClsProperties.HPUX_UDP_CONN_REGEX);
		} else if ("solaris".equals(shortName)) {
			retVal = getValue(ClsProperties.UDP_CONN_REGEX, ClsProperties.SOL_UDP_CONN_REGEX);
		} else {
			if (getValue(ClsProperties.IS_PID, false)) {
				retVal = getValue(ClsProperties.TCP_CONN_REGEX, ClsProperties.LINUX_UDP_CONN_PID_REGEX);
			} else {
				retVal = getValue(ClsProperties.UDP_CONN_REGEX, ClsProperties.LINUX_UDP_CONN_REGEX);
			}
		}
		return retVal;
	}

	/**
	 * 指定コマンドを実行し、その標準出力を文字列として一括取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String out = prop.execToString("hostname");
	 * </pre>
	 *
	 * @param cmd 実行するコマンド文字列 (例: "hostname")
	 * @return コマンドの標準出力文字列（失敗時は空文字）
	 */
	public String execToString(String cmd) {
		String retVal = "";
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A")) {
			retVal = s.hasNext() ? s.next() : "";
		} catch (IOException | RuntimeException e) {
			// コマンド実行失敗時は空文字
			// ignore
		}
		return retVal;
	}

	/**
	 * 指定ホスト名に関連付けられたすべての IP アドレスを DNS/OS から取得し、内部リストに格納します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * prop.fetchIpAddrs("localhost");
	 * List&lt;String&gt; ips = prop.getIpAddrList();
	 * </pre>
	 *
	 * @param host 解決対象のホスト名 (null または空文字時は "localhost")
	 */
	public void fetchIpAddrs(String host) {
		ipAddrList.clear();
		String targetHost = (host != null && !host.trim().isEmpty()) ? host : "localhost";
		try {
			InetAddress[] ia = InetAddress.getAllByName(targetHost);
			if (ia != null) {
				for (InetAddress addr : ia) {
					if (addr != null) {
						String ip = addr.getHostAddress().trim();
						if (!ip.isEmpty() && !ipAddrList.contains(ip)) {
							ipAddrList.add(ip);
						}
					}
				}
			}
		} catch (UnknownHostException | SecurityException e) {
			// ignore
		}
	}

	/**
	 * 実行時刻とホスト名に基づいたデフォルトの出力ファイル名を生成します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String filename = prop.getDefaultName(); // 例: "NwConnHostInfo_server.20260817.120000.txt"
	 * </pre>
	 *
	 * @return デフォルト出力ファイル名文字列
	 */
	public String getDefaultName() {
		return "NwConnHostInfo_" + this.hostName + "." + convUnixToJst(getValue(ClsProperties.START_TIME_MSEC, System.currentTimeMillis()), "yyyyMMdd.HHmmss") + ".txt";
	}

	/**
	 * OS種別IDに応じたデフォルト文字エンコーディング名を取得します。
	 * <p>
	 * Windows の場合は "MS932"、HP-UX/Solaris の場合は "EUC-JP"、それ以外は "UTF-8" を返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String enc = prop.getDefEncoding(ClsProperties.OS_WIN); // "MS932"
	 * </pre>
	 *
	 * @param osId OS種別ID ({@link #OS_WIN}, {@link #OS_LINUX}, {@link #OS_HPUX}, {@link #OS_SOLARIS})
	 * @return 文字エンコーディング名文字列
	 */
	public String getDefEncoding(int osId) {
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
	 * 指定されたテキストファイルの内容を文字列として全行読み込みます。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * String content = prop.readFile("data.txt");
	 * </pre>
	 *
	 * @param filePath 読み込み対象ファイルパス
	 * @return ファイルの全内容文字列
	 */
	public String readFile(String filePath) {
		StringBuilder sb = new StringBuilder();
		String enc = getValue(CNF_FILE_ENCODING, getDefEncoding(getOsId(getValue(OS_NAME, getOsShortName()))));
		try (FileInputStream fis = new FileInputStream(filePath);
			 InputStreamReader isr = new InputStreamReader(fis, enc);
			 BufferedReader br = new BufferedReader(isr)) {
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException ioex) {
			System.err.println("EXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (RuntimeException ex) {
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * 指定されたテキストファイルを読み込み、コメント行（#始まり）を除外した行リストを返却します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * List&lt;String&gt; lines = prop.readFileToList("ip_list.txt");
	 * </pre>
	 *
	 * @param filePath 読み込み対象ファイルパス
	 * @return 有効な行文字列のリスト
	 */
	public List<String> readFileToList(String filePath) {
		List<String> bodyList = new ArrayList<>();
		String commentLineRegex = "^\\s*#.*";
		String enc = getValue(ClsProperties.CNF_FILE_ENCODING, getDefEncoding(getOsId(getValue(OS_NAME, getOsShortName()))));
		try (FileInputStream fis = new FileInputStream(filePath);
			 InputStreamReader isr = new InputStreamReader(fis, enc);
			 BufferedReader br = new BufferedReader(isr)) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty() && !line.matches(commentLineRegex)) {
					bodyList.add(line);
				}
			}
		} catch (IOException ioex) {
			System.err.println("EXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (RuntimeException ex) {
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return bodyList;
	}

	/**
	 * 指定されたファイルパスへメッセージ文字列を書き込みます。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * boolean success = prop.writeFile("output.log", "Sample message\n", true);
	 * </pre>
	 *
	 * @param filePath 書き込み先ファイルパス
	 * @param message 書き込むメッセージ文字列
	 * @param isAppend 追記する場合は true、新規作成・上書きは false
	 * @return 書き込み成功時は true、失敗時は false
	 */
	public boolean writeFile(String filePath, String message, boolean isAppend) {
		boolean isOk = true;
		String enc = getValue(ClsProperties.OUTPUT_FILE_ENCODING, getDefEncoding(getOsId(getValue(OS_NAME, getOsShortName()))));
		try (FileOutputStream fos = new FileOutputStream(filePath, isAppend);
			 OutputStreamWriter osw = new OutputStreamWriter(fos, enc);
			 BufferedWriter bw = new BufferedWriter(osw)) {
			if (message != null) {
				bw.write(message);
			}
		} catch (IOException ioex) {
			isOk = false;
			System.err.println("EXCEPTION : " + filePath + " : " + ioex.getMessage());
			ioex.printStackTrace();
		} catch (RuntimeException ex) {
			isOk = false;
			System.err.println("EXCEPTION : " + filePath + " : " + ex.getMessage());
			ex.printStackTrace();
		}
		return isOk;
	}

	/**
	 * 文字列が整数数値としてパース可能かどうかを判定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * boolean isNum = prop.isNumber("12345"); // true
	 * boolean isNotNum = prop.isNumber("abc"); // false
	 * </pre>
	 *
	 * @param str 判定対象の文字列
	 * @return 整数としてパース可能な場合は true、それ以外（null、空文字、非数値）は false
	 */
	public boolean isNumber(String str) {
		if (str == null || str.trim().isEmpty()) {
			return false;
		}
		try {
			Integer.parseInt(str.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
