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
 * netstat コマンドの実行結果や出力ファイルを解析し、TCP/UDPのLISTENポートおよび対外接続状況を集計・出力するクラスです。
 */
public final class clsNetstat {

	/** プロパティ設定管理オブジェクト */
	private clsProperties prop = null;

	/** コマンド標準出力格納オブジェクト */
	private clsCmdStdOut cmdStdOut = null;

	/** コマンド実行管理オブジェクト */
	private clsCmdExec cmdExec = null;

	/** コマンド標準出力行リスト */
	private List<String> cmdStdOutList = new LinkedList<>();

	/** TCP LISTENポート文字列表現リスト */
	private List<String> tcpListenPortStrList = new ArrayList<>();

	/** UDP LISTENポート文字列表現リスト */
	private List<String> udpListenPortStrList = new ArrayList<>();

	/** TCP LISTENポート番号リスト */
	private List<Integer> tcpListenPortList = new ArrayList<>();

	/** UDP LISTENポート番号リスト */
	private List<Integer> udpListenPortList = new ArrayList<>();

	/** リモートホストログ対象IPv4アドレスリスト */
	private List<String> incIpV4Addrs = new ArrayList<>();

	/** リモートホストログ除外IPv4アドレスリスト */
	private List<String> excIpV4Addrs = new ArrayList<>();

	/** リモートホストログ対象IPv6アドレスリスト */
	private List<String> incIpV6Addrs = new ArrayList<>();

	/** リモートホストログ除外IPv6アドレスリスト */
	private List<String> excIpV6Addrs = new ArrayList<>();

	/** LISTEN状態文字列リスト */
	private List<String> listenStateList = new ArrayList<>();

	/** TCP送信接続マップ (ローカル => リモートリスト) */
	private Map<String, List<String>> tcpSendMap = new HashMap<>();

	/** TCP受信接続マップ (ローカル => リモートリスト) */
	private Map<String, List<String>> tcpRecvMap = new HashMap<>();

	/** UDP送信接続マップ (ローカル => リモートリスト) */
	private Map<String, List<String>> udpSendMap = new HashMap<>();

	/** UDP受信接続マップ (ローカル => リモートリスト) */
	private Map<String, List<String>> udpRecvMap = new HashMap<>();

	/** TCPプロセス情報マップ */
	private Map<String, clsAppProp> tcpAppMap = new HashMap<>();

	/** UDPプロセス情報マップ */
	private Map<String, clsAppProp> udpAppMap = new HashMap<>();

	/** 冗長ログ出力レベル */
	private int verbose = 0;

	/** ループ実行カウント */
	private int loopCount = 0;

	/** OS種別ID */
	private int osId = clsProperties.OS_OTHER;

	/** netstat実行時刻 (UNIXミリ秒) */
	private long execNetstatTime = 0;

	/** TCPブロック判定正規表現 */
	private String tcpBlockRegex = null;

	/** UDPブロック判定正規表現 */
	private String udpBlockRegex = null;

	/** SCTPブロック判定正規表現 */
	private String sctpBlockRegex = null;

	/** TCP接続抽出正規表現 */
	private String tcpConnRegex = null;

	/** UDP接続抽出正規表現 */
	private String udpConnRegex = null;

	/** TCP LISTEN抽出正規表現 */
	private String tcpLstnRegex = null;

	/** UDP LISTEN抽出正規表現 */
	private String udpLstnRegex = null;

	/** 実行コマンドパス */
	private String cmdPath = null;

	/** コマンド引数文字列 */
	private String cmdArgs = null;

	/** IPv4ループバックアドレス */
	private String ipV4LoopbackAddr = null;

	/** IPv6ループバックアドレス (Windows) */
	private String ipV6LoopbackAddrWin = null;

	/** IPv6ループバックアドレス (Linux) */
	private String ipV6LoopbackAddrLinux = null;

	/** IPv4 UDP LISTEN状態ワイルドカード */
	private String ipV4UdpLstnStateWildcard = null;

	/** IPv4 UDP LISTEN状態 (HPUX) */
	private String ipV4UdpLstnStateHpux = null;

	/** IPv4 UDP LISTEN状態 (4Bitワイルドカード) */
	private String ipV4UdpLstnStateWildcard4Bit = null;

	/** IPv6 UDP LISTEN状態 */
	private String ipV6UdpLstnState = null;

	/** PID取得フラグ */
	private boolean isPid = false;

	/** リモートホストログ出力フラグ */
	private boolean isRHostLog = false;

	/** リモートホストログ対象IP指定フラグ */
	private boolean isRHostLogIncIp = false;

	/** リモートホストログ除外IP指定フラグ */
	private boolean isRHostLogExcIp = false;

	/** 高位ポート:ANY変換フラグ */
	private boolean isAddPortAny = true;

	/**
	 * プロパティ設定オブジェクトを指定して clsNetstat を初期化します。
	 *
	 * <pre>
	 * clsProperties prop = new clsProperties();
	 * clsNetstat netstat = new clsNetstat(prop);
	 * </pre>
	 *
	 * @param prop プロパティ管理オブジェクト
	 */
	public clsNetstat(final clsProperties prop) {
		this.prop = prop;
		this.cmdStdOut = new clsCmdStdOut();
		this.cmdExec = new clsCmdExec();
		this.cmdExec.setCmdStdOut(this.cmdStdOut);
		this.clear();
	}

	/**
	 * コマンド標準出力リストを設定します。
	 *
	 * <pre>
	 * netstat.setStdOutList(Arrays.asList("line1", "line2"));
	 * </pre>
	 *
	 * @param list 標準出力リスト
	 */
	public void setStdOutList(final List<String> list) {
		this.cmdStdOutList = (list != null) ? list : new LinkedList<>();
	}

	/**
	 * 実行ループカウントを設定します。
	 *
	 * <pre>
	 * netstat.setLoopCount(1);
	 * </pre>
	 *
	 * @param count ループカウント
	 */
	public void setLoopCount(final int count) {
		this.loopCount = count;
	}

	/**
	 * コマンド標準出力リストを取得します。
	 *
	 * <pre>
	 * List&lt;String&gt; outList = netstat.getStdOutList();
	 * </pre>
	 *
	 * @return 標準出力リスト
	 */
	public List<String> getStdOutList() {
		return this.cmdStdOutList;
	}

	/**
	 * 実行ループカウントを取得します。
	 *
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
	 * netstat 実行に必要な正規表現・パラメータ等の初期化を行います。
	 *
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

		verbose = prop.getValue(clsProperties.VERBOSE, clsProperties.DEFAULT_VERBOSE);
		isPid = prop.getValue(clsProperties.IS_PID, false);
		isAddPortAny = prop.getValue(clsProperties.IS_PORT_ANY, clsProperties.DEFAULT_IS_PORT_ANY);

		osId = prop.getOsId(prop.getValue(clsProperties.OS_NAME, prop.getOsShortName()));
		if (osId == clsProperties.OS_SOLARIS) {
			if (!listenStateList.contains("BOUND")) {
				listenStateList.add("BOUND");
			}
		}
		cmdPath = prop.getValue(clsProperties.COMMAND, clsProperties.DEFAULT_COMMAND);
		cmdArgs = prop.getCommandArgs();

		tcpBlockRegex = prop.getValue(clsProperties.TCP_BLOCK_REGEX, clsProperties.DEFAULT_TCP_BLOCK_REGEX);
		udpBlockRegex = prop.getValue(clsProperties.UDP_BLOCK_REGEX, clsProperties.DEFAULT_UDP_BLOCK_REGEX);
		sctpBlockRegex = prop.getValue(clsProperties.SCTP_BLOCK_REGEX, clsProperties.DEFAULT_SCTP_BLOCK_REGEX);
		tcpConnRegex = prop.getTcpConnRegex();
		udpConnRegex = prop.getUdpConnRegex();
		tcpLstnRegex = (osId == clsProperties.OS_SOLARIS ? prop.getValue(clsProperties.TCP_LSTN_REGEX, clsProperties.SOL_TCP_LSTN_REGEX) : tcpConnRegex);
		udpLstnRegex = (osId == clsProperties.OS_SOLARIS ? prop.getValue(clsProperties.UDP_LSTN_REGEX, clsProperties.SOL_UDP_LSTN_REGEX) : udpConnRegex);

		cmdExec.setWatchdog(prop.getValue(clsProperties.TIMEOUT, clsProperties.DEFAULT_TIMEOUT));

		String tempPath = prop.getValue(clsProperties.PATHDRHOSTLOG, "");
		if (!tempPath.isEmpty() && prop.isExist(tempPath)) {
			isRHostLog = true;
		}
		tempPath = prop.getValue(clsProperties.PATHFRHOSTLOG_I_IP, "");
		if (!tempPath.isEmpty() && prop.isExist(tempPath)) {
			isRHostLogIncIp = true;
		}
		tempPath = prop.getValue(clsProperties.PATHFRHOSTLOG_X_IP, "");
		if (!tempPath.isEmpty() && prop.isExist(tempPath)) {
			isRHostLogExcIp = true;
		}
	}

	/**
	 * 収集・集計したポートおよび接続マップ情報をクリアします。
	 *
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
	 * プロセスID文字列からアプリケーション情報オブジェクトを構築して取得します。
	 *
	 * <pre>
	 * clsAppProp app = netstat.getAppProp("1234/httpd");
	 * </pre>
	 *
	 * @param pidStr PID文字列（Linuxの場合は "pid/name" 形式）
	 * @return アプリケーションプロパティ
	 */
	public clsAppProp getAppProp(final String pidStr) {
		clsAppProp appProp = new clsAppProp();
		if (pidStr == null || pidStr.trim().isEmpty()) {
			return appProp;
		}
		switch (osId) {
			case clsProperties.OS_WIN:
				appProp.setPid(pidStr.trim());
				break;
			case clsProperties.OS_LINUX:
				String[] elem = pidStr.split("/");
				appProp.setPid(elem[0].trim());
				if (1 < elem.length) {
					appProp.setAppName(elem[1].trim().replaceAll("\\s+", "_"));
				}
				break;
			default:
				break;
		}
		if (8 < verbose) {
			System.out.println("# -> clsNetstat.getAppProp(" + pidStr + ") : PID=" + appProp.getPid() + " / NAME=" + appProp.getAppName() + " / PATH=" + appProp.getAppPath());
		}
		return appProp;
	}

	/**
	 * Mapのキーとして使用するために特殊文字（コロン、アスタリスク）をエンコードします。
	 *
	 * <pre>
	 * String encoded = netstat.encodeStr("192.168.1.1:80");
	 * </pre>
	 *
	 * @param key エンコード対象文字列
	 * @return エンコード後文字列
	 */
	public String encodeStr(final String key) {
		String retStr = "";
		if (key != null && !key.isEmpty()) {
			retStr = key.replace(":", "_").replace("*", "_ALL_");
		}
		return retStr;
	}

	/**
	 * エンコードされたキー文字列を元の文字列表現に復元します。
	 *
	 * <pre>
	 * String decoded = netstat.decodeStr("192.168.1.1_80");
	 * </pre>
	 *
	 * @param key デコード対象文字列
	 * @return 復元された文字列
	 */
	public String decodeStr(final String key) {
		String retStr = "";
		if (key != null && !key.isEmpty()) {
			retStr = key.replace("_ALL_", "*").replace("_", ":");
		}
		return retStr;
	}

	/**
	 * 指定アドレスがループバックアドレス（127.0.0.1, [::1], ::1等）であるか判定します。
	 *
	 * <pre>
	 * boolean isLoop = netstat.isLoopbackAddr("127_0_0_1");
	 * </pre>
	 *
	 * @param checkAddr 判定対象アドレス文字列（エンコード済み）
	 * @return ループバックアドレスの場合は true、それ以外は false
	 */
	public boolean isLoopbackAddr(final String checkAddr) {
		if (checkAddr == null) {
			return false;
		}
		return checkAddr.equals(ipV4LoopbackAddr)
				|| checkAddr.equals(ipV6LoopbackAddrWin)
				|| checkAddr.equals(ipV6LoopbackAddrLinux);
	}

	/**
	 * TCPソケットの状態が LISTEN/LISTENING であるか判定します。
	 *
	 * <pre>
	 * boolean isLstn = netstat.isTcpListen("LISTEN");
	 * </pre>
	 *
	 * @param state 状態文字列
	 * @return LISTEN状態の場合は true、それ以外は false
	 */
	public boolean isTcpListen(final String state) {
		if (state == null) {
			return false;
		}
		return listenStateList.contains(state.toUpperCase());
	}

	/**
	 * UDPソケットの状態が LISTEN（待機）状態であるか判定します。
	 *
	 * <pre>
	 * boolean isLstn = netstat.isUdpListen("*:*");
	 * </pre>
	 *
	 * @param state 状態またはリモート接続文字列
	 * @return LISTEN状態の場合は true、それ以外は false
	 */
	public boolean isUdpListen(final String state) {
		if (state == null) {
			return false;
		}
		if (osId == clsProperties.OS_SOLARIS) {
			return listenStateList.contains(state.toUpperCase());
		}
		return state.equals(ipV4UdpLstnStateWildcard)
				|| state.equals(ipV4UdpLstnStateWildcard4Bit)
				|| state.equals(ipV6UdpLstnState)
				|| state.equals(ipV4UdpLstnStateHpux);
	}

	/**
	 * TCPソケットの状態がアクティブなコネクション（ESTABLISHED等）であるか判定します。
	 *
	 * <pre>
	 * boolean isConn = netstat.isTcpConnState("ESTABLISHED");
	 * </pre>
	 *
	 * @param state 状態文字列
	 * @return コネクション状態の場合は true、LISTENやSYN_等の場合は false
	 */
	public boolean isTcpConnState(final String state) {
		if (state == null) {
			return false;
		}
		String checkStr = state.toUpperCase();
		if (osId == clsProperties.OS_SOLARIS) {
			return !isTcpListen(state) && !checkStr.contains("IDLE") && !checkStr.contains("SYN_");
		}
		return !isTcpListen(state) && !checkStr.contains("SYN_");
	}

	/**
	 * UDPソケットの状態がアクティブな接続状態であるか判定します。
	 *
	 * <pre>
	 * boolean isConn = netstat.isUdpConnState("192.168.1.2:53");
	 * </pre>
	 *
	 * @param state リモートアドレス文字列
	 * @return 接続状態の場合は true、LISTEN状態の場合は false
	 */
	public boolean isUdpConnState(final String state) {
		return !isUdpListen(state);
	}

	/**
	 * 指定文字列がIPv4形式のアドレス（例: 192.168.0.1 または CIDR付き）であるか判定します。
	 *
	 * <pre>
	 * boolean isV4 = netstat.isIpv4Address("192.168.1.1");
	 * </pre>
	 *
	 * @param ipAddr IPアドレス文字列
	 * @return IPv4形式の場合は true、それ以外は false
	 */
	public boolean isIpv4Address(final String ipAddr) {
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
				// ignore
				return false;
			}
		}
		return true;
	}

	/**
	 * リモートホストログ出力用のフィルタ定義ファイルから対象/除外IPアドレス一覧を読み込みます。
	 *
	 * <pre>
	 * netstat.getValidIpAddrs();
	 * </pre>
	 */
	public void getValidIpAddrs() {
		if (isRHostLogIncIp) {
			incIpV4Addrs.clear();
			incIpV6Addrs.clear();
			String strPath = prop.getValue(clsProperties.PATHFRHOSTLOG_I_IP, "");
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
			String strPath = prop.getValue(clsProperties.PATHFRHOSTLOG_X_IP, "");
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
	 * 対象IPアドレスがログ出力対象条件に適合するか判定します。
	 *
	 * <pre>
	 * boolean valid = netstat.isValidIpAddr("192.168.1.100");
	 * </pre>
	 *
	 * @param ipAddr チェック対象IPアドレス
	 * @return 対象の場合は true、除外対象または非対象の場合は false
	 */
	public boolean isValidIpAddr(final String ipAddr) {
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
	 * netstat コマンドを実行し、標準出力を内部バッファに取り込みます。
	 *
	 * <pre>
	 * int ret = netstat.execNetstat();
	 * </pre>
	 *
	 * @return コマンド終了コード
	 */
	public int execNetstat() {
		cmdStdOut.clear();
		execNetstatTime = System.currentTimeMillis();
		int returnCode = cmdExec.execute(cmdPath, cmdArgs.split("\\s+"));
		cmdStdOutList = cmdStdOut.getStdOutList();
		if (2 < verbose) {
			System.out.println("# -> === " + prop.convUnixToJst(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss") + " / CYCLE : " + loopCount + " ===");
			if (4 < verbose) {
				System.out.println("# -> clsNetstat.execNetstat() : cmdStdOutList.size() = " + cmdStdOutList.size());
			}
		}
		return returnCode;
	}

	/**
	 * netstat の出力結果が保存されたファイルを読み込み、内部バッファにセットします。
	 *
	 * <pre>
	 * int ret = netstat.readFile(new File("netstat.txt"), Charset.forName("UTF-8"));
	 * </pre>
	 *
	 * @param file 読み込み対象ファイル
	 * @param charset 文字エンコーディング
	 * @return 終了コード（正常時は 0）
	 */
	public int readFile(final File file, final Charset charset) {
		int returnCode = 0;
		cmdStdOut.clear();
		if (0 < verbose) {
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
			System.err.println("IOEXCEPTION : " + file.getAbsolutePath() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("EXCEPTION : " + file.getAbsolutePath() + " : " + e.getMessage());
			e.printStackTrace();
		}
		return returnCode;
	}

	/**
	 * 内部バッファの netstat 出力から LISTEN 状態の TCP/UDP ポート一覧を抽出・ソートします。
	 *
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
		int blockId = clsProperties.BLOCK_VALID;
		if (osId == clsProperties.OS_SOLARIS) {
			blockId = clsProperties.BLOCK_NONE;
		}

		for (String line : cmdStdOutList) {
			if (line != null && !line.trim().isEmpty()) {
				boolean isOk = true;
				if (osId == clsProperties.OS_SOLARIS) {
					Matcher tcpBlockMatcher = pttnForTcpBlock.matcher(line);
					if (tcpBlockMatcher.find()) {
						blockId = clsProperties.BLOCK_TCP;
					}
					Matcher udpBlockMatcher = pttnForUdpBlock.matcher(line);
					if (udpBlockMatcher.find()) {
						blockId = clsProperties.BLOCK_UDP;
					}
					Matcher sctpBlockMatcher = pttnForSctpBlock.matcher(line);
					if (sctpBlockMatcher.find()) {
						blockId = clsProperties.BLOCK_NONE;
					}
				}

				if (blockId == clsProperties.BLOCK_VALID || blockId == clsProperties.BLOCK_TCP) {
					Matcher tcpMatcher = pttnForTcp.matcher(line);
					if (tcpMatcher.find()) {
						if (8 < verbose) {
							System.out.println("# -> TCP : tcpMatcher.find() : " + encodeStr(line));
						}
						String localAddr = encodeStr("" + tcpMatcher.group(1));
						String localPort = encodeStr("" + tcpMatcher.group(2));
						if ("_ALL_".equals(localAddr)) {
							localAddr = "0.0.0.0";
						}
						if ("_ALL_".equals(localPort)) {
							isOk = false;
						}
						String state = "" + tcpMatcher.group(5);

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
								if (0 < portNo && !tcpListenPortList.contains(portNo)) {
									tcpListenPortList.add(portNo);
								}
								if (!tcpListenPortStrList.contains(elem)) {
									tcpListenPortStrList.add(elem);
									if (isPid && !tcpAppMap.containsKey(elem)) {
										tcpAppMap.put(elem, this.getAppProp(tcpMatcher.group(6)));
										if (8 < verbose) {
											System.out.println("# -> tcpAppMap.put(" + elem + ", PID=" + tcpAppMap.get(elem).getPid() + ")");
										}
									}
								}
								if (6 < verbose) {
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

				if (blockId == clsProperties.BLOCK_VALID || blockId == clsProperties.BLOCK_UDP) {
					Matcher udpMatcher = pttnForUdp.matcher(line);
					if (udpMatcher.find()) {
						if (8 < verbose) {
							System.out.println("# -> UDP : pttnForUdp.find() : " + encodeStr(line));
						}
						String localAddr = encodeStr("" + udpMatcher.group(1));
						String localPort = encodeStr("" + udpMatcher.group(2));
						if ("_ALL_".equals(localAddr)) {
							localAddr = "0.0.0.0";
						}
						if ("_ALL_".equals(localPort)) {
							isOk = false;
						}
						String remoteAddr = "_ALL_";
						String remotePort = "_ALL_";
						if (osId == clsProperties.OS_SOLARIS) {
							String checkStr = udpMatcher.group(3).toUpperCase();
							if (!"IDLE".equals(checkStr) && !"LISTEN".equals(checkStr)) {
								isOk = false;
							}
						} else {
							remoteAddr = encodeStr("" + udpMatcher.group(3));
							remotePort = encodeStr("" + udpMatcher.group(4));
						}
						String remote = remoteAddr + "_" + remotePort;
						int localPortNum = 0;
						try {
							localPortNum = Integer.parseInt(localPort);
						} catch (NumberFormatException ignored) {
							// ignore
						}

						if (osId != clsProperties.OS_SOLARIS) {
							if (!isUdpListen(remote)) {
								isOk = false;
							}
						}
						if (isOk) {
							if (!this.isLoopbackAddr(localAddr)) {
								int portNo = localPortNum;
								String elem = localAddr + "_" + localPort;
								if (0 < portNo && !udpListenPortList.contains(portNo)) {
									udpListenPortList.add(portNo);
								}
								if (!udpListenPortStrList.contains(elem)) {
									udpListenPortStrList.add(elem);
									if (isPid && !udpAppMap.containsKey(elem)) {
										udpAppMap.put(elem, this.getAppProp(udpMatcher.group(5)));
										if (8 < verbose) {
											System.out.println("# -> udpAppMap.put(" + elem + ", PID=" + udpAppMap.get(elem).getPid() + ")");
										}
									}
								}
								if (6 < verbose) {
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

		if (5 < verbose) {
			System.out.println("# -> clsNetstat.getListenPorts() : tcpListenPortList.size() = " + tcpListenPortList.size());
			System.out.println("# -> clsNetstat.getListenPorts() : tcpListenPortStrList.size() = " + tcpListenPortStrList.size());
			System.out.println("# -> clsNetstat.getListenPorts() : udpListenPortList.size() = " + udpListenPortList.size());
			System.out.println("# -> clsNetstat.getListenPorts() : udpListenPortStrList.size() = " + udpListenPortStrList.size());
			System.out.println("# -> clsNetstat.getListenPorts() : tcpAppMap.size() = " + tcpAppMap.size());
			System.out.println("# -> clsNetstat.getListenPorts() : udpAppMap.size() = " + udpAppMap.size());
			System.out.println("# -> clsNetstat.getListenPorts() : cmdStdOutList.size() = " + cmdStdOutList.size());
		}
	}

	/**
	 * 内部バッファの netstat 出力から外部との送受信コネクション一覧を抽出・分類します。
	 *
	 * <pre>
	 * netstat.getConnList();
	 * </pre>
	 */
	public void getConnList() {
		Pattern pttnForTcpBlock = Pattern.compile(tcpBlockRegex);
		Pattern pttnForUdpBlock = Pattern.compile(udpBlockRegex);
		Pattern pttnForTcp = Pattern.compile(tcpConnRegex);
		Pattern pttnForUdp = Pattern.compile(udpConnRegex);
		int blockId = clsProperties.BLOCK_VALID;
		if (osId == clsProperties.OS_SOLARIS) {
			blockId = clsProperties.BLOCK_NONE;
		}

		if (isRHostLog) {
			getValidIpAddrs();
		}

		for (String line : cmdStdOutList) {
			if (line != null && !line.trim().isEmpty()) {
				Matcher tcpBlockMatcher = pttnForTcpBlock.matcher(line);
				if (tcpBlockMatcher.find()) {
					blockId = clsProperties.BLOCK_TCP;
				}
				Matcher udpBlockMatcher = pttnForUdpBlock.matcher(line);
				if (udpBlockMatcher.find()) {
					blockId = clsProperties.BLOCK_UDP;
				}

				if (blockId == clsProperties.BLOCK_VALID || blockId == clsProperties.BLOCK_TCP) {
					Matcher tcpMatcher = pttnForTcp.matcher(line);
					if (tcpMatcher.find()) {
						String localAddr = encodeStr("" + tcpMatcher.group(1));
						String localPort = encodeStr("" + tcpMatcher.group(2));
						String remoteAddr = encodeStr("" + tcpMatcher.group(3));
						String remotePort = encodeStr("" + tcpMatcher.group(4));
						if ("_ALL_".equals(localAddr)) {
							localAddr = "0.0.0.0";
						}
						String state = "" + tcpMatcher.group(5);
						String local = (localAddr + "_" + localPort);
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
											if (0 < remotePortNum && remotePortNum < 1024) {
												remoteVal = remoteAddr + ":" + remotePort;
											}
										}
										tcpRecvMap.computeIfAbsent(local, k -> new ArrayList<>());
										if (!tcpRecvMap.get(local).contains(remoteVal)) {
											tcpRecvMap.get(local).add(remoteVal);
											if (isPid && !tcpAppMap.containsKey(local)) {
												tcpAppMap.put(local, this.getAppProp(tcpMatcher.group(6)));
												if (8 < verbose) {
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
										if (3 < verbose) {
											if (isPid && udpAppMap.containsKey(local)) {
												System.out.println("# -> TCP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort + " " + udpAppMap.get(local).getPid());
											} else {
												System.out.println("# -> TCP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(clsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
											logPath = logPath.replace("[", "").replace("]", "");
											String msg = prop.convUnixToJst(execNetstatTime, "yyyy/MM/dd HH:mm:ss") + " TCP I " + line + System.lineSeparator();
											prop.writeFile(logPath, msg, true);
										}
									} else {
										// 送信：ローカル ⇒ リモート：ポート
										String localVal = localAddr;
										if (isAddPortAny) {
											localVal += ":ANY";
											if (0 < localPortNum && (localPortNum < 1024 || tcpListenPortList.contains(localPortNum))) {
												localVal = localAddr + ":" + localPort;
											}
										}
										tcpSendMap.computeIfAbsent(localVal, k -> new ArrayList<>());
										if (!tcpSendMap.get(localVal).contains(remote)) {
											tcpSendMap.get(localVal).add(remote);
											if (isPid && !tcpAppMap.containsKey(remote)) {
												tcpAppMap.put(remote, this.getAppProp(tcpMatcher.group(6)));
												if (8 < verbose) {
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
										if (3 < verbose) {
											if (isPid && udpAppMap.containsKey(remote)) {
												System.out.println("# -> TCP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort + " " + udpAppMap.get(remote).getPid());
											} else {
												System.out.println("# -> TCP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(clsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
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

				if (blockId == clsProperties.BLOCK_VALID || blockId == clsProperties.BLOCK_UDP) {
					Matcher udpMatcher = pttnForUdp.matcher(line);
					if (udpMatcher.find()) {
						String localAddr = encodeStr("" + udpMatcher.group(1));
						String localPort = encodeStr("" + udpMatcher.group(2));
						String remoteAddr = encodeStr("" + udpMatcher.group(3));
						String remotePort = encodeStr("" + udpMatcher.group(4));
						if ("_ALL_".equals(localAddr)) {
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
											if (0 < remotePortNum && remotePortNum < 1024) {
												remoteVal = remoteAddr + ":" + remotePort;
											}
										}
										udpRecvMap.computeIfAbsent(local, k -> new ArrayList<>());
										if (!udpRecvMap.get(local).contains(remoteVal)) {
											udpRecvMap.get(local).add(remoteVal);
											if (isPid && !udpAppMap.containsKey(local)) {
												udpAppMap.put(local, this.getAppProp(udpMatcher.group(5)));
												if (8 < verbose) {
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
										if (3 < verbose) {
											if (isPid && udpAppMap.containsKey(local)) {
												System.out.println("# -> UDP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort + " " + udpAppMap.get(local).getPid());
											} else {
												System.out.println("# -> UDP I " + remoteVal + "(" + remotePort + ") => " + localAddr + ":" + localPort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(clsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
											logPath = logPath.replace("[", "").replace("]", "");
											String msg = prop.convUnixToJst(execNetstatTime, "yyyy/MM/dd HH:mm:ss") + " UDP I " + line + System.lineSeparator();
											prop.writeFile(logPath, msg, true);
										}
									} else {
										// 送信：ローカル ⇒ リモート：ポート
										String localVal = localAddr;
										if (isAddPortAny) {
											localVal += ":ANY";
											if (0 < localPortNum && (localPortNum < 1024 || udpListenPortList.contains(localPortNum))) {
												localVal = localAddr + ":" + localPort;
											}
										}
										udpSendMap.computeIfAbsent(localVal, k -> new ArrayList<>());
										if (!udpSendMap.get(localVal).contains(remote)) {
											udpSendMap.get(localVal).add(remote);
											if (isPid && !udpAppMap.containsKey(remote)) {
												udpAppMap.put(remote, this.getAppProp(udpMatcher.group(5)));
												if (8 < verbose) {
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
										if (3 < verbose) {
											if (isPid && udpAppMap.containsKey(remote)) {
												System.out.println("# -> UDP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort + " " + udpAppMap.get(remote).getPid());
											} else {
												System.out.println("# -> UDP O " + localVal + "(" + localPort + ") => " + remoteAddr + ":" + remotePort);
											}
										}
										if (isRHostLog && isValidIpAddr(decodeStr(remoteAddr))) {
											String logPath = prop.getValue(clsProperties.PATHDRHOSTLOG, ".") + File.separator + remoteAddr + "_" + prop.convUnixToJst(execNetstatTime, "yyyyMMdd") + ".log";
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

		if (5 < verbose) {
			System.out.println("# -> clsNetstat.getConnList() : tcpRecvMap.size() = " + tcpRecvMap.size());
			System.out.println("# -> clsNetstat.getConnList() : tcpSendMap.size() = " + tcpSendMap.size());
			System.out.println("# -> clsNetstat.getConnList() : udpRecvMap.size() = " + udpRecvMap.size());
			System.out.println("# -> clsNetstat.getConnList() : udpSendMap.size() = " + udpSendMap.size());
			System.out.println("# -> clsNetstat.getConnList() : tcpAppMap.size() = " + tcpAppMap.size());
			System.out.println("# -> clsNetstat.getConnList() : udpAppMap.size() = " + udpAppMap.size());
			System.out.println("# -> clsNetstat.getConnList() : cmdStdOutList.size() = " + cmdStdOutList.size());
		}
	}

	/**
	 * 集計結果（ホスト情報、LISTENポート一覧、送受信コネクション一覧）を標準出力および出力ファイルへ書き出します。
	 *
	 * <pre>
	 * netstat.showList();
	 * </pre>
	 */
	public void showList() {
		boolean isFileOut = false;
		String outputDir = prop.getValue(clsProperties.PATHDOUT, "");
		if (outputDir != null && !outputDir.isEmpty()) {
			isFileOut = true;
		}

		String encName = prop.getValue(clsProperties.OUTPUT_FILE_ENCODING, prop.getDefEncoding(prop.getOsId(prop.getValue(clsProperties.OS_NAME, prop.getOsShortName()))));
		File outFile = isFileOut ? new File(outputDir, prop.getDefaultName()) : null;

		String strRun = "# START       : " + prop.convUnixToJst(prop.getValue(clsProperties.START_TIME_MSEC, System.currentTimeMillis()), "yyyy/MM/dd HH:mm:ss");
		String strEnd = "# E N D       : " + prop.convUnixToJst(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss");

		try (FileOutputStream fos = (isFileOut && outFile != null) ? new FileOutputStream(outFile) : null;
			 OutputStreamWriter osw = (fos != null) ? new OutputStreamWriter(fos, Charset.forName(encName)) : null;
			 BufferedWriter bw = (osw != null) ? new BufferedWriter(osw) : null) {

			if (0 < verbose) {
				System.out.println("############################################################");
				System.out.println("# INFO");
				System.out.println("############################################################");
				System.out.println(strRun);
				System.out.println(strEnd);
				System.out.println("# CYCLE COUNT : " + loopCount + " / " + prop.getValue(clsProperties.MAX_LOOP_COUNT, clsProperties.DEFAULT_MAX_LOOP_COUNT));
				System.out.println("# SLEEP SEC   : " + prop.getValue(clsProperties.SLEEP_SEC, clsProperties.DEFAULT_SLEEP_SEC));
				System.out.println("# EXEC TIME   : " + prop.getValue(clsProperties.EXECUTION_TIME_SEC, clsProperties.DEFAULT_EXECUTION_TIME_SEC));
				System.out.println("# OS NAME     : " + prop.getOsName() + " -> " + prop.getValue(clsProperties.OS_NAME, prop.getOsShortName()));
				System.out.println("# HOSTNAME    : " + prop.getHostName());
				for (int ipCnt = 0; ipCnt < prop.getIpAddrList().size(); ipCnt++) {
					System.out.println("# IP ADDRESS[" + ipCnt + "] : " + prop.getIpAddrList().get(ipCnt));
				}
			}
			if (bw != null) {
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
				bw.write("# CYCLE COUNT : " + loopCount + " / " + prop.getValue(clsProperties.MAX_LOOP_COUNT, clsProperties.DEFAULT_MAX_LOOP_COUNT));
				bw.newLine();
				bw.write("# SLEEP SEC   : " + prop.getValue(clsProperties.SLEEP_SEC, clsProperties.DEFAULT_SLEEP_SEC));
				bw.newLine();
				bw.write("# EXEC TIME   : " + prop.getValue(clsProperties.EXECUTION_TIME_SEC, clsProperties.DEFAULT_EXECUTION_TIME_SEC));
				bw.newLine();
				bw.write("# OS NAME     : " + prop.getOsName() + " -> " + prop.getValue(clsProperties.OS_NAME, prop.getOsShortName()));
				bw.newLine();
				bw.write("# HOSTNAME    : " + prop.getHostName());
				bw.newLine();
				for (int ipCnt = 0; ipCnt < prop.getIpAddrList().size(); ipCnt++) {
					bw.write("# IP ADDRESS[" + ipCnt + "] : " + prop.getIpAddrList().get(ipCnt));
					bw.newLine();
				}
			}

			// TCP LISTEN
			if (0 < verbose) {
				System.out.println("############################################################");
				System.out.println("# TCP LISTEN PORT LIST");
				System.out.println("############################################################");
			}
			if (bw != null) {
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
				if (bw != null) {
					bw.write(buff);
					bw.newLine();
				}
			}

			// UDP LISTEN
			if (0 < verbose) {
				System.out.println("############################################################");
				System.out.println("# UDP LISTEN PORT LIST");
				System.out.println("############################################################");
			}
			if (bw != null) {
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
				if (bw != null) {
					bw.write(buff);
					bw.newLine();
				}
			}

			// TCP INBOUND
			if (0 < verbose) {
				System.out.println("############################################################");
				System.out.println("# TCP INBOUND CONNECTION");
				System.out.println("############################################################");
			}
			if (bw != null) {
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
					if (bw != null) {
						bw.write(buff);
						bw.newLine();
					}
				}
			}

			// TCP OUTBOUND
			if (0 < verbose) {
				System.out.println("############################################################");
				System.out.println("# TCP OUTBOUND CONNECTION");
				System.out.println("############################################################");
			}
			if (bw != null) {
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
					if (bw != null) {
						bw.write(buff);
						bw.newLine();
					}
				}
			}

			// UDP INBOUND
			if (0 < verbose) {
				System.out.println("############################################################");
				System.out.println("# UDP INBOUND CONNECTION");
				System.out.println("############################################################");
			}
			if (bw != null) {
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
					if (bw != null) {
						bw.write(buff);
						bw.newLine();
					}
				}
			}

			// UDP OUTBOUND
			if (0 < verbose) {
				System.out.println("############################################################");
				System.out.println("# UDP OUTBOUND CONNECTION");
				System.out.println("############################################################");
			}
			if (bw != null) {
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
					if (bw != null) {
						bw.write(buff);
						bw.newLine();
					}
				}
			}

			if (0 < verbose) {
				System.out.println("############################################################");
			}
			if (bw != null) {
				bw.write("############################################################");
				bw.newLine();
			}
		} catch (Exception ex) {
			System.err.println("Exception " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}
