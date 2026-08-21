package tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * コマンド実行時のパイプ入力を読み込み、標準出力格納オブジェクトへ格納するスレッドクラスです。
 */
public final class clsPipeParser extends Thread {

	/** クラス名 */
	private static final String CLASS_NAME = "clsPipeParser";

	/** パイプ入力ストリーム */
	private final InputStream inputStream;

	/** 標準出力格納オブジェクト */
	private final clsCmdStdOut cmdStdOut;

	/**
	 * パイプ入力ストリームと出力格納オブジェクトを指定してパーサーを初期化します。
	 *
	 * <pre>
	 * clsCmdStdOut stdOut = new clsCmdStdOut();
	 * clsPipeParser parser = new clsPipeParser(inputStream, stdOut);
	 * parser.start();
	 * </pre>
	 *
	 * @param is パイプ入力ストリーム
	 * @param cmdStdOut 外部コマンドの標準出力格納オブジェクト
	 */
	public clsPipeParser(final InputStream is, final clsCmdStdOut cmdStdOut) {
		this.inputStream = is;
		this.cmdStdOut = cmdStdOut;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>スレッドを実行し、入力ストリームから行単位でデータを読み込んで格納します。</p>
	 *
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
			// ignore: "Write end dead" IO exceptions can be ignored
		} catch (Exception ex) {
			System.err.println(msgPrefix + "Exception " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}
