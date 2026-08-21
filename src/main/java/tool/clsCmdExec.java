package tool;

import java.io.DataInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * 外部プロセス・OSコマンドを実行し、標準出力を捕捉するクラスです。
 */
public final class clsCmdExec {

	/** クラス名 */
	private static final String CLASS_NAME = "clsCmdExec";

	/** デフォルトウォッチドッグタイムアウト秒数 */
	private static final int DEFAULT_WATCHDOG_SEC = 30;

	/** 標準出力格納オブジェクト */
	private clsCmdStdOut cmdStdOut = null;

	/** ウォッチドッグタイムアウト秒数 */
	private int watchdog = DEFAULT_WATCHDOG_SEC;

	/**
	 * デフォルトコンストラクタ。
	 *
	 * <pre>
	 * clsCmdExec exec = new clsCmdExec();
	 * </pre>
	 */
	public clsCmdExec() {
	}

	/**
	 * 標準出力格納オブジェクトを取得します。
	 *
	 * <pre>
	 * clsCmdExec exec = new clsCmdExec();
	 * clsCmdStdOut out = exec.getCmdStdOut();
	 * </pre>
	 *
	 * @return 標準出力格納オブジェクト
	 */
	public clsCmdStdOut getCmdStdOut() {
		return this.cmdStdOut;
	}

	/**
	 * 標準出力格納オブジェクトを設定します。
	 *
	 * <pre>
	 * clsCmdExec exec = new clsCmdExec();
	 * clsCmdStdOut out = new clsCmdStdOut();
	 * exec.setCmdStdOut(out);
	 * </pre>
	 *
	 * @param cmdStdOut 標準出力格納オブジェクト
	 */
	public void setCmdStdOut(final clsCmdStdOut cmdStdOut) {
		this.cmdStdOut = cmdStdOut;
	}

	/**
	 * コマンド実行のタイムアウト（秒）を設定します。
	 *
	 * <pre>
	 * clsCmdExec exec = new clsCmdExec();
	 * exec.setWatchdog(60);
	 * </pre>
	 *
	 * @param watchdogSec タイムアウト秒数
	 */
	public void setWatchdog(final int watchdogSec) {
		this.watchdog = watchdogSec;
	}

	/**
	 * 指定されたコマンドおよび引数配列を実行し、標準出力を捕捉します。
	 *
	 * <pre>
	 * clsCmdExec exec = new clsCmdExec();
	 * clsCmdStdOut stdOut = new clsCmdStdOut();
	 * exec.setCmdStdOut(stdOut);
	 * int exitCode = exec.execute("netstat", new String[]{"-an"});
	 * </pre>
	 *
	 * @param commandName 実行するコマンド名または実行ファイルパス
	 * @param args コマンドに渡す引数の配列
	 * @return プロセスの終了コード（例外発生時は clsProperties.LVL_ERROR）
	 */
	public int execute(final String commandName, final String[] args) {
		String msgPrefix = "[" + CLASS_NAME + ".execute()] ";
		int returnCode = clsProperties.LVL_ERROR;

		try {
			if (cmdStdOut != null) {
				cmdStdOut.clear();
			}
			CommandLine cmdLine = new CommandLine(commandName);
			if (args != null) {
				for (String element : args) {
					if (element != null && !element.isEmpty()) {
						cmdLine.addArgument(element);
					}
				}
			}

			DefaultExecutor executor = DefaultExecutor.builder().get();

			try (PipedOutputStream pos = new PipedOutputStream();
				 PipedInputStream pis = new PipedInputStream(pos);
				 DataInputStream dis = new DataInputStream(pis)) {

				PumpStreamHandler psHandler = new PumpStreamHandler(pos, pos);
				executor.setStreamHandler(psHandler);

				clsPipeParser pipeParser = new clsPipeParser(dis, cmdStdOut);
				pipeParser.setPriority(Thread.NORM_PRIORITY + 1);
				pipeParser.start();

				if (0 < watchdog) {
					ExecuteWatchdog execWatchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(watchdog)).get();
					executor.setWatchdog(execWatchdog);
				}

				returnCode = executor.execute(cmdLine);
				pipeParser.join();
			}
		} catch (Exception ex) {
			System.err.println(msgPrefix + "Exception " + ex.getMessage());
			ex.printStackTrace();
		}
		return returnCode;
	}

	/**
	 * プロセスの終了・クリーンアップを行います。
	 *
	 * <pre>
	 * clsCmdExec exec = new clsCmdExec();
	 * exec.terminate();
	 * </pre>
	 */
	public void terminate() {
		// 現在処理は特になし
	}

}
