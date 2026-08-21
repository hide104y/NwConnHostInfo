// 2026/08/15 Gemini 3.6 Flash (High) Review & Modified

namespace NwConnHostInfo.Class;

/// <summary>
/// ネットワーク接続に関連するプロセス情報を保持するプロパティクラスです。
/// </summary>
/// <example>
/// <code>
/// var prop = new ClsProp
/// {
///     Pid = 1234,
///     AppName = "sample.exe",
///     AppPath = @"C:\Program Files\Sample\sample.exe"
/// };
/// </code>
/// </example>
public class ClsProp
{
    /// <summary>
    /// <see cref="ClsProp"/> クラスの新しいインスタンスを初期化します。
    /// </summary>
    /// <example>
    /// <code>
    /// var prop = new ClsProp();
    /// </code>
    /// </example>
    public ClsProp()
    {
    }

    /// <summary>
    /// プロセスID（PID）を取得または設定します。
    /// </summary>
    /// <value>プロセスID。デフォルト値は 0 です。</value>
    /// <example>
    /// <code>
    /// var prop = new ClsProp();
    /// prop.Pid = 1234;
    /// </code>
    /// </example>
    public int Pid { get; set; } = 0;

    /// <summary>
    /// アプリケーション名（プロセス名）を取得または設定します。
    /// </summary>
    /// <value>アプリケーション名。デフォルト値は "-" です。</value>
    /// <example>
    /// <code>
    /// var prop = new ClsProp();
    /// prop.AppName = "sample.exe";
    /// </code>
    /// </example>
    public string AppName { get; set; } = "-";

    /// <summary>
    /// アプリケーションの実行ファイルパスを取得または設定します。
    /// </summary>
    /// <value>実行ファイルのフルパス。デフォルト値は "-" です。</value>
    /// <example>
    /// <code>
    /// var prop = new ClsProp();
    /// prop.AppPath = @"C:\Program Files\Sample\sample.exe";
    /// </code>
    /// </example>
    public string AppPath { get; set; } = "-";
}
