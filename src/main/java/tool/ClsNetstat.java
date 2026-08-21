package tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils;

/**
 * netstat コマンドの実行結果またはキャプチャファイルを解析し、TCP/UDPのLISTENポートおよび対外通信コネクションを集計・出力するクラスです。
 * <p>
 * Windows, Linux, HP-UX, Solaris の各 OS に対応した正規表現パターンを用いて netstat 出力を解析し、
 * 受信 (INBOUND) / 送信 (OUTBOUND) 接続の分類、PID/プロセス名の紐付け、IPアドレスによるフィルタリング、
 * 結果ファイルおよびリモートホスト別通信ログの出力を行います。
 * </p>
 *
 * <p>使用例:</p>
 * <pre>
 * ClsProperties prop = new ClsProperties();
 * ClsNetstat netstat = new ClsNetstat(prop);
 * netstat.init();
 * netstat.execNetstat();
 * netstat.getListenPorts();
 * netstat.getConnList();
 * netstat.showList();
 * netstat.clear();
 * </pre>
 */
public class ClsNetstat {

	/** プロパティ設定管理オブジェクト */
	private ClsProperties prop = null;

	/** コマンド標準出力格納オブジェクト */
	private ClsCmdStdOut cmdStdOut = null;

	/** 外部コマンド実行オブジェクト */
	private ClsCmdExec cmdExec = null;

	/** netstat 出力行リスト */
	private List<String> cmdStdOutList = new LinkedList<>();

	/** TCP LISTENポート文字列一覧リスト (IP_PORT形式) */
	private List<String> tcpListenPortStrList = new ArrayList<>();

	/** UDP LISTENポート文字列一覧リスト (IP_PORT形式) */
	private List<String> udpListenPortStrList = new ArrayList<>();

	/** TCP LISTENポート番号（数値）一覧リスト */
	private List<Integer> tcpListenPortList = new ArrayList<>();

	/** UDP LISTENポート番号（数値）一覧リスト */
	private List<Integer> udpListenPortList = new ArrayList<>();

	/** リモートホストログ対象IPv4アドレス一覧 */
	private List<String> incIpV4Addrs = new ArrayList<>();

	/** リモートホストログ除外IPv4アドレス一覧 */
	private List<String> excIpV4Addrs = new ArrayList<>();

	/** リモートホストログ対象IPv6アドレス一覧 */
	private List<String> incIpV6Addrs = new ArrayList<>();

	/** リモートホストログ除外IPv6アドレス一覧 */
	private List<String> excIpV6Addrs = new ArrayList<>();

	/** LISTEN状態とみなすステータス文字列リスト ("LISTEN", "LISTENING", "BOUND") */
	private List<String> listenStateList = new ArrayList<>();

	/** TCP 送信（OUTBOUND）マップ (Local:Port => Remote:Port リスト) */
	private Map<String, List<String>> tcpSendMap = new HashMap<>();

	/** TCP 受信（INBOUND）マップ (Local:Port => Remote:Port リスト) */
	private Map<String, List<String>> tcpRecvMap = new HashMap<>();

	/** UDP 送信（OUTBOUND）マップ (Local:Port => Remote:Port リスト) */
	private Map<String, List<String>> udpSendMap = new HashMap<>();

	/** UDP 受信（INBOUND）マップ (Local:Port => Remote:Port リスト) */
	private Map<String, List<String>> udpRecvMap = new HashMap<>();

	/** TCP ポート/接続別プロセス情報マップ (Local:Port または Remote:Port => ClsAppProp) */
	private Map<String, ClsAppProp> tcpAppMap = new HashMap<>();

	/** UDP ポート/接続別プロセス情報マップ (Local:Port または Remote:Port => ClsAppProp) */
	private Map<String, ClsAppProp> udpAppMap = new HashMap<>();

	/** 冗長ログ出力レベル */
	private int verbose = 0;

	/** 現在のループ実行カウント */
	private int loopCount = 0;

	/** 判定対象のOS種別ID */
	private int osId = ClsProperties.OS_OTHER;

	/** netstat 実行時刻（エポックミリ秒） */
	private long execNetstatTime = 0;

	/** Solaris用TCPブロック正規表現 */
	private String tcpBlockRegex = null;

	/** Solaris用UDPブロック正規表現 */
	private String udpBlockRegex = null;

	/** Solaris用SCTPブロック正規表現 */
	private String sctpBlockRegex = null;

	/** TCP接続抽出正規表現 */
	private String tcpConnRegex = null;

	/** UDP接続抽出正規表現 */
	private String udpConnRegex = null;

	/** TCP LISTEN抽出正規表現 */
	private String tcpLstnRegex = null;

	/** UDP LISTEN抽出正規表現 */
	private String udpLstnRegex = null;

	/** netstat 実行コマンドパス */
	private String cmdPath = null;

	/** netstat 実行コマンド引数 */
	private String cmdArgs = null;

	/** エンコード済み IPv4 ループバックアドレス ("127.0.0.1") */
	private String ipV4LoopbackAddr = null;

	/** エンコード済み Windows用 IPv6 ループバックアドレス ("[::1]") */
	private String ipV6LoopbackAddrWin = null;

	/** エンコード済み Linux用 IPv6 ループバックアドレス ("::1") */
	private String ipV6LoopbackAddrLinux = null;

	/** エンコード済み IPv4 UDP ワイルドカードLISTEN状態 ("*:*") */
	private String ipV4UdpLstnStateWildcard = null;

	/** エンコード済み HP-UX用 IPv4 UDP ワイルドカードLISTEN状態 ("*.*") */
	private String ipV4UdpLstnStateHpux = null;

	/** エンコード済み IPv4 UDP 0.0.0.0:* LISTEN状態 */
	private String ipV4UdpLstnStateWildcard4Bit = null;

	/** エンコード済み IPv6 UDP :::* LISTEN状態 */
	private String ipV6UdpLstnState = null;

	/** PID取得モード有効フラグ */
	private boolean isPid = false;

	/** リモートホストログ出力有効フラグ */
	private boolean isRHostLog = false;

	/** リモートホストログ対象IPフィルタ有効フラグ */
	private boolean isRHostLogIncIp = false;

	/** リモートホストログ除外IPフィルタ有効フラグ */
	private boolean isRHostLogExcIp = false;

	/** 高位ポートを ANY (:ANY) に正規化するフラグ */
	private boolean isAddPortAny = true;

	/**
	 * プロパティ設定オブジェクトを指定して ClsNetstat を初期化します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsProperties prop = new ClsProperties();
	 * ClsNetstat netstat = new ClsNetstat(prop);
	 * </pre>
	 *
	 * @param prop プロパティ管理オブジェクト
	 */
	public ClsNetstat(ClsProperties prop) {
		this.prop = prop;
		this.cmdStdOut = new ClsCmdStdOut();
		this.cmdExec = new ClsCmdExec();
		this.cmdExec.setCmdStdOut(this.cmdStdOut);
		this.clear();
	}

	/**
	 * コマンド標準出力行リストを取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * List&lt;String&gt; outList = netstat.getStdOutList();
	 * </pre>
	 *
	 * @return 標準出力文字列リスト
	 */
	public List<String> getStdOutList() {
		return this.cmdStdOutList;
	}

	/**
	 * コマンド標準出力行リストを設定します。
	 * <p>
	 * 引数が null の場合は空のリストが設定されます。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * List&lt;String&gt; lines = Arrays.asList("line1", "line2");
	 * netstat.setStdOutList(lines);
	 * </pre>
	 *
	 * @param list 設定する標準出力文字列リスト
	 */
	public void setStdOutList(List<String> list) {
		this.cmdStdOutList = (list != null ? list : new LinkedList<String>());
	}

	/**
	 * 現在のループ実行カウントを取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * int count = netstat.getLoopCount();
	 * </pre>
	 *
	 * @return ループカウント
	 */
	public int getLoopCount() {
		return this.loopCount;
	}

	/**
	 * 現在のループ実行カウントを設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * netstat.setLoopCount(1);
	 * </pre>
	 *
	 * @param count ループカウント
	 */
	public void setLoopCount(int count) {
		this.loopCount = count;
	}

	/**
	 * netstat 実行に必要な正規表現パターン、コマンド引数、各種判定フラグを初期化します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * netstat.init();
	 * </pre>
	 */
	public void init() {
		listenStateList.add("LISTEN");
		listenStateList.add("LISTENING");
		ipV4LoopbackAddr = encodeStr("127.0.0.1");
		ipV6LoopbackAddrWin = encodeStr("[::1]");
		ipV6LoopbackAddrLinux = encodeStr("::1");
		ipV4UdpLstnStateWildcard = encodeStr("*:*");
		ipV4UdpLstnStateHpux = encodeStr("*.*");
		ipV4UdpLstnStateWildcard4Bit = encodeStr("0.0.0.0:*");
		ipV6UdpLstnState = encodeStr(":::*");

		verbose = prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE);
		isPid = prop.getValue(ClsProperties.IS_PID, false);
		isAddPortAny = prop.getValue(ClsProperties.IS_PORT_ANY, ClsProperties.DEFAULT_IS_PORT_ANY);

		osId = prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName()));
		if (osId == ClsProperties.OS_SOLARIS) {
			if (!listenStateList.contains("BOUND")) {
				listenStateList.add("BOUND");
			}
		}
		cmdPath = prop.getValue(ClsProperties.COMMAND, ClsProperties.DEFAULT_COMMAND);
		cmdArgs = prop.getCommandArgs();

		tcpBlockRegex = prop.getValue(ClsProperties.TCP_BLOCK_REGEX, ClsProperties.DEFAULT_TCP_BLOCK_REGEX);
		udpBlockRegex = prop.getValue(ClsProperties.UDP_BLOCK_REGEX, ClsProperties.DEFAULT_UDP_BLOCK_REGEX);
		sctpBlockRegex = prop.getValue(ClsProperties.SCTP_BLOCK_REGEX, ClsProperties.DEFAULT_SCTP_BLOCK_REGEX);
		tcpConnRegex = prop.getTcpConnRegex();
		udpConnRegex = prop.getUdpConnRegex();
		tcpLstnRegex = (osId == ClsProperties.OS_SOLARIS ? prop.getValue(ClsProperties.TCP_LSTN_REGEX, ClsProperties.SOL_TCP_LSTN_REGEX) : tcpConnRegex);
		udpLstnRegex = (osId == ClsProperties.OS_SOLARIS ? prop.getValue(ClsProperties.UDP_LSTN_REGEX, ClsProperties.SOL_UDP_LSTN_REGEX) : udpConnRegex);

		cmdExec.setWatchdog(prop.getValue(ClsProperties.TIMEOUT, ClsProperties.DEFAULT_TIMEOUT));

		String tempPath = prop.getValue(ClsProperties.PATHDRHOSTLOG, "");
		if (!tempPath.isEmpty() && prop.isExist(tempPath)) {
			isRHostLog = true;
		}
		tempPath = prop.getValue(ClsProperties.PATHFRHOSTLOG_I_IP, "");
		if (!tempPath.isEmpty() && prop.isExist(tempPath)) {
			isRHostLogIncIp = true;
		}
		tempPath = prop.getValue(ClsProperties.PATHFRHOSTLOG_X_IP, "");
		if (!tempPath.isEmpty() && prop.isExist(tempPath)) {
			isRHostLogExcIp = true;
		}
	}

	/**
	 * 収集・集計したポート一覧および送受信コネクションマップ情報を全クリアします。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * netstat.clear();
	 * </pre>
	 */
	public void clear() {
		cmdStdOutList.clear();
		tcpListenPortStrList.clear();
		udpListenPortStrList.clear();
		tcpListenPortList.clear();
		udpListenPortList.clear();
		tcpSendMap.clear();
		tcpRecvMap.clear();
		udpSendMap.clear();
		udpRecvMap.clear();
		tcpAppMap.clear();
		udpAppMap.clear();
	}

	/**
	 * プロセスID文字列からアプリケーション情報オブジェクト ({@link ClsAppProp}) を構築して返却します。
	 * <p>
	 * Windows の場合は PID 数値のみ、Linux の場合は "PID/プロセス名" 形式を分解して設定します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsAppProp app = netstat.getAppProp("1234/httpd");
	 * </pre>
	 *
	 * @param pidStr PID 文字列 (例: "1234", "1234/httpd")
	 * @return アプリケーションプロパティオブジェクト
	 */
	public ClsAppProp getAppProp(String pidStr) {
		ClsAppProp appProp = new ClsAppProp();
		if (pidStr == null || pidStr.trim().isEmpty()) {
			return appProp;
		}
		switch (osId) {
			case ClsProperties.OS_WIN:
				appProp.setPid(pidStr.trim());
				break;
			case ClsProperties.OS_LINUX:
				String[] elem = pidStr.split("/");
				appProp.setPid(elem[0].trim());
				if (elem.length > 1) {
					appProp.setAppName(elem[1].trim().replaceAll("\\s+", "_"));
				}
				break;
			default:
				break;
		}
		if (verbose > 8) {
			System.out.println("# -> ClsNetstat.getAppProp(" + pidStr + ") : PID=" + appProp.getPid() + " / NAME=" + appProp.getAppName() + " / PATH=" + appProp.getAppPath());
		}
		return appProp;
	}

	/**
	 * Map のキーとして安全に使用するために特殊文字（コロン、アスタリスク）をアンダースコア等にエンコードします。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * String encoded = netstat.encodeStr("192.168.1.1:80"); // "192.168.1.1_80"
	 * String wildcard = netstat.encodeStr("*:*"); // "_ALL___ALL_"
	 * </pre>
	 *
	 * @param key エンコード対象文字列
	 * @return エンコード後文字列
	 */
	public String encodeStr(String key) {
		String retStr = "";
		if (key != null && !key.isEmpty()) {
			retStr = key.replace(":", "_").replace("*", "_ALL_");
		}
		return retStr;
	}

	/**
	 * エンコードされたキー文字列を元の文字列表現（コロン、アスタリスク）に復元（デコード）します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * String decoded = netstat.decodeStr("192.168.1.1_80"); // "192.168.1.1:80"
	 * </pre>
	 *
	 * @param key デコード対象文字列
	 * @return 復元された文字列
	 */
	public String decodeStr(String key) {
		String retStr = "";
		if (key != null && !key.isEmpty()) {
			retStr = key.replace("_ALL_", "*").replace("_", ":");
		}
		return retStr;
	}

	/**
	 * 指定アドレス（エンコード済み）がループバックアドレス（127.0.0.1, [::1], ::1等）であるか判定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * boolean isLoop = netstat.isLoopbackAddr("127.0.0.1"); // true
	 * </pre>
	 *
	 * @param checkAddr 判定対象のアドレス文字列（エンコード済み）
	 * @return ループバックアドレスの場合は true、それ以外は false
	 */
	public boolean isLoopbackAddr(String checkAddr) {
		if (checkAddr == null) {
			return false;
		}
		return checkAddr.equals(ipV4LoopbackAddr)
				|| checkAddr.equals(ipV6LoopbackAddrWin)
				|| checkAddr.equals(ipV6LoopbackAddrLinux);
	}

	/**
	 * TCP ソケットの状態が LISTEN/LISTENING/BOUND 状態であるか判定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * boolean isLstn = netstat.isTcpListen("LISTENING"); // true
	 * </pre>
	 *
	 * @param state 状態文字列 (例: "LISTENING", "ESTABLISHED")
	 * @return LISTEN 状態の場合は true、それ以外は false
	 */
	public boolean isTcpListen(String state) {
		if (state == null) {
			return false;
		}
		return listenStateList.contains(state.toUpperCase());
	}

	/**
	 * UDP ソケットの状態が LISTEN（待機）状態であるか判定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * boolean isLstn = netstat.isUdpListen("_ALL___ALL_"); // true
	 * </pre>
	 *
	 * @param state 状態またはリモート接続文字列 (例: "*:*", "0.0.0.0:*")
	 * @return LISTEN 状態の場合は true、それ以外は false
	 */
	public boolean isUdpListen(String state) {
		if (state == null) {
			return false;
		}
		if (osId == ClsProperties.OS_SOLARIS) {
			return listenStateList.contains(state.toUpperCase());
		}
		return state.equals(ipV4UdpLstnStateWildcard)
				|| state.equals(ipV4UdpLstnStateWildcard4Bit)
				|| state.equals(ipV6UdpLstnState)
				|| state.equals(ipV4UdpLstnStateHpux);
	}

	/**
	 * TCP ソケットの状態がアクティブなコネクション（ESTABLISHED, CLOSE_WAIT 等）であるか判定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * boolean isConn = netstat.isTcpConnState("ESTABLISHED"); // true
	 * </pre>
	 *
	 * @param state 状態文字列
	 * @return アクティブなコネクション状態の場合は true、LISTEN や SYN_ 状態等は false
	 */
	public boolean isTcpConnState(String state) {
		if (state == null) {
			return false;
		}
		String checkStr = state.toUpperCase();
		if (osId == ClsProperties.OS_SOLARIS) {
			return !isTcpListen(state) && !checkStr.contains("IDLE") && !checkStr.contains("SYN_");
		}
		return !isTcpListen(state) && !checkStr.contains("SYN_");
	}

	/**
	 * UDP ソケットの状態がアクティブな接続状態であるか判定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * boolean isConn = netstat.isUdpConnState("192.168.1.5_53"); // true
	 * </pre>
	 *
	 * @param state リモートアドレス文字列
	 * @return 接続状態の場合は true、LISTEN 待機状態の場合は false
	 */
	public boolean isUdpConnState(String state) {
		return !isUdpListen(state);
	}

	/**
	 * 指定文字列が IPv4 形式のアドレス（または CIDR 表記）であるかを判定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * boolean isV4 = netstat.isIpv4Address("192.168.1.1"); // true
	 * boolean isNotV4 = netstat.isIpv4Address("fe80::1"); // false
	 * </pre>
	 *
	 * @param ipAddr 判定対象の IP アドレス文字列
	 * @return 有効な IPv4 形式の場合は true、それ以外は false
	 */
	public boolean isIpv4Address(String ipAddr) {
		if (ipAddr == null || ipAddr.trim().isEmpty()) {
			return false;
		}
		String[] elem = ipAddr.split("/");
		String[] addrs = elem[0].split("\\.");
		if (addrs.length != 4) {
			return false;
		}
		for (String addr : addrs) {
			try {
				int b = Integer.parseInt(addr);
				if (b < 0 || 255 < b) {
					return false;
				}
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * リモートホストログ出力用に対象/除外 IP 定義ファイルを読み込み、内部リストを更新します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * netstat.getValidIpAddrs();
	 * </pre>
	 */
	public void getValidIpAddrs() {
		if (isRHostLogIncIp) {
			incIpV4Addrs.clear();
			incIpV6Addrs.clear();
			String strPath = prop.getValue(ClsProperties.PATHFRHOSTLOG_I_IP, "");
			if (prop.isExist(strPath)) {
				for (String elem : prop.readFileToList(strPath)) {
					if (isIpv4Address(elem)) {
						incIpV4Addrs.add(elem);
					} else {
						incIpV6Addrs.add(elem);
					}
				}
			}
		}
		if (isRHostLogExcIp) {
			excIpV4Addrs.clear();
			excIpV6Addrs.clear();
			String strPath = prop.getValue(ClsProperties.PATHFRHOSTLOG_X_IP, "");
			if (prop.isExist(strPath)) {
				for (String elem : prop.readFileToList(strPath)) {
					if (isIpv4Address(elem)) {
						excIpV4Addrs.add(elem);
					} else {
						excIpV6Addrs.add(elem);
					}
				}
			}
		}
	}

	/**
	 * 対象 IP アドレスがリモートホストログ出力条件に合致するか判定します。
	 * <p>
	 * 対象リスト (INCLUDE) に含まれ、かつ除外リスト (EXCLUDE) に含まれない場合に true を返します。
	 * CIDR 表記 (サブネット範囲) にも対応します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * boolean valid = netstat.isValidIpAddr("192.168.1.100");
	 * </pre>
	 *
	 * @param ipAddr チェック対象の IP アドレス文字列
	 * @return 出力対象の場合は true、それ以外は false
	 */
	public boolean isValidIpAddr(String ipAddr) {
		boolean isHit = true;
		if (isRHostLogIncIp) {
			isHit = false;
			if (isIpv4Address(ipAddr)) {
				for (String elem : incIpV4Addrs) {
					String cidr = elem.contains("/") ? elem : elem + "/32";
					SubnetUtils subnet = new SubnetUtils(cidr);
					subnet.setInclusiveHostCount(true);
					if (subnet.getInfo().isInRange(ipAddr)) {
						isHit = true;
						break;
					}
				}
			} else {
				if (incIpV6Addrs.contains(ipAddr)) {
					isHit = true;
				}
			}
		}
		if (isRHostLogExcIp) {
			if (isIpv4Address(ipAddr)) {
				for (String elem : excIpV4Addrs) {
					String cidr = elem.contains("/") ? elem : elem + "/32";
					SubnetUtils subnet = new SubnetUtils(cidr);
					subnet.setInclusiveHostCount(true);
					if (subnet.getInfo().isInRange(ipAddr)) {
						isHit = false;
						break;
					}
				}
			} else {
				if (excIpV6Addrs.contains(ipAddr)) {
					isHit = false;
				}
			}
		}
		return isHit;
	}

	/**
	 * netstat コマンドを実行し、標準出力結果を行単位で内部バッファに取り込みます。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * int retCode = netstat.execNetstat();
	 * </pre>
	 *
	 * @return コマンド実行の終了コード
	 */
	public int execNetstat() {
		cmdStdOut.clear();
		execNetstatTime = System.currentTimeMillis();
		int returnCode = cmdExec.execute(cmdPath, cmdArgs.split("\\s+"));
		cmdStdOutList = cmdStdOut.getStdOutList();
		if (verbose > 2) {
			System.out.println("# -> === " + prop.convUnixToJst(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss") + " / CYCLE : " + loopCount + " ===");
			if (verbose > 4) {
				System.out.println("# -> ClsNetstat.execNetstat() : cmdStdOutList.size() = " + cmdStdOutList.size());
			}
		}
		return returnCode;
	}

	/**
	 * netstat コマンドの出力が保存されたテキストファイルを読み込み、内部バッファにセットします。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * File file = new File("netstat_output.txt");
	 * int retCode = netstat.readFile(file, Charset.forName("MS932"));
	 * </pre>
	 *
	 * @param file 読み込み対象ファイル
	 * @param charset 文字エンコーディング
	 * @return 終了コード（正常時は 0）
	 */
	public int readFile(File file, Charset charset) {
		int returnCode = 0;
		cmdStdOut.clear();
		if (verbose > 0) {
			System.out.println("# -> === READ : " + file.getAbsolutePath() + " ===");
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					cmdStdOutList.add(line);
				}
			}
		} catch (IOException e) {
			System.err.println("IOException in readFile: " + e.getMessage());
			e.printStackTrace();
		}
		return returnCode;
	}

	/**
	 * 内部バッファの netstat 出力から TCP/UDP の LISTEN 待機ポート一覧を抽出・ソートします。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * netstat.getListenPorts();
	 * </pre>
	 */
	public void getListenPorts() {
		Pattern pttnForTcpBlock = Pattern.compile(tcpBlockRegex);
		Pattern pttnForUdpBlock = Pattern.compile(udpBlockRegex);
		Pattern pttnForSctpBlock = Pattern.compile(sctpBlockRegex);
		Pattern pttnForTcp = Pattern.compile(tcpLstnRegex);
		Pattern pttnForUdp = Pattern.compile(udpLstnRegex);
		int blockId = ClsProperties.BLOCK_VALID;
		if (osId == ClsProperties.OS_SOLARIS) {
			blockId = ClsProperties.BLOCK_NONE;
		}

		for (String line : cmdStdOutList) {
			if (line != null && !line.trim().isEmpty()) {
				boolean isOk = true;
				if (osId == ClsProperties.OS_SOLARIS) {
					Matcher tcpBlockMatcher = pttnForTcpBlock.matcher(line);
					if (tcpBlockMatcher.find()) {
						blockId = ClsProperties.BLOCK_TCP;
					}
					Matcher udpBlockMatcher = pttnForUdpBlock.matcher(line);
					if (udpBlockMatcher.find()) {
						blockId = ClsProperties.BLOCK_UDP;
					}
					Matcher sctpBlockMatcher = pttnForSctpBlock.matcher(line);
					if (sctpBlockMatcher.find()) {
						blockId = ClsProperties.BLOCK_NONE;
					}
				}

				if (blockId == ClsProperties.BLOCK_VALID || blockId == ClsProperties.BLOCK_TCP) {
					Matcher tcpMatcher = pttnForTcp.matcher(line);
					if (tcpMatcher.find()) {
						if (verbose > 8) {
							System.out.println("# -> TCP : tcpMatcher.find() : " + encodeStr(line));
						}
						String localAddr = encodeStr(tcpMatcher.group(1));
						String localPort = encodeStr(tcpMatcher.group(2));
						if (localAddr.equals("_ALL_")) {
							localAddr = "0.0.0.0";
						}
						if (localPort.equals("_ALL_")) {
							isOk = false;
						}
						String state = tcpMatcher.group(5);

						int localPortNum = 0;
						try {
							localPortNum = Integer.parseInt(localPort);
						} catch (NumberFormatException ignored) {
							// ignore
						}

						if (isOk && isTcpListen(state)) {
							if (!this.isLoopbackAddr(localAddr)) {
								int portNo = localPortNum;
								String elem = localAddr + "_" + localPort;
								if (portNo > 0 && !tcpListenPortList.contains(portNo)) {
									tcpListenPortList.add(portNo);
								}
								if (!tcpListenPortStrList.contains(elem)) {
									tcpListenPortStrList.add(elem);
									if (isPid && !tcpAppMap.containsKey(elem)) {
										tcpAppMap.put(elem, this.getAppProp(tcpMatcher.group(6)));
										if (verbose > 8) {
											System.out.println("# -> tcpAppMap.put(" + elem + ", PID=" + tcpAppMap.get(elem).getPid() + ")");
										}
									}
								}
								if (verbose > 6) {
									if (isPid && tcpAppMap.containsKey(elem)) {
										System.out.println("# -> TCP L " + localAddr + ":" + localPort + " " + tcpAppMap.get(elem).getPid());
									} else {
										System.out.println("# -> TCP L " + localAddr + ":" + localPort);
									}
								}
							}
						}
					}
				}

				if (blockId == ClsProperties.BLOCK_VALID || blockId == ClsProperties.BLOCK_UDP) {
					Matcher udpMatcher = pttnForUdp.matcher(line);
					if (udpMatcher.find()) {
						if (verbose > 8) {
							System.out.println("# -> UDP : pttnForUdp.find() : " + encodeStr(line));
						}
						String localAddr = encodeStr(udpMatcher.group(1));
						String localPort = encodeStr(udpMatcher.group(2));
						if (localAddr.equals("_ALL_")) {
							localAddr = "0.0.0.0";
						}
						if (localPort.equals("_ALL_")) {
							isOk = false;
						}
						String remoteAddr = "_ALL_";
						String remotePort = "_ALL_";
						if (osId == ClsProperties.OS_SOLARIS) {
							String checkStr = udpMatcher.group(3).toUpperCase();
							if (!"IDLE".equals(checkStr) && !"LISTEN".equals(checkStr)) {
								isOk = false;
							}
						} else {
							remoteAddr = encodeStr(udpMatcher.group(3));
							remotePort = encodeStr(udpMatcher.group(4));
						}
						String remote = remoteAddr + "_" + remotePort;
						int localPortNum = 0;
						try {
							localPortNum = Integer.parseInt(localPort);
						} catch (NumberFormatException ignored) {
							// ignore
						}

						if (osId != ClsProperties.OS_SOLARIS) {
							if (!isUdpListen(remote)) {
								isOk = false;
							}
						}
						if (isOk) {
							if (!this.isLoopbackAddr(localAddr)) {
								int portNo = localPortNum;
								String elem = localAddr + "_" + localPort;
								if (portNo > 0 && !udpListenPortList.contains(portNo)) {
									udpListenPortList.add(portNo);
								}
								if (!udpListenPortStrList.contains(elem)) {
									udpListenPortStrList.add(elem);
									if (isPid && !udpAppMap.containsKey(elem)) {
										udpAppMap.put(elem, this.getAppProp(udpMatcher.group(5)));
										if (verbose > 8) {
											System.out.println("# -> udpAppMap.put(" + elem + ", PID=" + udpAppMap.get(elem).getPid() + ")");
										}
									}
								}
								if (verbose > 6) {
									if (isPid && udpAppMap.containsKey(elem)) {
										System.out.println("# -> UDP L " + localAddr + ":" + localPort + " " + udpAppMap.get(elem).getPid());
									} else {
										System.out.println("# -> UDP L " + localAddr + ":" + localPort);
									}
								}
							}
						}
					}
				}
			}
		}

		Collections.sort(tcpListenPortList);
		Collections.sort(tcpListenPortStrList);
		Collections.sort(udpListenPortList);
		Collections.sort(udpListenPortStrList);

		if (verbose > 5) {
			System.out.println("# -> ClsNetstat.getListenPorts() : tcpListenPortList.size() = " + tcpListenPortList.size());
			System.out.println("# -> ClsNetstat.getListenPorts() : tcpListenPortStrList.size() = " + tcpListenPortStrList.size());
			System.out.println("# -> ClsNetstat.getListenPorts() : udpListenPortList.size() = " + udpListenPortList.size());
			System.out.println("# -> ClsNetstat.getListenPorts() : udpListenPortStrList.size() = " + udpListenPortStrList.size());
			System.out.println("# -> ClsNetstat.getListenPorts() : tcpAppMap.size() = " + tcpAppMap.size());
			System.out.println("# -> ClsNetstat.getListenPorts() : udpAppMap.size() = " + udpAppMap.size());
			System.out.println("# -> ClsNetstat.getListenPorts() : cmdStdOutList.size() = " + cmdStdOutList.size());
		}
	}

	/**
	 * 内部バッファの netstat 出力から外部との送受信コネクション一覧を抽出し、
	 * INBOUND (受信) / OUTBOUND (送信) に分類して内部マップに登録します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * netstat.getConnList();
	 * </pre>
	 */
	public void getConnList() {
		Pattern pttnForTcpBlock = Pattern.compile(tcpBlockRegex);
		Pattern pttnForUdpBlock = Pattern.compile(udpBlockRegex);
		Pattern pttnForTcp = Pattern.compile(tcpConnRegex);
		Pattern pttnForUdp = Pattern.compile(udpConnRegex);
		int blockId = ClsProperties.BLOCK_VALID;
		if (osId == ClsProperties.OS_SOLARIS) {
			blockId = ClsProperties.BLOCK_NONE;
		}

		if (isRHostLog) {
			getValidIpAddrs();
		}

		for (String line : cmdStdOutList) {
			if (line != null && !line.trim().isEmpty()) {
				Matcher tcpBlockMatcher = pttnForTcpBlock.matcher(line);
				if (tcpBlockMatcher.find()) {
					blockId = ClsProperties.BLOCK_TCP;
				}
				Matcher udpBlockMatcher = pttnForUdpBlock.matcher(line);
				if (udpBlockMatcher.find()) {
					blockId = ClsProperties.BLOCK_UDP;
				}

				if (blockId == ClsProperties.BLOCK_VALID || blockId == ClsProperties.BLOCK_TCP) {
					Matcher tcpMatcher = pttnForTcp.matcher(line);
					if (tcpMatcher.find()) {
						String localAddr = encodeStr(tcpMatcher.group(1));
						String localPort = encodeStr(tcpMatcher.group(2));
						String remoteAddr = encodeStr(tcpMatcher.group(3));
						String remotePort = encodeStr(tcpMatcher.group(4));
						if (localAddr.equals("_ALL_")) {
							localAddr = "0.0.0.0";
						}
						String state = tcpMatcher.group(5);
						String local = localAddr + "_" + localPort;
						String remote = remoteAddr + "_" + remotePort;
						int localPortNum = 0;
						int remotePortNum = 0;
						try {
							localPortNum = Integer.parseInt(localPort);
						} catch (NumberFormatException ignored) {
							// ignore
						}
						try {
							remotePortNum = Integer.parseInt(remotePort);
						} catch (NumberFormatException ignored) {
							// ignore
						}

						if (isTcpConnState(state)) {
							if (!this.isLoopbackAddr(localAddr)) {
								if (!localAddr.equals(remoteAddr)) {
									// 受信：リモート ⇒ ローカル：ポート
									if (tcpListenPortList.contains(localPortNum)) {
										String remoteVal = remoteAddr;
										if (isAddPortAny) {
											remoteVal += ":ANY";
											if (remotePortNum > 0 && remotePortNum < 1024) {
												remoteVal = remoteAddr + ":" + remotePort;
											}
										}
										List<String> recvList = tcpRecvMap.get(local);
										if (recvList == null) {
											recvList = new ArrayList<>();
											tcpRecvMap.put(local, recvList);
										}
										if (!recvList.contains(remoteVal)) {
											recvList.add(remoteVal);
											if (isPid && !tcpAppMap.containsKey(local)) {
												tcpAppMap.put(local, this.getAppProp(tcpMatcher.group(6)));
												if (verbose > 8) {
													System.out.println("# -> tcpAppMap.put(" + local + ", PID=" + tcpAppMap.get(local).getPid() + ")");
												}
											}
										} else {
											if (isPid && tcpAppMap.containsKey(local) && tcpAppMap.get(local).getPid() == 0) {
												tcpAppMap.remove(local);
												if (!tcpAppMap.containsKey(local)) {
													tcpAppMap.put(local, this.getAppProp(tcpMatcher.group(6)));
												}
											}
										}
										if (verbose > 3) {
											if (isPid && udpAppMap.containsKey(local)) {
												System.out.println("# -> TCP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort + " " + udpAppMap.get(local).getPid());
											} else {
												System.out.println("# -> TCP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(ClsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
											logPath = logPath.replace("[", "").replace("]", "");
											String msg = prop.convUnixToJst(execNetstatTime, "yyyy/MM/dd HH:mm:ss") + " TCP I " + line + System.lineSeparator();
											prop.writeFile(logPath, msg, true);
										}
									} else {
										// 送信：ローカル ⇒ リモート：ポート
										String localVal = localAddr;
										if (isAddPortAny) {
											localVal += ":ANY";
											if (localPortNum > 0 && (localPortNum < 1024 || tcpListenPortList.contains(localPortNum))) {
												localVal = localAddr + ":" + localPort;
											}
										}
										List<String> sendList = tcpSendMap.get(localVal);
										if (sendList == null) {
											sendList = new ArrayList<>();
											tcpSendMap.put(localVal, sendList);
										}
										if (!sendList.contains(remote)) {
											sendList.add(remote);
											if (isPid && !tcpAppMap.containsKey(remote)) {
												tcpAppMap.put(remote, this.getAppProp(tcpMatcher.group(6)));
												if (verbose > 8) {
													System.out.println("# -> tcpAppMap.put(" + remote + ", PID=" + tcpAppMap.get(remote).getPid() + ")");
												}
											}
										} else {
											if (isPid && tcpAppMap.containsKey(remote) && tcpAppMap.get(remote).getPid() == 0) {
												tcpAppMap.remove(remote);
												if (!tcpAppMap.containsKey(remote)) {
													tcpAppMap.put(remote, this.getAppProp(tcpMatcher.group(6)));
												}
											}
										}
										if (verbose > 3) {
											if (isPid && udpAppMap.containsKey(remote)) {
												System.out.println("# -> TCP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort + " " + udpAppMap.get(remote).getPid());
											} else {
												System.out.println("# -> TCP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(ClsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
											logPath = logPath.replace("[", "").replace("]", "");
											String msg = prop.convUnixToJst(execNetstatTime, "yyyy/MM/dd HH:mm:ss") + " TCP O " + line + System.lineSeparator();
											prop.writeFile(logPath, msg, true);
										}
									}
								}
							}
						}
					}
				}

				if (blockId == ClsProperties.BLOCK_VALID || blockId == ClsProperties.BLOCK_UDP) {
					Matcher udpMatcher = pttnForUdp.matcher(line);
					if (udpMatcher.find()) {
						String localAddr = encodeStr(udpMatcher.group(1));
						String localPort = encodeStr(udpMatcher.group(2));
						String remoteAddr = encodeStr(udpMatcher.group(3));
						String remotePort = encodeStr(udpMatcher.group(4));
						if (localAddr.equals("_ALL_")) {
							localAddr = "0.0.0.0";
						}
						String local = localAddr + "_" + localPort;
						String remote = remoteAddr + "_" + remotePort;
						int localPortNum = 0;
						int remotePortNum = 0;
						try {
							localPortNum = Integer.parseInt(localPort);
						} catch (NumberFormatException ignored) {
							// ignore
						}
						try {
							remotePortNum = Integer.parseInt(remotePort);
						} catch (NumberFormatException ignored) {
							// ignore
						}

						if (this.isUdpConnState(remote)) {
							if (!this.isLoopbackAddr(localAddr)) {
								if (!localAddr.equals(remoteAddr)) {
									// 受信：リモート ⇒ ローカル：ポート
									if ((udpListenPortList.contains(localPortNum) || prop.getIpAddrList().contains(localAddr)) && !prop.getIpAddrList().contains(remoteAddr)) {
										String remoteVal = remoteAddr;
										if (isAddPortAny) {
											remoteVal += ":ANY";
											if (remotePortNum > 0 && remotePortNum < 1024) {
												remoteVal = remoteAddr + ":" + remotePort;
											}
										}
										List<String> recvList = udpRecvMap.get(local);
										if (recvList == null) {
											recvList = new ArrayList<>();
											udpRecvMap.put(local, recvList);
										}
										if (!recvList.contains(remoteVal)) {
											recvList.add(remoteVal);
											if (isPid && !udpAppMap.containsKey(local)) {
												udpAppMap.put(local, this.getAppProp(udpMatcher.group(5)));
												if (verbose > 8) {
													System.out.println("# -> udpAppMap.put(" + local + ", PID=" + udpAppMap.get(local).getPid() + ")");
												}
											}
										} else {
											if (isPid && udpAppMap.containsKey(local) && udpAppMap.get(local).getPid() == 0) {
												udpAppMap.remove(local);
												if (!udpAppMap.containsKey(local)) {
													udpAppMap.put(local, this.getAppProp(udpMatcher.group(5)));
												}
											}
										}
										if (verbose > 3) {
											if (isPid && udpAppMap.containsKey(local)) {
												System.out.println("# -> UDP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort + " " + udpAppMap.get(local).getPid());
											} else {
												System.out.println("# -> UDP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(ClsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
											logPath = logPath.replace("[", "").replace("]", "");
											String msg = prop.convUnixToJst(execNetstatTime, "yyyy/MM/dd HH:mm:ss") + " UDP I " + line + System.lineSeparator();
											prop.writeFile(logPath, msg, true);
										}
									} else {
										// 送信：ローカル ⇒ リモート：ポート
										String localVal = localAddr;
										if (isAddPortAny) {
											localVal += ":ANY";
											if (localPortNum > 0 && (localPortNum < 1024 || udpListenPortList.contains(localPortNum))) {
												localVal = localAddr + ":" + localPort;
											}
										}
										List<String> sendList = udpSendMap.get(localVal);
										if (sendList == null) {
											sendList = new ArrayList<>();
											udpSendMap.put(localVal, sendList);
										}
										if (!sendList.contains(remote)) {
											sendList.add(remote);
											if (isPid && !udpAppMap.containsKey(remote)) {
												udpAppMap.put(remote, this.getAppProp(udpMatcher.group(5)));
												if (verbose > 8) {
													System.out.println("# -> udpAppMap.put(" + remote + ", PID=" + udpAppMap.get(remote).getPid() + ")");
												}
											}
										} else {
											if (isPid && udpAppMap.containsKey(remote) && udpAppMap.get(remote).getPid() == 0) {
												udpAppMap.remove(remote);
												if (!udpAppMap.containsKey(remote)) {
													udpAppMap.put(remote, this.getAppProp(udpMatcher.group(5)));
												}
											}
										}
										if (verbose > 3) {
											if (isPid && udpAppMap.containsKey(remote)) {
												System.out.println("# -> UDP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort + " " + udpAppMap.get(remote).getPid());
											} else {
												System.out.println("# -> UDP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(ClsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
											logPath = logPath.replace("[", "").replace("]", "");
											String msg = prop.convUnixToJst(execNetstatTime, "yyyy/MM/dd HH:mm:ss") + " UDP O " + line + System.lineSeparator();
											prop.writeFile(logPath, msg, true);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		if (verbose > 5) {
			System.out.println("# -> ClsNetstat.getConnList() : tcpRecvMap.size() = " + tcpRecvMap.size());
			System.out.println("# -> ClsNetstat.getConnList() : tcpSendMap.size() = " + tcpSendMap.size());
			System.out.println("# -> ClsNetstat.getConnList() : udpRecvMap.size() = " + udpRecvMap.size());
			System.out.println("# -> ClsNetstat.getConnList() : udpSendMap.size() = " + udpSendMap.size());
			System.out.println("# -> ClsNetstat.getConnList() : tcpAppMap.size() = " + tcpAppMap.size());
			System.out.println("# -> ClsNetstat.getConnList() : udpAppMap.size() = " + udpAppMap.size());
			System.out.println("# -> ClsNetstat.getConnList() : cmdStdOutList.size() = " + cmdStdOutList.size());
		}
	}

	/**
	 * 集計結果（ホスト情報、LISTENポート一覧、TCP/UDP 送受信コネクション一覧）を標準出力および出力ファイルへ書き出します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * netstat.showList();
	 * </pre>
	 */
	public void showList() {
		boolean isFileOut = false;
		String outputDir = prop.getValue(ClsProperties.PATHDOUT, "");
		if (outputDir != null && !outputDir.isEmpty()) {
			isFileOut = true;
		}

		String encName = prop.getValue(ClsProperties.OUTPUT_FILE_ENCODING, prop.getDefEncoding(prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName()))));
		File outFile = isFileOut ? new File(outputDir, prop.getDefaultName()) : null;

		try {
			BufferedWriter bw = null;
			FileOutputStream fos = null;
			OutputStreamWriter osw = null;
			if (isFileOut && outFile != null) {
				fos = new FileOutputStream(outFile);
				osw = new OutputStreamWriter(fos, Charset.forName(encName));
				bw = new BufferedWriter(osw);
			}

			try {
				String strRun = "# START       : " + prop.convUnixToJst(prop.getValue(ClsProperties.START_TIME_MSEC, System.currentTimeMillis()), "yyyy/MM/dd HH:mm:ss");
				String strEnd = "# E N D       : " + prop.convUnixToJst(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss");
				if (verbose > 0) {
					System.out.println("############################################################");
					System.out.println("# INFO");
					System.out.println("############################################################");
					System.out.println(strRun);
					System.out.println(strEnd);
					System.out.println("# CYCLE COUNT : " + loopCount + " / " + prop.getValue(ClsProperties.MAX_LOOP_COUNT, ClsProperties.DEFAULT_MAX_LOOP_COUNT));
					System.out.println("# SLEEP SEC   : " + prop.getValue(ClsProperties.SLEEP_SEC, ClsProperties.DEFAULT_SLEEP_SEC));
					System.out.println("# EXEC TIME   : " + prop.getValue(ClsProperties.EXECUTION_TIME_SEC, ClsProperties.DEFAULT_EXECUTION_TIME_SEC));
					System.out.println("# OS NAME     : " + prop.getOsName() + " -> " + prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName()));
					System.out.println("# HOSTNAME    : " + prop.getHostName());
					for (int ipCnt = 0; ipCnt < prop.getIpAddrList().size(); ipCnt++) {
						System.out.println("# IP ADDRESS[" + ipCnt + "] : " + prop.getIpAddrList().get(ipCnt));
					}
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
					bw.write("# INFO");
					bw.newLine();
					bw.write("############################################################");
					bw.newLine();
					bw.write(strRun);
					bw.newLine();
					bw.write(strEnd);
					bw.newLine();
					bw.write("# CYCLE COUNT : " + loopCount + " / " + prop.getValue(ClsProperties.MAX_LOOP_COUNT, ClsProperties.DEFAULT_MAX_LOOP_COUNT));
					bw.newLine();
					bw.write("# SLEEP SEC   : " + prop.getValue(ClsProperties.SLEEP_SEC, ClsProperties.DEFAULT_SLEEP_SEC));
					bw.newLine();
					bw.write("# EXEC TIME   : " + prop.getValue(ClsProperties.EXECUTION_TIME_SEC, ClsProperties.DEFAULT_EXECUTION_TIME_SEC));
					bw.newLine();
					bw.write("# OS NAME     : " + prop.getOsName() + " -> " + prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName()));
					bw.newLine();
					bw.write("# HOSTNAME    : " + prop.getHostName());
					bw.newLine();
					for (int ipCnt = 0; ipCnt < prop.getIpAddrList().size(); ipCnt++) {
						bw.write("# IP ADDRESS[" + ipCnt + "] : " + prop.getIpAddrList().get(ipCnt));
						bw.newLine();
					}
				}

				// TCP LISTEN
				if (verbose > 0) {
					System.out.println("############################################################");
					System.out.println("# TCP LISTEN PORT LIST");
					System.out.println("############################################################");
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
					bw.write("# TCP LISTEN PORT LIST");
					bw.newLine();
					bw.write("############################################################");
					bw.newLine();
				}
				for (String elemTcpLstn : tcpListenPortStrList) {
					String buff = "TCP L " + decodeStr(elemTcpLstn);
					if (isPid && tcpAppMap.containsKey(elemTcpLstn)) {
						buff = buff + " " + tcpAppMap.get(elemTcpLstn).getPid() + " " + tcpAppMap.get(elemTcpLstn).getAppName() + " " + tcpAppMap.get(elemTcpLstn).getAppPath();
					}
					System.out.println(buff);
					if (isFileOut && bw != null) {
						bw.write(buff);
						bw.newLine();
					}
				}

				// UDP LISTEN
				if (verbose > 0) {
					System.out.println("############################################################");
					System.out.println("# UDP LISTEN PORT LIST");
					System.out.println("############################################################");
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
					bw.write("# UDP LISTEN PORT LIST");
					bw.newLine();
					bw.write("############################################################");
					bw.newLine();
				}
				for (String elemUdpLstn : udpListenPortStrList) {
					String buff = "UDP L " + decodeStr(elemUdpLstn);
					if (isPid && udpAppMap.containsKey(elemUdpLstn)) {
						buff = buff + " " + udpAppMap.get(elemUdpLstn).getPid() + " " + udpAppMap.get(elemUdpLstn).getAppName() + " " + udpAppMap.get(elemUdpLstn).getAppPath();
					}
					System.out.println(buff);
					if (isFileOut && bw != null) {
						bw.write(buff);
						bw.newLine();
					}
				}

				// TCP INBOUND
				if (verbose > 0) {
					System.out.println("############################################################");
					System.out.println("# TCP INBOUND CONNECTION");
					System.out.println("############################################################");
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
					bw.write("# TCP INBOUND CONNECTION");
					bw.newLine();
					bw.write("############################################################");
					bw.newLine();
				}
				for (Map.Entry<String, List<String>> entryTcpRecv : tcpRecvMap.entrySet()) {
					Collections.sort(entryTcpRecv.getValue());
					for (String elem : entryTcpRecv.getValue()) {
						String buff = "TCP I " + decodeStr(elem) + " => " + decodeStr(entryTcpRecv.getKey());
						if (isPid && tcpAppMap.containsKey(entryTcpRecv.getKey())) {
							buff = buff + " " + tcpAppMap.get(entryTcpRecv.getKey()).getPid() + " " + tcpAppMap.get(entryTcpRecv.getKey()).getAppName() + " " + tcpAppMap.get(entryTcpRecv.getKey()).getAppPath();
						}
						System.out.println(buff);
						if (isFileOut && bw != null) {
							bw.write(buff);
							bw.newLine();
						}
					}
				}

				// TCP OUTBOUND
				if (verbose > 0) {
					System.out.println("############################################################");
					System.out.println("# TCP OUTBOUND CONNECTION");
					System.out.println("############################################################");
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
					bw.write("# TCP OUTBOUND CONNECTION");
					bw.newLine();
					bw.write("############################################################");
					bw.newLine();
				}
				for (Map.Entry<String, List<String>> entryTcpSend : tcpSendMap.entrySet()) {
					Collections.sort(entryTcpSend.getValue());
					for (String elem : entryTcpSend.getValue()) {
						String buff = "TCP O " + decodeStr(entryTcpSend.getKey()) + " => " + decodeStr(elem);
						if (isPid && tcpAppMap.containsKey(elem)) {
							buff = buff + " " + tcpAppMap.get(elem).getPid() + " " + tcpAppMap.get(elem).getAppName() + " " + tcpAppMap.get(elem).getAppPath();
						}
						System.out.println(buff);
						if (isFileOut && bw != null) {
							bw.write(buff);
							bw.newLine();
						}
					}
				}

				// UDP INBOUND
				if (verbose > 0) {
					System.out.println("############################################################");
					System.out.println("# UDP INBOUND CONNECTION");
					System.out.println("############################################################");
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
					bw.write("# UDP INBOUND CONNECTION");
					bw.newLine();
					bw.write("############################################################");
					bw.newLine();
				}
				for (Map.Entry<String, List<String>> entryUdpRecv : udpRecvMap.entrySet()) {
					Collections.sort(entryUdpRecv.getValue());
					for (String elem : entryUdpRecv.getValue()) {
						String buff = "UDP I " + decodeStr(elem) + " => " + decodeStr(entryUdpRecv.getKey());
						if (isPid && udpAppMap.containsKey(entryUdpRecv.getKey())) {
							buff = buff + " " + udpAppMap.get(entryUdpRecv.getKey()).getPid() + " " + udpAppMap.get(entryUdpRecv.getKey()).getAppName() + " " + udpAppMap.get(entryUdpRecv.getKey()).getAppPath();
						}
						System.out.println(buff);
						if (isFileOut && bw != null) {
							bw.write(buff);
							bw.newLine();
						}
					}
				}

				// UDP OUTBOUND
				if (verbose > 0) {
					System.out.println("############################################################");
					System.out.println("# UDP OUTBOUND CONNECTION");
					System.out.println("############################################################");
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
					bw.write("# UDP OUTBOUND CONNECTION");
					bw.newLine();
					bw.write("############################################################");
					bw.newLine();
				}
				for (Map.Entry<String, List<String>> entryUdpSend : udpSendMap.entrySet()) {
					Collections.sort(entryUdpSend.getValue());
					for (String elem : entryUdpSend.getValue()) {
						String buff = "UDP O " + decodeStr(entryUdpSend.getKey()) + " => " + decodeStr(elem);
						if (isPid && udpAppMap.containsKey(elem)) {
							buff = buff + " " + udpAppMap.get(elem).getPid() + " " + udpAppMap.get(elem).getAppName() + " " + udpAppMap.get(elem).getAppPath();
						}
						System.out.println(buff);
						if (isFileOut && bw != null) {
							bw.write(buff);
							bw.newLine();
						}
					}
				}

				if (verbose > 0) {
					System.out.println("############################################################");
				}
				if (isFileOut && bw != null) {
					bw.write("############################################################");
					bw.newLine();
				}
			} finally {
				if (bw != null) {
					bw.close();
				}
				if (osw != null) {
					osw.close();
				}
				if (fos != null) {
					fos.close();
				}
			}
		} catch (IOException ex) {
			System.err.println("IOException in showList: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}
