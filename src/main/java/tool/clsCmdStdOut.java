package tool;

import java.util.LinkedList;
import java.util.List;

/**
 * 外部コマンドの標準出力文字列を保持するクラスです。
 */
public final class clsCmdStdOut {

	/** 標準出力行リスト */
	private List<String> stdOutList = new LinkedList<>();

	/**
	 * デフォルトコンストラクタ。
	 *
	 * <pre>
	 * clsCmdStdOut stdOut = new clsCmdStdOut();
	 * </pre>
	 */
	public clsCmdStdOut() {
	}

	/**
	 * 標準出力文字列リストを設定します。
	 *
	 * <pre>
	 * clsCmdStdOut stdOut = new clsCmdStdOut();
	 * List&lt;String&gt; list = Arrays.asList("line1", "line2");
	 * stdOut.setStdOutList(list);
	 * </pre>
	 *
	 * @param list 標準出力リスト
	 */
	public void setStdOutList(final List<String> list) {
		this.stdOutList = (list != null) ? list : new LinkedList<>();
	}

	/**
	 * 標準出力文字列リストを取得します。
	 *
	 * <pre>
	 * clsCmdStdOut stdOut = new clsCmdStdOut();
	 * List&lt;String&gt; list = stdOut.getStdOutList();
	 * </pre>
	 *
	 * @return 標準出力文字列リスト
	 */
	public List<String> getStdOutList() {
		return this.stdOutList;
	}

	/**
	 * 保持している標準出力文字列を全削除します。
	 *
	 * <pre>
	 * clsCmdStdOut stdOut = new clsCmdStdOut();
	 * stdOut.clear();
	 * </pre>
	 */
	public void clear() {
		this.stdOutList.clear();
	}

	/**
	 * 標準出力文字列を1行追加します。
	 *
	 * <pre>
	 * clsCmdStdOut stdOut = new clsCmdStdOut();
	 * stdOut.add("output line");
	 * </pre>
	 *
	 * @param value 追加する文字列
	 */
	public void add(final String value) {
		if (value != null) {
			this.stdOutList.add(value);
		}
	}

}
