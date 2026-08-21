using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using CmnClsLib.Class;
using CmnClsLib.Module;

// 2026/08/15 Gemini 3.6 Flash (High) Review & Modified

namespace NwConnHostInfo.Class;

/// <summary>
/// アプリケーションのコマンドライン引数、動作オプション、および実行時パラメータを管理するクラスです。
/// </summary>
/// <example>
/// <code>
/// var logger = new ClsLogger();
/// var appArg = new ClsAppArg(logger);
/// if (appArg.Parse(args))
/// {
///     appArg.LoadIpAddresses();
///     Console.WriteLine($"OutputDir: {appArg.OutputDir}");
/// }
/// </code>
/// </example>
public partial class ClsAppArg
{
    /// <summary>
    /// 使用方法フラグ: ヘルプなし（通常実行）を表します。
    /// </summary>
    public const int USAGE_NONE = 0;

    /// <summary>
    /// 使用方法フラグ: ヘルプ表示（-h/--help）を表します。
    /// </summary>
    public const int USAGE_USAGE = 1;

    /// <summary>
    /// 使用方法フラグ: IPアドレス一覧表示（--show-ipaddr）を表します。
    /// </summary>
    public const int USAGE_IPADDR = 2;

    private readonly ClsLogger _logger;
    private readonly ClsCmmnArgs _cmmnArgs;
    private bool _isCustomCommandArgs;
    private bool _isCustomTcpRegex;
    private bool _isCustomUdpRegex;

    [GeneratedRegex(@"^\s*#")]
    private static partial Regex CommentRegex();

    [GeneratedRegex(@"^\s*$")]
    private static partial Regex EmptyLineRegex();

    /// <summary>
    /// <see cref="ClsAppArg"/> クラスの新しいインスタンスを初期化します。
    /// </summary>
    /// <param name="logger">ログ出力を行う <see cref="ClsLogger"/> オブジェクト</param>
    /// <example>
    /// <code>
    /// var logger = new ClsLogger();
    /// var appArg = new ClsAppArg(logger);
    /// </code>
    /// </example>
    public ClsAppArg(ClsLogger logger)
    {
        _logger = logger;
        _cmmnArgs = new(_logger);
        _cmmnArgs.GetModuleInfo(Process.GetCurrentProcess().MainModule?.FileName ?? "");
        ExeDir = _cmmnArgs.ExeDir;
        ExeBaseName = _cmmnArgs.ExeBaseName;
        Pid = _cmmnArgs.Pid;
    }

    /// <summary>
    /// 実行ファイル名（拡張子なしのベース名）を取得または設定します。
    /// </summary>
    /// <value>実行ファイルのベース名</value>
    /// <example>
    /// <code>
    /// string exeName = appArg.ExeBaseName;
    /// </code>
    /// </example>
    public string ExeBaseName { get; set; } = "";

    /// <summary>
    /// 実行ファイルの配置ディレクトリパスを取得または設定します。
    /// </summary>
    /// <value>ディレクトリパス</value>
    /// <example>
    /// <code>
    /// string dir = appArg.ExeDir;
    /// </code>
    /// </example>
    public string ExeDir { get; set; } = "";

    /// <summary>
    /// 現在のプロセスIDを取得または設定します。
    /// </summary>
    /// <value>プロセスID</value>
    /// <example>
    /// <code>
    /// int pid = appArg.Pid;
    /// </code>
    /// </example>
    public int Pid { get; set; }

    /// <summary>
    /// 使用方法フラグ（通常実行、ヘルプ表示、IP表示）を取得します。
    /// </summary>
    /// <value>使用方法フラグ値（<see cref="USAGE_NONE"/>, <see cref="USAGE_USAGE"/>, <see cref="USAGE_IPADDR"/>）</value>
    /// <example>
    /// <code>
    /// if (appArg.UsageFlag == ClsAppArg.USAGE_USAGE)
    /// {
    ///     appArg.ShowUsage();
    /// }
    /// </code>
    /// </example>
    public int UsageFlag { get; private set; } = USAGE_NONE;

    /// <summary>
    /// アプリケーションの終了コードを取得または設定します。
    /// </summary>
    /// <value>終了コード（デフォルト: <see cref="MdlConst.LVL_I"/>）</value>
    /// <example>
    /// <code>
    /// appArg.ReturnCode = MdlConst.LVL_W;
    /// </code>
    /// </example>
    public int ReturnCode { get; set; } = MdlConst.LVL_I;

    /// <summary>
    /// 冗長ログ出力レベルを取得または設定します。
    /// </summary>
    /// <value>冗長度レベル（0: 通常, 1以上: 詳細）</value>
    /// <example>
    /// <code>
    /// if (appArg.Verbose > 0)
    /// {
    ///     logger.WriteLine(MdlConst.LVL_NONE, "詳細ログ");
    /// }
    /// </code>
    /// </example>
    public int Verbose { get; set; }

    /// <summary>
    /// 例外発生時にスタックトレースを表示するかどうかを取得または設定します。
    /// </summary>
    /// <value>スタックトレースを表示する場合は <c>true</c>、それ以外は <c>false</c></value>
    /// <example>
    /// <code>
    /// appArg.IsStackTrace = true;
    /// </code>
    /// </example>
    public bool IsStackTrace { get; set; }

    /// <summary>
    /// 実行対象のマシン名を取得または設定します。
    /// </summary>
    /// <value>マシン名（デフォルトは環境のマシン名）</value>
    /// <example>
    /// <code>
    /// string machine = appArg.MachineName;
    /// </code>
    /// </example>
    public string MachineName { get; set; } = Environment.MachineName;

    /// <summary>
    /// 外部コマンド実行時のタイムアウト秒数を取得または設定します。
    /// </summary>
    /// <value>タイムアウト秒数（デフォルト: 30秒）</value>
    /// <example>
    /// <code>
    /// appArg.Timeout = 60;
    /// </code>
    /// </example>
    public int Timeout { get; set; } = 30;

    /// <summary>
    /// プロセスの実行優先度（1〜5）を取得または設定します。
    /// </summary>
    /// <value>優先度数値（デフォルト: 3）</value>
    /// <example>
    /// <code>
    /// appArg.Priority = 2;
    /// </code>
    /// </example>
    public int Priority { get; set; } = 3;

    /// <summary>
    /// 取得したネットワークインターフェース情報の文字列リストを取得または設定します。
    /// </summary>
    /// <value>NIC情報リスト</value>
    /// <example>
    /// <code>
    /// foreach (string info in appArg.NicInfoList)
    /// {
    ///     Console.WriteLine(info);
    /// }
    /// </code>
    /// </example>
    public List<string> NicInfoList { get; set; } = [];

    /// <summary>
    /// リモートホスト接続ログの出力ディレクトリパスを取得または設定します。
    /// </summary>
    /// <value>接続履歴ディレクトリパス</value>
    /// <example>
    /// <code>
    /// appArg.HostLogDir = @"C:\Logs\Host";
    /// </code>
    /// </example>
    public string HostLogDir { get; set; } = "";

    /// <summary>
    /// 接続履歴記録時に絞り込む対象IPアドレスリストファイルのパスを取得または設定します。
    /// </summary>
    /// <value>絞込IP定義ファイルパス</value>
    /// <example>
    /// <code>
    /// appArg.HostLogIncIpListPath = @"C:\Config\inc_ip.txt";
    /// </code>
    /// </example>
    public string HostLogIncIpListPath { get; set; } = "";

    /// <summary>
    /// 接続履歴記録時に除外する対象IPアドレスリストファイルのパスを取得または設定します。
    /// </summary>
    /// <value>除外IP定義ファイルパス</value>
    /// <example>
    /// <code>
    /// appArg.HostLogExcIpListPath = @"C:\Config\exc_ip.txt";
    /// </code>
    /// </example>
    public string HostLogExcIpListPath { get; set; } = "";

    /// <summary>
    /// 出力先ディレクトリパスを取得または設定します。
    /// </summary>
    /// <value>出力ディレクトリパス</value>
    /// <example>
    /// <code>
    /// appArg.OutputDir = @"C:\Output";
    /// </code>
    /// </example>
    public string OutputDir { get; set; } = "";

    /// <summary>
    /// 出力ファイル名を取得または設定します。
    /// </summary>
    /// <value>出力ファイル名</value>
    /// <example>
    /// <code>
    /// appArg.FileName = "custom_log.txt";
    /// </code>
    /// </example>
    public string FileName { get; set; } = "";

    /// <summary>
    /// サイクル実行時の取得間隔・待機時間（秒）を取得または設定します。
    /// </summary>
    /// <value>待機秒数（デフォルト: 0）</value>
    /// <example>
    /// <code>
    /// appArg.SleepSeconds = 30;
    /// </code>
    /// </example>
    public int SleepSeconds { get; set; }

    /// <summary>
    /// サイクル全体の最大実行時間（秒）を取得または設定します。
    /// </summary>
    /// <value>実行時間秒数（デフォルト: 0）</value>
    /// <example>
    /// <code>
    /// appArg.ExecutionTimeSeconds = 3600;
    /// </code>
    /// </example>
    public int ExecutionTimeSeconds { get; set; }

    /// <summary>
    /// サイクル実行時の最大ループ回数を取得または設定します。
    /// </summary>
    /// <value>最大ループ回数（デフォルト: 1）</value>
    /// <example>
    /// <code>
    /// appArg.MaxLoopCount = 100;
    /// </code>
    /// </example>
    public ulong MaxLoopCount { get; set; } = 1;

    /// <summary>
    /// 実行する netstat 外部コマンドのパスまたはコマンド名を取得または設定します。
    /// </summary>
    /// <value>コマンドパス（デフォルト: "netstat"）</value>
    /// <example>
    /// <code>
    /// appArg.CommandPath = "netstat";
    /// </code>
    /// </example>
    public string CommandPath { get; set; } = "netstat";

    /// <summary>
    /// 実行する外部コマンドの引数を取得または設定します。
    /// </summary>
    /// <value>コマンド引数文字列（デフォルト: "-an"）</value>
    /// <example>
    /// <code>
    /// appArg.CommandArgs = "-ano";
    /// </code>
    /// </example>
    public string CommandArgs { get; set; } = "-an";

    /// <summary>
    /// TCP接続情報を抽出するための正規表現パターンを取得または設定します。
    /// </summary>
    /// <value>正規表現パターン文字列</value>
    /// <example>
    /// <code>
    /// string regex = appArg.TcpConnectionRegex;
    /// </code>
    /// </example>
    public string TcpConnectionRegex { get; set; } = @"^\s*TCP\s+(?<LADDR>[^\s]+):(?<LPORT>[^\s:]+)\s+(?<RADDR>[^\s]+):(?<RPORT>[^\s:]+)\s+(?<STATE>\w*)\s*$";

    /// <summary>
    /// UDP接続情報を抽出するための正規表現パターンを取得または設定します。
    /// </summary>
    /// <value>正規表現パターン文字列</value>
    /// <example>
    /// <code>
    /// string regex = appArg.UdpConnectionRegex;
    /// </code>
    /// </example>
    public string UdpConnectionRegex { get; set; } = @"^\s*UDP\s+(?<LADDR>[^\s]+):(?<LPORT>[^\s:]+)\s+(?<RADDR>[^\s]+):(?<RPORT>[^\s:]+)\s*.*$";

    /// <summary>
    /// ファイル出力時等に使用する文字エンコーディングオブジェクトを取得または設定します。
    /// </summary>
    /// <value><see cref="Encoding"/> オブジェクト（デフォルト: Shift_JIS/CP932）</value>
    /// <example>
    /// <code>
    /// appArg.Encoding = Encoding.UTF8;
    /// </code>
    /// </example>
    public Encoding Encoding { get; set; } = Encoding.GetEncoding(932);

    /// <summary>
    /// 文字エンコーディング名を取得または設定します。
    /// </summary>
    /// <value>エンコーディング名（デフォルト: "SJIS"）</value>
    /// <example>
    /// <code>
    /// appArg.EncodingName = "UTF8";
    /// </code>
    /// </example>
    public string EncodingName { get; set; } = "SJIS";

    /// <summary>
    /// PID（プロセスID）を出力するかどうかを取得または設定します。
    /// </summary>
    /// <value>PIDを出力する場合は <c>true</c>、それ以外は <c>false</c></value>
    /// <example>
    /// <code>
    /// appArg.ShowPid = true;
    /// </code>
    /// </example>
    public bool ShowPid { get; set; }

    /// <summary>
    /// ポート番号に「:ANY」を付与するかどうかを取得または設定します。
    /// </summary>
    /// <value>付与する場合は <c>true</c>、それ以外は <c>false</c></value>
    /// <example>
    /// <code>
    /// appArg.IsAddPortAny = false;
    /// </code>
    /// </example>
    public bool IsAddPortAny { get; set; } = true;

    /// <summary>
    /// 認証用ドメイン名を取得または設定します。
    /// </summary>
    /// <value>ドメイン名</value>
    /// <example>
    /// <code>
    /// appArg.DomainName = "CORP";
    /// </code>
    /// </example>
    public string DomainName { get; set; } = "";

    /// <summary>
    /// 認証用ユーザー名を取得または設定します。
    /// </summary>
    /// <value>ユーザー名</value>
    /// <example>
    /// <code>
    /// appArg.Username = @"CORP\user1";
    /// </code>
    /// </example>
    public string Username { get; set; } = "";

    /// <summary>
    /// ドメイン部を除いたユーザー名を取得または設定します。
    /// </summary>
    /// <value>ユーザー名（ドメインなし）</value>
    /// <example>
    /// <code>
    /// string user = appArg.UsernameWithoutDomain;
    /// </code>
    /// </example>
    public string UsernameWithoutDomain { get; set; } = "";

    /// <summary>
    /// 認証用パスワードを取得または設定します。
    /// </summary>
    /// <value>パスワード文字列</value>
    /// <example>
    /// <code>
    /// appArg.Password = "secret";
    /// </code>
    /// </example>
    public string Password { get; set; } = "";

    /// <summary>
    /// 別ユーザー権限での実行を行うかどうかを取得または設定します。
    /// </summary>
    /// <value>別ユーザー実行を行う場合は <c>true</c>、それ以外は <c>false</c></value>
    /// <example>
    /// <code>
    /// bool su = appArg.IsSwitchUser;
    /// </code>
    /// </example>
    public bool IsSwitchUser { get; set; }

    /// <summary>
    /// ログオン実行を行うかどうかを取得または設定します。
    /// </summary>
    /// <value>ログオンを実行する場合は <c>true</c>、それ以外は <c>false</c></value>
    /// <example>
    /// <code>
    /// bool logon = appArg.IsLogon;
    /// </code>
    /// </example>
    public bool IsLogon { get; set; }

    /// <summary>
    /// 認証エラー発生時でも処理を継続するかどうかを取得または設定します。
    /// </summary>
    /// <value>エラーを無視する場合は <c>true</c>、それ以外は <c>false</c></value>
    /// <example>
    /// <code>
    /// bool ignore = appArg.IsLogonAlwaysOk;
    /// </code>
    /// </example>
    public bool IsLogonAlwaysOk { get; set; }

    /// <summary>
    /// コマンドライン引数の配列を解析し、各プロパティに値を設定します。
    /// </summary>
    /// <param name="args">コマンドライン引数の文字列配列</param>
    /// <returns>引数の解析および検証が正常に完了した場合は <c>true</c>、エラーが発生した場合は <c>false</c></returns>
    /// <example>
    /// <code>
    /// var appArg = new ClsAppArg(logger);
    /// bool success = appArg.Parse(new[] { "-o", @"C:\Output", "-s", "30" });
    /// </code>
    /// </example>
    public bool Parse(string[] args)
    {
        Dictionary<string, string> namedArgs = MdlArg.GetNamedArgs(args);
        _cmmnArgs.NamedArgs = namedArgs;
        bool isOk = _cmmnArgs.GetCommonArgs();

        // 共通引数の反映
        UsageFlag = _cmmnArgs.IsUsage ? USAGE_USAGE : USAGE_NONE;
        Verbose = _cmmnArgs.Verbose;
        IsStackTrace = _cmmnArgs.IsStackTrace;
        Timeout = _cmmnArgs.Timeout;

        // デフォルトファイル名設定
        FileName = $"NwConnHostInfo_{MachineName}.{MdlDate.GetFormattedDate("yyyyMMdd.HHmmss")}.txt";

        // 認証情報の反映
        isOk = _cmmnArgs.GetArgsForAuth() && isOk;
        DomainName = _cmmnArgs.DomainName;
        Username = _cmmnArgs.Username;
        UsernameWithoutDomain = _cmmnArgs.UsernameWithoutDomain;
        Password = _cmmnArgs.Password;
        IsLogonAlwaysOk = _cmmnArgs.IsLogonAlwaysOk;
        IsSwitchUser = _cmmnArgs.IsSwitchUser;
        IsLogon = _cmmnArgs.IsLogon;

        // 出力ディレクトリ (-o)
        string outputDirParam = MdlArg.GetFullPath(namedArgs, "o");
        if (!string.IsNullOrEmpty(outputDirParam))
        {
            switch (MdlFile.GetPathType(outputDirParam))
            {
                case MdlFile.PATH_IS_DIRECTORY:
                    OutputDir = outputDirParam;
                    break;
                case MdlFile.PATH_IS_FILE:
                    isOk = false;
                    OutputDir = "";
                    _logger.WriteLine(MdlConst.LVL_E, $"FILE EXIST (NOT A DIRECTORY) : -o {outputDirParam}");
                    break;
                default:
                    if (MdlFile.CreateDirectory(outputDirParam) <= MdlFile.OK_MKDIR_HANTEI)
                    {
                        OutputDir = outputDirParam;
                    }
                    else
                    {
                        isOk = false;
                        OutputDir = "";
                        _logger.WriteLine(MdlConst.LVL_E, $"FAILED TO MKDIR : -o {outputDirParam}");
                    }
                    break;
            }
        }

        // 接続履歴ディレクトリ (--hl)
        string hostLogDirParam = MdlArg.GetFullPath(namedArgs, "hl");
        if (!string.IsNullOrEmpty(hostLogDirParam))
        {
            switch (MdlFile.GetPathType(hostLogDirParam))
            {
                case MdlFile.PATH_IS_DIRECTORY:
                    HostLogDir = hostLogDirParam;
                    break;
                case MdlFile.PATH_IS_FILE:
                    isOk = false;
                    HostLogDir = "";
                    _logger.WriteLine(MdlConst.LVL_E, $"FILE EXIST (NOT A DIRECTORY) : --hl {hostLogDirParam}");
                    break;
                default:
                    if (MdlFile.CreateDirectory(hostLogDirParam) <= MdlFile.OK_MKDIR_HANTEI)
                    {
                        HostLogDir = hostLogDirParam;
                    }
                    else
                    {
                        isOk = false;
                        HostLogDir = "";
                        _logger.WriteLine(MdlConst.LVL_E, $"FAILED TO MKDIR : --hl {hostLogDirParam}");
                    }
                    break;
            }
        }

        // 絞込IP定義ファイル (--i-ip)
        string incIpParam = MdlArg.GetFullPath(namedArgs, "i-ip");
        if (!string.IsNullOrEmpty(incIpParam))
        {
            if (MdlFile.GetPathType(incIpParam) == MdlFile.PATH_IS_FILE)
            {
                HostLogIncIpListPath = incIpParam;
            }
            else
            {
                isOk = false;
                HostLogIncIpListPath = "";
                _logger.WriteLine(MdlConst.LVL_E, $"NO SUCH A FILE : --i-ip {incIpParam}");
            }
        }

        // 除外IP定義ファイル (--x-ip)
        string excIpParam = MdlArg.GetFullPath(namedArgs, "x-ip");
        if (!string.IsNullOrEmpty(excIpParam))
        {
            if (MdlFile.GetPathType(excIpParam) == MdlFile.PATH_IS_FILE)
            {
                HostLogExcIpListPath = excIpParam;
            }
            else
            {
                isOk = false;
                HostLogExcIpListPath = "";
                _logger.WriteLine(MdlConst.LVL_E, $"NO SUCH A FILE : --i-ip {excIpParam}");
            }
        }

        // 出力ファイル名 (-n)
        string fileNameParam = MdlArg.GetValue(namedArgs, "n") ?? "";
        if (!string.IsNullOrEmpty(fileNameParam))
        {
            FileName = fileNameParam;
        }

        // 実行コマンド名 (--cmd)
        string cmdParam = MdlArg.GetValue(namedArgs, "cmd") ?? "";
        if (!string.IsNullOrEmpty(cmdParam))
        {
            CommandPath = cmdParam;
        }

        // 実行コマンド引数 (--arg)
        string argParam = MdlArg.GetValue(namedArgs, "arg") ?? "";
        if (!string.IsNullOrEmpty(argParam))
        {
            _isCustomCommandArgs = true;
            CommandArgs = argParam;
        }

        // TCP正規表現 (--tcp-regex / --tcp-regex regex)
        string tcpRegexParam = MdlArg.GetValue(namedArgs, "tcp-regex") ?? "";
        if (string.IsNullOrEmpty(tcpRegexParam))
        {
            tcpRegexParam = MdlArg.GetValue(namedArgs, "tcp-regex regex") ?? "";
        }
        if (!string.IsNullOrEmpty(tcpRegexParam))
        {
            _isCustomTcpRegex = true;
            TcpConnectionRegex = tcpRegexParam;
        }

        // UDP正規表現 (--udp-regex / --udp-regex regex)
        string udpRegexParam = MdlArg.GetValue(namedArgs, "udp-regex") ?? "";
        if (string.IsNullOrEmpty(udpRegexParam))
        {
            udpRegexParam = MdlArg.GetValue(namedArgs, "udp-regex regex") ?? "";
        }
        if (!string.IsNullOrEmpty(udpRegexParam))
        {
            _isCustomUdpRegex = true;
            UdpConnectionRegex = udpRegexParam;
        }

        // 待機間隔 (-s)
        if (MdlArg.ContainsKey(namedArgs, "s"))
        {
            string val = MdlArg.GetValue(namedArgs, "s");
            int tempInt = MdlUtil.ParseInt(val, MdlConst.INT_NULL);
            if (tempInt != MdlConst.INT_NULL)
            {
                SleepSeconds = tempInt;
            }
        }

        // 取得回数 (-c)
        if (MdlArg.ContainsKey(namedArgs, "c"))
        {
            string val = MdlArg.GetValue(namedArgs, "c");
            int tempInt = MdlUtil.ParseInt(val, MdlConst.INT_NULL);
            if (tempInt != MdlConst.INT_NULL)
            {
                MaxLoopCount = (ulong)tempInt;
            }
        }

        // サイクル実行時間 (-et)
        if (MdlArg.ContainsKey(namedArgs, "et"))
        {
            string val = MdlArg.GetValue(namedArgs, "et");
            int tempInt = MdlUtil.ParseInt(val, MdlConst.INT_NULL);
            if (tempInt != MdlConst.INT_NULL)
            {
                ExecutionTimeSeconds = tempInt;
                if (SleepSeconds > 0)
                {
                    MaxLoopCount = (ulong)(ExecutionTimeSeconds / SleepSeconds);
                }
            }
        }

        // プロセス優先度 (--nice)
        if (MdlArg.ContainsKey(namedArgs, "nice"))
        {
            string val = MdlArg.GetValue(namedArgs, "nice");
            int tempInt = MdlUtil.ParseInt(val, MdlConst.INT_NULL);
            if (tempInt != MdlConst.INT_NULL)
            {
                Priority = Math.Min(tempInt, 5);
            }
        }

        // PIDフラグ (--pid)
        if (MdlArg.ContainsKey(namedArgs, "pid"))
        {
            ShowPid = true;
            if (!_isCustomCommandArgs)
            {
                CommandArgs = "-ano";
            }
            if (!_isCustomTcpRegex)
            {
                TcpConnectionRegex = @"^\s*TCP\s+(?<LADDR>[^\s]+):(?<LPORT>[^\s:]+)\s+(?<RADDR>[^\s]+):(?<RPORT>[^\s:]+)\s+(?<STATE>\w*)\s+(?<PID>\w*)\s*$";
            }
            if (!_isCustomUdpRegex)
            {
                UdpConnectionRegex = @"^\s*UDP\s+(?<LADDR>[^\s]+):(?<LPORT>[^\s:]+)\s+(?<RADDR>[^\s]+):(?<RPORT>[^\s:]+)\s+(?<PID>\w*)\s*$";
            }
        }

        // :ANY付与フラグ (--add-any)
        if (MdlArg.ContainsKey(namedArgs, "add-any"))
        {
            IsAddPortAny = true;
            string addAnyVal = MdlArg.GetValue(namedArgs, "add-any") ?? "";
            if (!string.IsNullOrEmpty(addAnyVal))
            {
                switch (addAnyVal.ToLowerInvariant())
                {
                    case "n" or "0" or "no" or "none" or "off" or "false":
                        IsAddPortAny = false;
                        break;
                }
            }
        }

        // IPアドレス表示フラグ (--show-ipaddr)
        if (MdlArg.ContainsKey(namedArgs, "show-ipaddr"))
        {
            UsageFlag = USAGE_IPADDR;
        }

        // 文字エンコーディング (--enc)
        string encParam = MdlArg.GetValue(namedArgs, "enc") ?? "";
        if (!string.IsNullOrEmpty(encParam))
        {
            Encoding = GetEncoding(encParam);
            EncodingName = NormalizeEncodingName(encParam);
        }

        namedArgs.Clear();
        return isOk;
    }

    /// <summary>
    /// コマンドライン引数の使用方法（ヘルプメッセージ）をログ出力します。
    /// </summary>
    /// <example>
    /// <code>
    /// appArg.ShowUsage();
    /// </code>
    /// </example>
    public void ShowUsage()
    {
        _logger.WriteLine(MdlConst.LVL_NONE, "");
        _logger.WriteLine(MdlConst.LVL_NONE, $"Usage : {ExeBaseName}.exe [Option] [Option]...");
        _logger.WriteLine(MdlConst.LVL_NONE, "");
        _logger.WriteLine(MdlConst.LVL_NONE, "Option:");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   -s sec            ：取得間隔・待機時間(秒)（ex. 30)    (現在値={SleepSeconds}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   -c num            ：取得回数              （ex. 2879)  (現在値={MaxLoopCount}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   -et sec           ：サイクル実行時間(秒)  （ex. 86400) (現在値={ExecutionTimeSeconds}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   -o path           ：出力ディレクトリパス               (現在値={OutputDir})");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   -n name           ：出力ファイル名                     (現在値={FileName})");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --pid             ：PID出力フラグ                      (現在値={ShowPid})");
        _logger.WriteLine(MdlConst.LVL_NONE, "");
        _logger.WriteLine(MdlConst.LVL_NONE, "Remote Host Log options:");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --hl path         ：接続履歴出力ディレクトリパス       (現在値={HostLogDir}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --i-ip path       ：絞込IPアドレス定義ファイルパス     (現在値={HostLogIncIpListPath}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --x-ip path       ：除外IPアドレス定義ファイルパス     (現在値={HostLogExcIpListPath}）");
        _logger.WriteLine(MdlConst.LVL_NONE, "");
        _logger.WriteLine(MdlConst.LVL_NONE, "Advanced options:");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --cmd command     ：実行コマンド                       (現在値={CommandPath}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --arg args        ：実行コマンド引数                   (現在値={CommandArgs}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --timeout sec     ：実行コマンドタイムアウト           (現在値={Timeout}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --tcp-regex regex ：TCP接続情報抽出正規表現            (現在値={TcpConnectionRegex}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --udp-regex regex ：UDP接続情報抽出正規表現            (現在値={UdpConnectionRegex}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --nice num        ：プロセス優先度                     (現在値={Priority}）");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --enc encode      ：ファイル文字コード                （現在値={EncodingName}）");
        _logger.WriteLine(MdlConst.LVL_NONE, "                       ※UTF8|JIS|SJIS|EUC|ASCII|UNICODE|DEFAULT");
        _logger.WriteLine(MdlConst.LVL_NONE, $"   --add-any y|n     ：「:ANY」文字列付与フラグ           (現在値={IsAddPortAny}）");
        _logger.WriteLine(MdlConst.LVL_NONE, "");
        _logger.WriteLine(MdlConst.LVL_NONE, "Help options:");
        _logger.WriteLine(MdlConst.LVL_NONE, "   -h                ：SHOW THIS HELP MESSAGE");
        _logger.WriteLine(MdlConst.LVL_NONE, "   --show-ipaddr     ：SHOW MACHINE IP ADDRESS");
        _logger.WriteLine(MdlConst.LVL_NONE, "");
        _logger.WriteLine(MdlConst.LVL_NONE, $"Return Code : {MdlConst.LVL_I}:SUCCESS / {MdlConst.LVL_W}:WARN / {MdlConst.LVL_E}:ERROR");
        _logger.WriteLine(MdlConst.LVL_NONE, "");
    }

    /// <summary>
    /// 指定されたエンコード名文字列に対応する <see cref="Encoding"/> オブジェクトを取得します。
    /// </summary>
    /// <param name="encodingName">エンコード名（"UTF8", "UTF-8", "UNICODE", "ASCII", "DEFAULT", "JIS", "EUC", "SJIS" 等）</param>
    /// <returns>対応する <see cref="Encoding"/> オブジェクト。認識できない場合は Shift_JIS (CP932)</returns>
    /// <example>
    /// <code>
    /// Encoding enc = appArg.GetEncoding("UTF-8");
    /// </code>
    /// </example>
    public Encoding GetEncoding(string encodingName)
    {
        return encodingName.Trim().ToUpperInvariant() switch
        {
            "UTF8" or "UTF-8" => Encoding.UTF8,
            "UNICODE" => Encoding.Unicode,
            "ASCII" => Encoding.ASCII,
            "DEFAULT" => Encoding.Default,
            "JIS" => Encoding.GetEncoding(50220),
            "EUC" or "EUCJP" or "EUC-JP" => Encoding.GetEncoding(51932),
            _ => Encoding.GetEncoding(932),
        };
    }

    /// <summary>
    /// 指定されたエンコード名文字列を標準化された大文字表記（"UTF8", "SJIS" 等）に正規化します。
    /// </summary>
    /// <param name="encodingName">正規化対象のエンコード名文字列</param>
    /// <returns>標準化されたエンコード名文字列（未知の名前の場合は "SJIS"）</returns>
    /// <example>
    /// <code>
    /// string standardName = appArg.NormalizeEncodingName("utf-8"); // => "UTF-8"
    /// string sjis = appArg.NormalizeEncodingName("unknown"); // => "SJIS"
    /// </code>
    /// </example>
    public string NormalizeEncodingName(string encodingName)
    {
        string standardized = encodingName.Trim().ToUpperInvariant();
        return standardized switch
        {
            "UTF8" or "UTF-8" or "UNICODE" or "ASCII" or "DEFAULT" or "JIS" or "EUC" or "EUCJP" or "EUC-JP" => standardized,
            _ => "SJIS",
        };
    }

    /// <summary>
    /// ローカルマシンのネットワークインターフェース情報を取得し、ホスト名、ドメイン、および各NICのIPアドレス・DNS・ゲートウェイ情報を <see cref="NicInfoList"/> に格納します。
    /// </summary>
    /// <example>
    /// <code>
    /// appArg.LoadIpAddresses();
    /// foreach (var line in appArg.NicInfoList)
    /// {
    ///     Console.WriteLine(line);
    /// }
    /// </code>
    /// </example>
    public void LoadIpAddresses()
    {
        try
        {
            NicInfoList.Clear();
            NicInfoList.Add($"HOSTNAME   = {Dns.GetHostName()}");
            NicInfoList.Add($"USERDOMAIN = {Environment.UserDomainName}");

            foreach (NetworkInterface networkInterface in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (networkInterface.OperationalStatus != OperationalStatus.Up)
                {
                    continue;
                }

                string nicName = networkInterface.Name;
                string nicType = networkInterface.NetworkInterfaceType.ToString();

                if ("Loopback".Equals(nicType, StringComparison.OrdinalIgnoreCase))
                {
                    continue;
                }

                IPInterfaceProperties? ipProperties = networkInterface.GetIPProperties();
                if (ipProperties is null)
                {
                    continue;
                }

                foreach (UnicastIPAddressInformation unicastAddress in ipProperties.UnicastAddresses)
                {
                    if (unicastAddress.Address.AddressFamily == AddressFamily.InterNetwork)
                    {
                        NicInfoList.Add($"[{nicType}][{nicName}] IP Address = {unicastAddress.Address} / {unicastAddress.IPv4Mask}");
                    }
                    else
                    {
                        NicInfoList.Add($"[{nicType}][{nicName}] IP Address = {unicastAddress.Address}");
                    }
                }

                foreach (IPAddress dnsAddress in ipProperties.DnsAddresses)
                {
                    NicInfoList.Add($"[{nicType}][{nicName}] Dns Server = {dnsAddress}");
                }

                foreach (GatewayIPAddressInformation gatewayAddress in ipProperties.GatewayAddresses)
                {
                    NicInfoList.Add($"[{nicType}][{nicName}] Gateway = {gatewayAddress.Address}");
                }
            }
        }
        catch (Exception ex)
        {
            _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
            _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
        }
    }

    /// <summary>
    /// 指定されたパスのテキストファイルを読み込み、コメント行（#で始まる行）および空白・空行を除外した有効な行のリストを返します。
    /// </summary>
    /// <param name="filePath">読み込むテキストファイルの絶対パスまたは相対パス</param>
    /// <param name="encoding">読み込み時に適用する文字エンコーディング</param>
    /// <returns>コメント行・空行を除いた行文字列のリスト。ファイルが存在しない場合やエラー時は空リスト</returns>
    /// <example>
    /// <code>
    /// var lines = appArg.ReadFileLines(@"C:\Config\ip_list.txt", Encoding.UTF8);
    /// </code>
    /// </example>
    public List<string> ReadFileLines(string filePath, Encoding encoding)
    {
        List<string> lines = [];
        try
        {
            using var reader = new StreamReader(filePath, encoding);
            string? line;
            while ((line = reader.ReadLine()) is not null)
            {
                if (CommentRegex().IsMatch(line))
                {
                    continue;
                }
                if (EmptyLineRegex().IsMatch(line))
                {
                    continue;
                }
                lines.Add(line);
            }
        }
        catch (Exception ex)
        {
            _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
            _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
        }
        return lines;
    }

    /// <summary>
    /// 指定されたパスのファイルに文字列コンテンツを書き込みます（末尾に改行を付与）。
    /// </summary>
    /// <param name="filePath">書き込み先のファイルパス</param>
    /// <param name="content">書き込む文字列コンテンツ</param>
    /// <param name="encoding">書き込み時に適用する文字エンコーディング</param>
    /// <param name="append">既存ファイルへ追記する場合は <c>true</c>、上書き（新規作成）する場合は <c>false</c></param>
    /// <example>
    /// <code>
    /// appArg.WriteFile(@"C:\Output\result.txt", "Log content", Encoding.UTF8, append: false);
    /// </code>
    /// </example>
    public void WriteFile(string filePath, string content, Encoding encoding, bool append)
    {
        try
        {
            using var streamWriter = new StreamWriter(filePath, append, encoding);
            streamWriter.WriteLine(content);
        }
        catch (Exception ex)
        {
            _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
            _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
        }
    }
}
