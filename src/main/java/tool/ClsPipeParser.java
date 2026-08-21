package tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 外部プロセスのパイプ入力ストリームから非同期に行単位でデータを読み取り、
 * 出力格納用オブジェクトへ格納するスレッドクラスです。
 * <p>
 * コマンド実行時の標準出力・標準エラー出力をブロックすることなくキャプチャするために
 * バックグラウンドスレッドとして動作します。
 * </p>
 *
 * <p>使用例:</p>
 * <pre>
 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
 * InputStream is = process.getInputStream();
 * ClsPipeParser parser = new ClsPipeParser(is, stdOut);
 * parser.start();
 * parser.join();
 * List&lt;String&gt; lines = stdOut.getStdOutList();
 * </pre>
 */
public class ClsPipeParser extends Thread {

	/** クラス名（ログ用） */
	private static final String CLASS_NAME = "ClsPipeParser";

	/** 読み取り対象の入力ストリーム */
	private final InputStream inputStream;

	/** 出力格納先オブジェクト */
	private final ClsCmdStdOut cmdStdOut;

	/**
	 * 入力ストリームと出力格納先オブジェクトを指定してパーサースレッドを初期化します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
	 * ClsPipeParser parser = new ClsPipeParser(inputStream, stdOut);
	 * </pre>
	 *
	 * @param is 読み取り対象のパイプ入力ストリーム
	 * @param cmdStdOut 外部コマンドの標準出力行を格納するオブジェクト
	 */
	public ClsPipeParser(InputStream is, ClsCmdStdOut cmdStdOut) {
		this.inputStream = is;
		this.cmdStdOut = cmdStdOut;
	}

	/**
	 * スレッドのメイン処理を実行します。
	 * <p>
	 * 指定された入力ストリームから UTF-8 エンコーディングで行単位でデータを読み込み、
	 * {@link ClsCmdStdOut} に追加します。ストリームの終端に達するか例外が発生した時点で終了します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * parser.start();
	 * parser.join();
	 * </pre>
	 */
	@Override
	public void run() {
		String msgPrefix = "[" + CLASS_NAME + ".run()] ";
		if (inputStream == null) {
			return;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (cmdStdOut != null) {
					cmdStdOut.add(line);
				}
			}
		} catch (IOException ioex) {
			// プロセス終了に伴うパイプ切断 ("Write end dead" 等) は通常終了とみなして無視
			// ignore
		} catch (RuntimeException ex) {
			System.err.println(msgPrefix + "Exception " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}
