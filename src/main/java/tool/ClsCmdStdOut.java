package tool;

import java.util.LinkedList;
import java.util.List;

/**
 * 外部コマンド実行時の標準出力および標準エラー出力を保持・管理するバッファクラスです。
 * <p>
 * コマンド実行スレッドやパイプパーサーから渡された文字列を行単位のリストとして保持し、
 * 出力内容の追加、参照、クリア等の操作を提供します。
 * </p>
 *
 * <p>使用例:</p>
 * <pre>
 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
 * stdOut.add("TCP 0.0.0.0:80 0.0.0.0:0 LISTENING");
 * for (String line : stdOut.getStdOutList()) {
 *     System.out.println(line);
 * }
 * stdOut.clear();
 * </pre>
 */
public class ClsCmdStdOut {

	/** 標準出力行文字列のリスト */
	private List<String> stdOutList = new LinkedList<>();

	/**
	 * デフォルトコンストラクタです。
	 * <p>
	 * 空の出力リストを初期化します。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
	 * </pre>
	 */
	public ClsCmdStdOut() {
	}

	/**
	 * 保持している標準出力文字列リストを取得します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
	 * List&lt;String&gt; list = stdOut.getStdOutList();
	 * </pre>
	 *
	 * @return 標準出力行文字列のリスト
	 */
	public List<String> getStdOutList() {
		return this.stdOutList;
	}

	/**
	 * 標準出力文字列リストを設定します。
	 * <p>
	 * 引数が null の場合は空の LinkedList が設定されます。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
	 * List&lt;String&gt; list = Arrays.asList("line1", "line2");
	 * stdOut.setStdOutList(list);
	 * </pre>
	 *
	 * @param list 設定する標準出力リスト
	 */
	public void setStdOutList(List<String> list) {
		this.stdOutList = (list != null ? list : new LinkedList<String>());
	}

	/**
	 * 保持している標準出力文字列リストを全件クリア（空に）します。
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
	 * stdOut.add("test line");
	 * stdOut.clear();
	 * </pre>
	 */
	public void clear() {
		this.stdOutList.clear();
	}

	/**
	 * 標準出力文字列を末尾に1行追加します。
	 * <p>
	 * 引数が null の場合は追加されません。
	 * </p>
	 *
	 * <p>使用例:</p>
	 * <pre>
	 * ClsCmdStdOut stdOut = new ClsCmdStdOut();
	 * stdOut.add("Active Connections");
	 * </pre>
	 *
	 * @param value 追加する行文字列
	 */
	public void add(String value) {
		if (value != null) {
			this.stdOutList.add(value);
		}
	}

}
