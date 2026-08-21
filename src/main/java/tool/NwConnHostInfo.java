package tool;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ネットワーク接続状況 (netstat) およびホスト情報を定期収集・集計・出力するメインエントリーポイントクラスです。
 * <p>
 * コマンドライン引数を解析し、リアルタイムでの netstat サイクル実行 (exe モード) または
 * 事前に取得された netstat ログファイルのパース (file モード) を実行します。
 * LISTEN ポート一覧、INBOUND/OUTBOUND 接続、PID/プロセス名の紐付け結果を標準出力および指定ディレクトリへ書き出します。
 * </p>
 *
 * <p>使用例:</p>
 * <pre>
 * // コマンドラインからの起動例
 * NwConnHostInfo.main(new String[]{"-m", "exe", "-c", "5", "-s", "10", "-o", "./log"});
 *
 * // プログラムからの呼出例（JVM強制終了なし）
 * NwConnHostInfo app = new NwConnHostInfo(new String[]{"-h"}, false);
 * </pre>
 */
public class NwConnHostInfo {

	/** クラス名（ログ用） */
	private static final String CLASS_NAME = "NwConnHostInfo";

	/** プロパティ設定管理オブジェクト */
	private ClsProperties prop = null;

	/** netstat 解析・集計オブジェクト */
	private ClsNetstat netstat = null;

	/** SIGINT/シャットダウンフックスレッド */
	private Thread shutdownHookThread = null;

	/** 実行開始時刻ミリ秒 */
	private long startTimeMillis = System.currentTimeMillis();

	/** 実行終了時刻ミリ秒 */
	private long endTimeMillis = 0;

	/** 終了時に JVM を強制終了 (System.exit/Runtime.halt) させるかどうかのフラグ */
	private boolean isKillJvm = false;

	/** キャンセル・中断フラグ */
	private boolean isCancel = false;

	/** シャットダウンフック実行中フラグ */
	private boolean isRunningShutdownHook = false;

	/** コマンドライン引数マップ */
	private Map<String, String> argMap = new LinkedHashMap<>();

	/**
	 * コマンドライン実行時のエントリーポイントです。
	 * <p>
	 * 引数を解析してアプリケーションを実行し、終了時に JVM を終了します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * NwConnHostInfo.main(new String[]{"-v", "-m", "exe"});
	 * </pre>
	 *
	 * @param args コマンドライン引数配列
	 */
	public static void main(String[] args) {
		new NwConnHostInfo(args, true);
	}

	/**
	 * 引数配列と JVM 強制終了フラグを指定して NwConnHostInfo を初期化・実行します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * NwConnHostInfo app = new NwConnHostInfo(new String[]{"--show-ipaddr"}, false);
	 * </pre>
	 *
	 * @param args コマンドライン引数配列
	 * @param isKillJvm 終了時に System.exit() または Runtime.halt() でJVMごと終了させる場合は true
	 */
	public NwConnHostInfo(String[] args, boolean isKillJvm) {
		exec(args, isKillJvm);
	}

	/**
	 * コマンドライン引数を解析し、netstat 情報の収集・出力メインロジックを実行します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * exec(args, isKillJvm);
	 * </pre>
	 *
	 * @param args コマンドライン引数配列
	 * @param isKillJvm JVM終了フラグ
	 */
	private void exec(String[] args, boolean isKillJvm) {
		String optionsStr = null;
		String optionsDelimiter = "[,|]";
		int returnCode = ClsProperties.LVL_INFO;
		int executionTimeSec = 0;
		int maxLoopCount = 0;
		int sleepSec = 0;
		boolean isShowUsage = false;
		boolean isShowSampleConfig = false;

		this.isKillJvm = isKillJvm;
		this.prop = new ClsProperties();
		this.argMap.put(ClsProperties.START_TIME_MSEC, String.valueOf(this.startTimeMillis));

		//----------------------------------------------------------------------
		// 引数処理
		//----------------------------------------------------------------------
		for (int i = 0; i < args.length; ++i) {
			if ("-h".equals(args[i]) || "--help".equalsIgnoreCase(args[i]) || "-help".equalsIgnoreCase(args[i]) || "-?".equals(args[i]) || "/?".equals(args[i])) {
				isShowUsage = true;
			} else if ("--show-sample-config".equalsIgnoreCase(args[i]) || "-show-sample-config".equalsIgnoreCase(args[i])) {
				isShowSampleConfig = true;
			} else if ("--show-ipaddr".equalsIgnoreCase(args[i]) || "-show-ipaddr".equalsIgnoreCase(args[i])) {
				System.out.println("HOSTNAME : " + prop.getHostName());
				for (int ipCnt = 0; ipCnt < prop.getIpAddrList().size(); ipCnt++) {
					System.out.println("IP ADDRESS[" + ipCnt + "] : " + prop.getIpAddrList().get(ipCnt));
				}
				terminate(0);
			} else if ("-v".equalsIgnoreCase(args[i]) || "--verbose".equalsIgnoreCase(args[i]) || "-verbose".equalsIgnoreCase(args[i])) {
				argMap.put(ClsProperties.VERBOSE, "1");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.VERBOSE, args[i + 1]);
					} catch (NumberFormatException ignored) {
						// ignore
					}
				}
			} else if ("--vv".equalsIgnoreCase(args[i]) || "-vv".equalsIgnoreCase(args[i])) {
				argMap.put(ClsProperties.VERBOSE, "2");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.VERBOSE, args[i + 1]);
					} catch (NumberFormatException ignored) {
						// ignore
					}
				}
			} else if ("--vvv".equalsIgnoreCase(args[i]) || "-vvv".equalsIgnoreCase(args[i])) {
				argMap.put(ClsProperties.VERBOSE, "3");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.VERBOSE, args[i + 1]);
					} catch (NumberFormatException ignored) {
						// ignore
					}
				}
			} else if ("--conf".equalsIgnoreCase(args[i]) || "-conf".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.PATHFCONF, args[i + 1]);
				}
			} else if ("-m".equalsIgnoreCase(args[i]) || "--mode".equalsIgnoreCase(args[i]) || "-mode".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.MODE, args[i + 1]);
				}
			} else if ("-c".equalsIgnoreCase(args[i]) || "--count".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.MAX_LOOP_COUNT, args[i + 1]);
					} catch (NumberFormatException objExcptn) {
						objExcptn.printStackTrace();
					}
				}
			} else if ("--et".equalsIgnoreCase(args[i]) || "-et".equalsIgnoreCase(args[i]) || "--execution-time".equalsIgnoreCase(args[i]) || "-execution-time".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.EXECUTION_TIME_SEC, args[i + 1]);
					} catch (NumberFormatException objExcptn) {
						objExcptn.printStackTrace();
					}
				}
			} else if ("-s".equalsIgnoreCase(args[i]) || "--sleep".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.SLEEP_SEC, args[i + 1]);
					} catch (NumberFormatException objExcptn) {
						objExcptn.printStackTrace();
					}
				}
			} else if ("--dir".equalsIgnoreCase(args[i]) || "-dir".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.FILE_PARSE_DIR, args[i + 1]);
				}
			} else if ("--file".equalsIgnoreCase(args[i]) || "-file".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.FILE_PARSE_NAME, args[i + 1]);
				}
			} else if ("--input-encode".equalsIgnoreCase(args[i]) || "-input-encode".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.INPUT_FILE_ENCODING, args[i + 1]);
				}
			} else if ("--hl".equals(args[i]) || "-hl".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.PATHDRHOSTLOG, args[i + 1]);
				}
			} else if ("--i-ip".equals(args[i]) || "-i-ip".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.PATHFRHOSTLOG_I_IP, args[i + 1]);
				}
			} else if ("--x-ip".equals(args[i]) || "-x-ip".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.PATHFRHOSTLOG_X_IP, args[i + 1]);
				}
			} else if ("-o".equals(args[i]) || "-out".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.PATHDOUT, args[i + 1]);
				}
			} else if ("--output-encode".equalsIgnoreCase(args[i]) || "-output-encode".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.OUTPUT_FILE_ENCODING, args[i + 1]);
				}
			} else if ("-n".equals(args[i]) || "-name".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.OUTPUT_FILE_NAME, args[i + 1]);
				}
			} else if ("--cmd".equalsIgnoreCase(args[i]) || "-cmd".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.COMMAND, args[i + 1]);
				}
			} else if ("--arg".equalsIgnoreCase(args[i]) || "-arg".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.ARGUMENT, args[i + 1]);
				}
			} else if ("--timeout".equalsIgnoreCase(args[i]) || "-timeout".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.TIMEOUT, args[i + 1]);
					} catch (NumberFormatException ignored) {
						// ignore
					}
				}
			} else if ("--tz".equalsIgnoreCase(args[i]) || "-tz".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.TIMEZONE, args[i + 1]);
				}
			} else if ("--tcp-regex".equalsIgnoreCase(args[i]) || "-tcp-regex".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.TCP_CONN_REGEX, args[i + 1]);
				}
			} else if ("--udp-regex".equalsIgnoreCase(args[i]) || "-udp-regex".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.UDP_CONN_REGEX, args[i + 1]);
				}
			} else if ("--os".equalsIgnoreCase(args[i]) || "-os".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					String osArg = args[i + 1].toLowerCase();
					if ("win".equals(osArg) || "windows".equals(osArg)) {
						argMap.put(ClsProperties.OS_NAME, "win");
					} else if ("linux".equals(osArg)) {
						argMap.put(ClsProperties.OS_NAME, "linux");
					} else if ("hpux".equals(osArg) || "hp-ux".equals(osArg)) {
						argMap.put(ClsProperties.OS_NAME, "hpux");
					} else if ("solaris".equals(osArg) || "sunos".equals(osArg)) {
						argMap.put(ClsProperties.OS_NAME, "solaris");
					} else {
						System.err.println("UNSUPPORTED OS (ARG --os) : " + args[i + 1]);
						isShowUsage = true;
					}
				}
			} else if ("--my-ip".equalsIgnoreCase(args[i]) || "-my-ip".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					argMap.put(ClsProperties.HOST_IP_ADDR_CSV, args[i + 1]);
				}
			} else if ("--pid".equalsIgnoreCase(args[i]) || "-pid".equalsIgnoreCase(args[i])) {
				argMap.put(ClsProperties.IS_PID, "true");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					if ("false".equalsIgnoreCase(args[i + 1])) {
						argMap.put(ClsProperties.IS_PID, "false");
					}
				}
			} else if ("--add-any".equalsIgnoreCase(args[i]) || "-add-any".equalsIgnoreCase(args[i])) {
				argMap.put(ClsProperties.IS_PORT_ANY, "true");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					String val = args[i + 1].toLowerCase();
					if ("false".equals(val) || "n".equals(val) || "no".equals(val) || "none".equals(val) || "off".equals(val) || "0".equals(val)) {
						argMap.put(ClsProperties.IS_PORT_ANY, "false");
					}
				}
			} else if ("--trace".equalsIgnoreCase(args[i]) || "-trace".equalsIgnoreCase(args[i])) {
				argMap.put(ClsProperties.IS_TRACE_LOG, "1");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						Integer.parseInt(args[i + 1]);
						argMap.put(ClsProperties.IS_TRACE_LOG, args[i + 1]);
					} catch (NumberFormatException ignored) {
						// ignore
					}
				}
			} else if ("--options".equals(args[i]) || "-options".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty()) {
					optionsStr = args[i + 1];
				}
			} else if ("--option-delimiter".equals(args[i]) || "-option-delimiter".equalsIgnoreCase(args[i])) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty()) {
					optionsDelimiter = args[i + 1];
				}
			}
		}

		//----------------------------------------------------------------------
		// プロパティファイルの読み込み
		//----------------------------------------------------------------------
		if (argMap.containsKey(ClsProperties.PATHFCONF) && !argMap.get(ClsProperties.PATHFCONF).isEmpty()) {
			String confPath = argMap.get(ClsProperties.PATHFCONF);
			String defaultEncodingName = prop.getDefEncoding(prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName())));
			if (prop.isExist(confPath)) {
				String encodingName = defaultEncodingName;
				if (argMap.containsKey(ClsProperties.CNF_FILE_ENCODING) && !argMap.get(ClsProperties.CNF_FILE_ENCODING).isEmpty()) {
					encodingName = argMap.get(ClsProperties.CNF_FILE_ENCODING);
				}
				if (encodingName == null || encodingName.isEmpty() || "UNKNOWN".equalsIgnoreCase(encodingName)) {
					encodingName = defaultEncodingName;
				}
				prop.read(confPath, encodingName);
			} else {
				System.err.println("NO SUCH A FILE (ARG -c) : " + confPath);
				isShowUsage = true;
			}
		}

		//----------------------------------------------------------------------
		// プロパティファイルへ引数を上書き設定
		//----------------------------------------------------------------------
		for (String key : argMap.keySet()) {
			prop.setValue(key, argMap.get(key));
		}
		argMap.clear();

		//----------------------------------------------------------------------
		// 引数「--options」で指定された設定リストをさらに上書き設定
		//----------------------------------------------------------------------
		if (optionsStr != null && !optionsStr.isEmpty()) {
			prop.splitMergeProp(optionsStr, optionsDelimiter);
		}

		//----------------------------------------------------------------------
		// USAGEチェック
		//----------------------------------------------------------------------
		if (isShowUsage) {
			showUsage(ClsProperties.LVL_WARN);
		}
		if (isShowSampleConfig) {
			showSampleConf();
		}

		//----------------------------------------------------------------------
		// 開始メッセージ
		//----------------------------------------------------------------------
		if (prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE) > 0) {
			System.out.println("#===<<< [" + CLASS_NAME + "] START : " + prop.convUnixToJst(startTimeMillis, "yyyy/MM/dd HH:mm:ss") + ">>>===");
		}

		//----------------------------------------------------------------------
		// プロパティ値チェック
		//----------------------------------------------------------------------
		String osName = prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName());
		int osId = prop.getOsId(osName);

		String tempPath = prop.getValue(ClsProperties.PATHDOUT, "");
		if (!tempPath.isEmpty()) {
			if (!prop.isExist(tempPath)) {
				System.err.println("No such a directory (ARG -o) : " + tempPath);
				showUsage(ClsProperties.LVL_ERROR);
			}
		}

		tempPath = prop.getValue(ClsProperties.PATHDRHOSTLOG, "");
		if (!tempPath.isEmpty()) {
			if (!prop.isExist(tempPath)) {
				System.err.println("No such a directory (ARG --hl) : " + tempPath);
				showUsage(ClsProperties.LVL_ERROR);
			}
		}

		tempPath = prop.getValue(ClsProperties.PATHFRHOSTLOG_I_IP, "");
		if (!tempPath.isEmpty()) {
			if (!prop.isExist(tempPath)) {
				System.err.println("No such a directory (ARG --i-ip) : " + tempPath);
				showUsage(ClsProperties.LVL_ERROR);
			}
		}

		tempPath = prop.getValue(ClsProperties.PATHFRHOSTLOG_X_IP, "");
		if (!tempPath.isEmpty()) {
			if (!prop.isExist(tempPath)) {
				System.err.println("No such a directory (ARG --x-ip) : " + tempPath);
				showUsage(ClsProperties.LVL_ERROR);
			}
		}

		sleepSec = prop.getValue(ClsProperties.SLEEP_SEC, ClsProperties.DEFAULT_SLEEP_SEC);
		maxLoopCount = prop.getValue(ClsProperties.MAX_LOOP_COUNT, ClsProperties.DEFAULT_MAX_LOOP_COUNT);
		executionTimeSec = prop.getValue(ClsProperties.EXECUTION_TIME_SEC, ClsProperties.DEFAULT_EXECUTION_TIME_SEC);
		if (executionTimeSec > 0 && sleepSec > 0) {
			maxLoopCount = executionTimeSec / sleepSec;
			prop.setValue(ClsProperties.MAX_LOOP_COUNT, maxLoopCount);
		}

		// ホストIPアドレス一覧
		prop.fetchIpAddrs(prop.getHostName());
		String hostIpCsv = prop.getValue(ClsProperties.HOST_IP_ADDR_CSV, "");
		if (!hostIpCsv.isEmpty()) {
			for (String rawIp : hostIpCsv.split(",")) {
				String ip = rawIp.trim();
				if (!ip.isEmpty() && !prop.getIpAddrList().contains(ip)) {
					prop.getIpAddrList().add(ip);
				}
			}
		}
		Collections.sort(prop.getIpAddrList());

		//----------------------------------------------------------------------
		// DEBUG
		//----------------------------------------------------------------------
		if (prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE) > 1) {
			System.out.println("############################################################");
			System.out.println("# PARAMTER");
			System.out.println("############################################################");
			prop.list();
			System.out.println("############################################################");
			System.out.println("# REGEX");
			System.out.println("############################################################");
			System.out.println("# OS NAME   : " + osName);
			if (prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE) > 5) {
				for (int ipCnt = 0; ipCnt < prop.getIpAddrList().size(); ipCnt++) {
					System.out.println("# IP ADDRESS[" + ipCnt + "] : " + prop.getIpAddrList().get(ipCnt));
				}
			}
			if (osId == ClsProperties.OS_SOLARIS) {
				System.out.println("# TCP LSTN REGEX : " + prop.getValue(ClsProperties.TCP_LSTN_REGEX, ClsProperties.SOL_TCP_LSTN_REGEX));
				System.out.println("# UDP LSTN REGEX : " + prop.getValue(ClsProperties.UDP_LSTN_REGEX, ClsProperties.SOL_UDP_LSTN_REGEX));
			}
			System.out.println("# TCP CONN REGEX : " + prop.getTcpConnRegex());
			System.out.println("# UDP CONN REGEX : " + prop.getUdpConnRegex());
			if ("exe".equals(prop.getValue(ClsProperties.MODE, ClsProperties.DEFAULT_MODE))) {
				System.out.println("############################################################");
				System.out.println("CYCLE PARAMS");
				System.out.println("############################################################");
				System.out.println("# SLEEP SEC : " + prop.getValue(ClsProperties.SLEEP_SEC, ClsProperties.DEFAULT_SLEEP_SEC));
				System.out.println("# MAX COUNT : " + prop.getValue(ClsProperties.MAX_LOOP_COUNT, ClsProperties.DEFAULT_MAX_LOOP_COUNT));
				System.out.println("# EXEC TIME : " + prop.getValue(ClsProperties.EXECUTION_TIME_SEC, ClsProperties.DEFAULT_EXECUTION_TIME_SEC));
			}
			System.out.println("############################################################");
		}

		//----------------------------------------------------------------------
		// コマンド設定
		//----------------------------------------------------------------------
		netstat = new ClsNetstat(prop);
		netstat.init();

		//----------------------------------------------------------------------
		// シグナル受信スレッド
		//----------------------------------------------------------------------
		if (isKillJvm) {
			isRunningShutdownHook = true;
			shutdownHookThread = new Thread("NwConnHostInfo-ShutdownHook") {
				@Override
				public void run() {
					System.out.println("START : shutdownHookThread : run()");
					System.out.flush();
					this.interrupt();
					try {
						this.join();
					} catch (InterruptedException ignored) {
						// ignore
					}
					int waitSec = 1;
					try {
						for (int i = 0; i < waitSec; i++) {
							if (waitSec > 1) {
								System.out.println("WAIT : " + i);
							}
							Thread.sleep(1000);
						}
					} catch (InterruptedException ignored) {
						// ignore
					}
					netstat.showList();
					netstat.clear();
					System.out.println("END : shutdownHookThread : run()");
					System.out.flush();
					terminate(ClsProperties.LVL_WARN);
				}
			};
			Runtime.getRuntime().addShutdownHook(shutdownHookThread);
		}

		//----------------------------------------------------------------------
		// 処理
		//----------------------------------------------------------------------
		if ("exe".equals(prop.getValue(ClsProperties.MODE, ClsProperties.DEFAULT_MODE))) {
			for (int i = 0; i < maxLoopCount; i++) {
				long cycleStartTime = System.currentTimeMillis();
				if (Thread.interrupted()) {
					isCancel = true;
				}
				if (isCancel) {
					break;
				}

				if (executionTimeSec > 0 && ((double) (cycleStartTime - startTimeMillis) / 1000.0) >= executionTimeSec) {
					break;
				}

				netstat.execNetstat();
				netstat.getListenPorts();
				netstat.getConnList();
				netstat.setLoopCount(i + 1);

				if (i < maxLoopCount - 1) {
					long cycleElapsTime = System.currentTimeMillis() - cycleStartTime;
					long cycleElapsSec = (long) (Math.floor(cycleElapsTime / 1000.0));
					long cycleElapsMSec = cycleElapsTime - (cycleElapsSec * 1000);
					long intervalSec = ((long) (sleepSec / 2) > cycleElapsSec ? (long) sleepSec - cycleElapsSec : (long) (sleepSec / 2));
					try {
						for (long n = 0; n < intervalSec; n++) {
							if (0 == n) {
								Thread.sleep(1000 - cycleElapsMSec);
							} else {
								Thread.sleep(1000);
							}
						}
					} catch (InterruptedException e) {
						isCancel = true;
						break;
					}
				}
			}
		} else {
			File dir = new File(prop.getValue(ClsProperties.FILE_PARSE_DIR, ClsProperties.DEFAULT_FILE_PARSE_DIR));
			File[] files = dir.listFiles();
			String regex = prop.getValue(ClsProperties.FILE_PARSE_NAME, ClsProperties.DEFAULT_FILE_PARSE_NAME);
			Charset encoding = Charset.forName(prop.getValue(ClsProperties.INPUT_FILE_ENCODING, prop.getDefEncoding(prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName())))));
			if (files != null) {
				for (File file : files) {
					if (file != null && file.getName().matches(regex)) {
						netstat.readFile(file, encoding);
						netstat.getListenPorts();
						netstat.getConnList();
					}
				}
			}
		}

		//----------------------------------------------------------------------
		// 結果出力
		//----------------------------------------------------------------------
		netstat.showList();
		netstat.clear();

		//----------------------------------------------------------------------
		// END
		//----------------------------------------------------------------------
		terminate(returnCode);
	}

	/**
	 * アプリケーションの終了処理を行い、実行経過時間を表示して JVM を終了します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * terminate(0);
	 * </pre>
	 *
	 * @param returnCode プロセス終了コード (0: 正常, 10: 警告, 20: エラー)
	 */
	private void terminate(int returnCode) {
		endTimeMillis = System.currentTimeMillis();
		double elapsedSec = (double) (endTimeMillis - startTimeMillis) / 1000.0;
		if (prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE) > 0) {
			System.out.println("#===<<< [" + CLASS_NAME + "] EXIT (" + returnCode + ") : " + prop.convUnixToJst(endTimeMillis, "yyyy/MM/dd HH:mm:ss") + " : " + elapsedSec + " sec>>>===");
		}
		if (isKillJvm) {
			if (!isCancel) {
				if (isRunningShutdownHook) {
					Runtime.getRuntime().halt(returnCode);
				} else {
					if (shutdownHookThread != null) {
						try {
							Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
						} catch (IllegalStateException ignored) {
							// ignore
						}
					}
					System.exit(returnCode);
				}
			}
		}
	}

	/**
	 * コマンドラインのヘルプ・使用方法 (Usage) メッセージを標準出力へ表示して終了します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * showUsage(ClsProperties.LVL_WARN);
	 * </pre>
	 *
	 * @param returnCode 終了コード
	 */
	private void showUsage(int returnCode) {
		System.out.println("");
		System.out.println("Usage:   java -jar NwConnHostInfo.jar [option...]");
		System.out.println("");
		System.out.println("Basic options:");
		System.out.println("  -conf path                   CONFIG FILE PATH : see --show-sample-config");
		System.out.println("  -o path                      OUTPUT DIRECTORY            (Val = " + prop.getValue(ClsProperties.PATHDOUT, "") + ")");
		System.out.println("  -n name                      OUTPUT FILENAME             (Val = " + prop.getDefaultName() + ")");
		System.out.println("  -m mode                      MODE : exe|file             (Val = " + prop.getValue(ClsProperties.MODE, ClsProperties.DEFAULT_MODE) + ")");
		System.out.println("  --pid                        GET PID MODE                (Val = " + prop.getValue(ClsProperties.IS_PID, false) + ")");
		System.out.println("Remote Host Log options:");
		System.out.println("  --hl path                    REMOTEHOST LOG DIR          (Val = " + prop.getValue(ClsProperties.PATHDRHOSTLOG, "") + ")");
		System.out.println("  --i-ip path                  INC IPLIST FILEPATH         (Val = " + prop.getValue(ClsProperties.PATHFRHOSTLOG_I_IP, "") + ")");
		System.out.println("  --x-ip path                  EXC IPLIST FILEPATH         (Val = " + prop.getValue(ClsProperties.PATHFRHOSTLOG_X_IP, "") + ")");
		System.out.println("Cycle Exec Netstat options:");
		System.out.println("  -c num                       NUMBER OF LOOP COUNT        (Val = " + prop.getValue(ClsProperties.MAX_LOOP_COUNT, ClsProperties.DEFAULT_MAX_LOOP_COUNT) + ")");
		System.out.println("  -et sec                      CYCLE EXECUTION TIME        (Val = " + prop.getValue(ClsProperties.EXECUTION_TIME_SEC, ClsProperties.DEFAULT_EXECUTION_TIME_SEC) + ")");
		System.out.println("  -s sec                       SLEEP SEC PER LOOP          (Val = " + prop.getValue(ClsProperties.SLEEP_SEC, ClsProperties.DEFAULT_SLEEP_SEC) + ")");
		System.out.println("File Parse options:");
		System.out.println("  --dir path                   DIRECTORY PATH              (Val = " + prop.getValue(ClsProperties.FILE_PARSE_DIR, ClsProperties.DEFAULT_FILE_PARSE_DIR) + ")");
		System.out.println("  --file pattern               FILENAME PATTERN            (Val = " + prop.getValue(ClsProperties.FILE_PARSE_NAME, ClsProperties.DEFAULT_FILE_PARSE_NAME) + ")");
		System.out.println("Advanced options:");
		System.out.println("  --cmd command                EXEC COMMAND                (Val = " + prop.getValue(ClsProperties.COMMAND, ClsProperties.DEFAULT_COMMAND) + ")");
		System.out.println("  --arg args                   EXEC COMMAND ARGS           (Val = " + prop.getCommandArgs() + ")");
		System.out.println("  --timeout sec                COMMAND TIMEOUT SEC         (Val = " + prop.getValue(ClsProperties.TIMEOUT, ClsProperties.DEFAULT_TIMEOUT) + ")");
		System.out.println("  --os name                    OS : win|linux|hpux|solaris (Val = " + prop.getOsShortName() + ")");
		System.out.println("  --tcp-regex regex            VAL EXTRACT REGEX           (Val = " + prop.getTcpConnRegex() + ")");
		System.out.println("  --udp-regex regex            VAL EXTRACT REGEX           (Val = " + prop.getUdpConnRegex() + ")");
		System.out.println("  --add-any y|n                ADD HIGH PORT :ANY          (Val = " + prop.getValue(ClsProperties.IS_PORT_ANY, ClsProperties.DEFAULT_IS_PORT_ANY) + ")");
		System.out.println("Encoding options:");
		System.out.println("  --input-encode encode        INPUT  FILE ENCODING        (Val = " + prop.getValue(ClsProperties.INPUT_FILE_ENCODING, prop.getDefEncoding(prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName())))) + ")");
		System.out.println("  --output-encode encode       OUTPUT FILE ENCODING        (Val = " + prop.getValue(ClsProperties.OUTPUT_FILE_ENCODING, prop.getDefEncoding(prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName())))) + ")");
		System.out.println("");
		System.out.println("Help options:");
		System.out.println("  -h                           SHOW THIS HELP MESSAGE");
		System.out.println("  -v|--vv|--vv num             SHOW VERBOSE");
		System.out.println("  --show-sample-config         SHOW SAMPLE CONFIG");
		System.out.println("  --show-ipaddr                SHOW MACHINE IP ADDRESS");
		System.out.println("");
		System.out.println("exit code: NORMAL=0 / WARN=10 / ERROR=20");
		System.out.println("");
		terminate(returnCode);
	}

	/**
	 * 設定ファイルに記述可能な全設定項目のサンプル（説明コメント付き）を標準出力へ表示します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * app.showSampleConf();
	 * </pre>
	 */
	public void showSampleConf() {
		System.out.println("################################################################################");
		System.out.println("# MODE");
		System.out.println("################################################################################");
		System.out.println("# MODE");
		System.out.println("# ---> 引数：-m mode | --options " + ClsProperties.MODE + "= exe|file");
		System.out.println("" + ClsProperties.MODE + " = " + prop.getValue(ClsProperties.MODE, ClsProperties.DEFAULT_MODE));
		System.out.println("# PID");
		System.out.println("# ---> 引数：--pid | --options " + ClsProperties.IS_PID + "= true|false");
		System.out.println("" + ClsProperties.IS_PID + " = " + prop.getValue(ClsProperties.IS_PID, false));
		System.out.println("# HIGH PORT ANY");
		System.out.println("# ---> 引数：--add-any | --options " + ClsProperties.IS_PORT_ANY + "= true|false");
		System.out.println("" + ClsProperties.IS_PORT_ANY + " = " + prop.getValue(ClsProperties.IS_PORT_ANY, ClsProperties.DEFAULT_IS_PORT_ANY));
		System.out.println("################################################################################");
		System.out.println("# サイクル実行");
		System.out.println("################################################################################");
		System.out.println("# ループ回数");
		System.out.println("# ---> 引数：-c num | --options " + ClsProperties.MAX_LOOP_COUNT + "= 整数");
		System.out.println("" + ClsProperties.MAX_LOOP_COUNT + " = " + prop.getValue(ClsProperties.MAX_LOOP_COUNT, ClsProperties.DEFAULT_MAX_LOOP_COUNT));
		System.out.println("# ループ毎の待機時間(秒)");
		System.out.println("# ---> 引数：-s num | --options " + ClsProperties.SLEEP_SEC + "= 整数");
		System.out.println("" + ClsProperties.SLEEP_SEC + " = " + prop.getValue(ClsProperties.SLEEP_SEC, ClsProperties.DEFAULT_SLEEP_SEC));
		System.out.println("################################################################################");
		System.out.println("# ファイルパース");
		System.out.println("################################################################################");
		System.out.println("# 読込対象ディレクトリパス");
		System.out.println("# ---> 引数：--dir path | --options " + ClsProperties.FILE_PARSE_DIR + "= 整数");
		System.out.println("" + ClsProperties.FILE_PARSE_DIR + " = " + prop.getValue(ClsProperties.FILE_PARSE_DIR, ClsProperties.DEFAULT_FILE_PARSE_DIR));
		System.out.println("# 読込対象ファイル名（正規表現）");
		System.out.println("# ---> 引数：--file num | --options " + ClsProperties.FILE_PARSE_NAME + "= 整数");
		System.out.println("" + ClsProperties.FILE_PARSE_NAME + " = " + prop.getValue(ClsProperties.FILE_PARSE_NAME, ClsProperties.DEFAULT_FILE_PARSE_NAME));
		System.out.println("################################################################################");
		System.out.println("# 出力ファイル：集計結果");
		System.out.println("################################################################################");
		System.out.println("# 出力ディレクトリ");
		System.out.println("# ---> 引数：-o path | --out path | --options " + ClsProperties.PATHDOUT + "= パス");
		System.out.println("" + ClsProperties.PATHDOUT + " = " + prop.getValue(ClsProperties.PATHDOUT, ""));
		System.out.println("# 出力ファイル名");
		System.out.println("# ---> 引数：-n name | --name name | --options " + ClsProperties.OUTPUT_FILE_NAME + "= 名前");
		System.out.println("" + ClsProperties.OUTPUT_FILE_NAME + " = " + prop.getDefaultName());
		System.out.println("################################################################################");
		System.out.println("# 出力ファイル：リモートホスト別接続履歴");
		System.out.println("################################################################################");
		System.out.println("# リモートホスト別接続履歴出力ディレクトリ");
		System.out.println("# ---> 引数：--hl path | --options " + ClsProperties.PATHDRHOSTLOG + "= パス");
		System.out.println("" + ClsProperties.PATHDRHOSTLOG + " = " + prop.getValue(ClsProperties.PATHDRHOSTLOG, ""));
		System.out.println("# リモートホスト別接続履歴出力対象IPアドレス定義ファイル");
		System.out.println("# ---> 引数：--i-ip path | --options " + ClsProperties.PATHFRHOSTLOG_I_IP + "= パス");
		System.out.println("" + ClsProperties.PATHFRHOSTLOG_I_IP + " = " + prop.getValue(ClsProperties.PATHFRHOSTLOG_I_IP, ""));
		System.out.println("# リモートホスト別接続履歴出力対象外IPアドレス定義ファイル");
		System.out.println("# ---> 引数：--x-ip path | --options " + ClsProperties.PATHFRHOSTLOG_X_IP + "= パス");
		System.out.println("" + ClsProperties.PATHFRHOSTLOG_X_IP + " = " + prop.getValue(ClsProperties.PATHFRHOSTLOG_X_IP, ""));
		System.out.println("################################################################################");
		System.out.println("# 実行コマンド");
		System.out.println("################################################################################");
		System.out.println("# コマンド");
		System.out.println("" + ClsProperties.COMMAND + " = " + ClsProperties.DEFAULT_COMMAND);
		System.out.println("# コマンド引数");
		System.out.println("" + ClsProperties.ARGUMENT + " = " + prop.getCommandArgs());
		System.out.println("# コマンドタイムアウト(秒)");
		System.out.println("# ---> 引数：-timeout num | --options " + ClsProperties.TIMEOUT + " = 整数");
		System.out.println("" + ClsProperties.TIMEOUT + " = " + prop.getValue(ClsProperties.TIMEOUT, ClsProperties.DEFAULT_TIMEOUT));
		System.out.println("################################################################################");
		System.out.println("# パーサー正規表現");
		System.out.println("################################################################################");
		System.out.println("# OS名");
		System.out.println("# ---> 引数：--os num | --options " + ClsProperties.OS_NAME + "= win|linux|hpux|solaris");
		System.out.println("" + ClsProperties.OS_NAME + " = " + prop.getOsShortName());
		System.out.println("# TCP行抽出正規表現");
		System.out.println("" + ClsProperties.TCP_CONN_REGEX + " = " + prop.getTcpConnRegex());
		System.out.println("# UDP行抽出正規表現");
		System.out.println("" + ClsProperties.UDP_CONN_REGEX + " = " + prop.getUdpConnRegex());
		System.out.println("# Solarisのみ：TCPブロック行判定正規表現");
		System.out.println("" + ClsProperties.TCP_BLOCK_REGEX + " = " + prop.getValue(ClsProperties.TCP_BLOCK_REGEX, ClsProperties.DEFAULT_TCP_BLOCK_REGEX));
		System.out.println("# Solarisのみ：UDPブロック行判定正規表現");
		System.out.println("" + ClsProperties.UDP_BLOCK_REGEX + " = " + prop.getValue(ClsProperties.UDP_BLOCK_REGEX, ClsProperties.DEFAULT_UDP_BLOCK_REGEX));
		System.out.println("# Solarisのみ：SCTPブロック行判定正規表現");
		System.out.println("" + ClsProperties.SCTP_BLOCK_REGEX + " = " + prop.getValue(ClsProperties.SCTP_BLOCK_REGEX, ClsProperties.DEFAULT_SCTP_BLOCK_REGEX));
		System.out.println("# Solarisのみ：TCPリッスン行判定正規表現");
		System.out.println("" + ClsProperties.TCP_LSTN_REGEX + " = " + prop.getValue(ClsProperties.TCP_LSTN_REGEX, ClsProperties.SOL_TCP_LSTN_REGEX));
		System.out.println("# Solarisのみ：UDPリッスン行判定正規表現");
		System.out.println("" + ClsProperties.UDP_LSTN_REGEX + " = " + prop.getValue(ClsProperties.UDP_LSTN_REGEX, ClsProperties.SOL_UDP_LSTN_REGEX));
		System.out.println("################################################################################");
		System.out.println("その他");
		System.out.println("################################################################################");
		System.out.println("# 入力ファイル文字コード");
		System.out.println("# ---> 引数：--input-encode encode | --options " + ClsProperties.INPUT_FILE_ENCODING + "= 文字列");
		System.out.println("" + ClsProperties.INPUT_FILE_ENCODING + " = " + prop.getValue(ClsProperties.INPUT_FILE_ENCODING, prop.getDefEncoding(prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName())))));
		System.out.println("# 出力ファイル文字コード");
		System.out.println("# ---> 引数：--output-encode encode | --options " + ClsProperties.OUTPUT_FILE_ENCODING + "= 文字列");
		System.out.println("" + ClsProperties.OUTPUT_FILE_ENCODING + " = " + prop.getValue(ClsProperties.OUTPUT_FILE_ENCODING, prop.getDefEncoding(prop.getOsId(prop.getValue(ClsProperties.OS_NAME, prop.getOsShortName())))));
		System.out.println("################################################################################");
		System.out.println("デバッグ");
		System.out.println("################################################################################");
		System.out.println("# 冗長ログ出力レベル");
		System.out.println("# ---> 引数：-v | --vv | --vvv | --vv num | --options " + ClsProperties.VERBOSE + " = 整数");
		System.out.println("" + ClsProperties.VERBOSE + " = " + prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE));
		System.out.println("# 冗長ログ出力レベル（起動時確認用）");
		System.out.println("# ---> 引数：--trace | --options " + ClsProperties.IS_TRACE_LOG + " = 整数");
		System.out.println("" + ClsProperties.IS_TRACE_LOG + " = " + prop.getValue(ClsProperties.IS_TRACE_LOG, ClsProperties.DEFAULT_IS_TRACE_LOG));
		System.out.println("# 設定項目がこのファイルに記載のない場合に警告出力フラグ");
		System.out.println("# ---> 引数：--options " + ClsProperties.IS_WARN_IF_KEY_NOT_FOUND + " = true|false");
		System.out.println("" + ClsProperties.IS_WARN_IF_KEY_NOT_FOUND + " = " + prop.getValue(ClsProperties.IS_WARN_IF_KEY_NOT_FOUND, false));
		System.out.println("# ホストIPアドレス");
		System.out.println("# ---> 引数：--my-ip | --options " + ClsProperties.HOST_IP_ADDR_CSV + " = ");
		System.out.println("" + ClsProperties.HOST_IP_ADDR_CSV + " = " + prop.getValue(ClsProperties.HOST_IP_ADDR_CSV, ""));
		System.out.println("################################################################################");
		terminate(ClsProperties.LVL_INFO);
	}

}
