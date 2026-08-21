package tool;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * 外部プロセスおよび OS コマンドを実行し、標準出力を捕捉する実行管理クラスです。
 * <p>
 * Apache Commons Exec を利用してプロセスの起動、パイプストリーム経由での標準出力取得、
 * タイムアウト制御 (Watchdog) を行います。
 * </p>
 *
 * <p>使用例:</p>
 * <pre>
 * ClsCmdExec exec = new ClsCmdExec();
 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
 * exec.setCmdStdOut(stdOut);
 * exec.setWatchdog(30);
 * int exitCode = exec.execute("netstat", new String[]{"-an"});
 * for (String line : stdOut.getStdOutList()) {
 *     System.out.println(line);
 * }
 * </pre>
 */
public class ClsCmdExec {

	/** クラス名（ログ用） */
	private static final String CLASS_NAME = "ClsCmdExec";

	/** デフォルトタイムアウト秒 (30秒) */
	private static final int DEFAULT_WATCHDOG_SEC = 30;

	/** 標準出力格納オブジェクト */
	private ClsCmdStdOut cmdStdOut = null;

	/** コマンド実行タイムアウト秒（デフォルト: 30秒） */
	private int watchdog = DEFAULT_WATCHDOG_SEC;

	/**
	 * デフォルトコンストラクタです。
	 * <p>
	 * タイムアウトをデフォルトの30秒に設定してインスタンスを初期化します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdExec exec = new ClsCmdExec();
	 * </pre>
	 */
	public ClsCmdExec() {
	}

	/**
	 * 現在設定されている標準出力格納オブジェクトを取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdExec exec = new ClsCmdExec();
	 * ClsCmdStdOut out = exec.getCmdStdOut();
	 * </pre>
	 *
	 * @return 標準出力格納オブジェクト（未設定時は null）
	 */
	public ClsCmdStdOut getCmdStdOut() {
		return this.cmdStdOut;
	}

	/**
	 * 標準出力格納オブジェクトを設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdExec exec = new ClsCmdExec();
	 * ClsCmdStdOut out = new ClsCmdStdOut();
	 * exec.setCmdStdOut(out);
	 * </pre>
	 *
	 * @param cmdStdOut 標準出力格納先オブジェクト
	 */
	public void setCmdStdOut(ClsCmdStdOut cmdStdOut) {
		this.cmdStdOut = cmdStdOut;
	}

	/**
	 * コマンド実行のタイムアウト（秒）を設定します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdExec exec = new ClsCmdExec();
	 * exec.setWatchdog(60);
	 * </pre>
	 *
	 * @param watchdogSec タイムアウト秒数（0以下の場合はタイムアウトなし）
	 */
	public void setWatchdog(int watchdogSec) {
		this.watchdog = watchdogSec;
	}

	/**
	 * 指定されたコマンド名および引数配列を実行し、標準出力を捕捉します。
	 * <p>
	 * 内部で {@link DefaultExecutor} および {@link ClsPipeParser} を用いて非同期ストリーム読み取りを行い、
	 * コマンドの終了コードを返却します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdExec exec = new ClsCmdExec();
	 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
	 * exec.setCmdStdOut(stdOut);
	 * int exitCode = exec.execute("netstat", new String[]{"-an"});
	 * if (exitCode == 0) {
	 *     System.out.println("Command succeeded.");
	 * }
	 * </pre>
	 *
	 * @param commandName 実行するコマンド名または実行ファイルパス (例: "netstat")
	 * @param args コマンドに渡す引数文字列配列 (例: new String[]{"-an"})
	 * @return プロセスの終了コード（正常時は 0、例外発生時は {@link ClsProperties#LVL_ERROR}）
	 */
	public int execute(String commandName, String[] args) {
		String msgPrefix = "[" + CLASS_NAME + ".execute()] ";
		int returnCode = ClsProperties.LVL_ERROR;

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

			DefaultExecutor executor = new DefaultExecutor();

			try (PipedOutputStream pos = new PipedOutputStream();
				 PipedInputStream pis = new PipedInputStream(pos);
				 DataInputStream dis = new DataInputStream(pis)) {

				PumpStreamHandler psHandler = new PumpStreamHandler(pos, pos);
				executor.setStreamHandler(psHandler);

				ClsPipeParser pipeParser = new ClsPipeParser(dis, cmdStdOut);
				pipeParser.setPriority(Thread.NORM_PRIORITY + 1);
				pipeParser.start();

				if (watchdog > 0) {
					ExecuteWatchdog execWatchdog = new ExecuteWatchdog(watchdog * 1000L);
					executor.setWatchdog(execWatchdog);
				}

				returnCode = executor.execute(cmdLine);
				pipeParser.join();
			}
		} catch (ExecuteException ee) {
			returnCode = ee.getExitValue();
			System.err.println(msgPrefix + "ExecuteException (exitValue=" + returnCode + "): " + ee.getMessage());
		} catch (IOException | InterruptedException ex) {
			System.err.println(msgPrefix + "Exception " + ex.getMessage());
			ex.printStackTrace();
		} catch (RuntimeException ex) {
			System.err.println(msgPrefix + "RuntimeException " + ex.getMessage());
			ex.printStackTrace();
		}
		return returnCode;
	}

	/**
	 * プロセスの終了・クリーンアップ処理を行います。
	 * <p>
	 * 現在の実装ではリソース解放処理は特になく、将来的な拡張用プレースホルダーです。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdExec exec = new ClsCmdExec();
	 * exec.terminate();
	 * </pre>
	 */
	public void terminate() {
		// 将来拡張用プレースホルダー
	}

}
