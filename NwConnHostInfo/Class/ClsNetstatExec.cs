using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using CmnClsLib.Class;
using CmnClsLib.Module;

// 2026/08/15 Gemini 3.6 Flash (High) Review & Modified

namespace NwConnHostInfo.Class;

/// <summary>
/// netstat コマンドを実行し、TCP/UDP のリッスンポートおよび確立された接続情報を解析・保持・出力するクラスです。
/// </summary>
/// <example>
/// <code>
/// var logger = new ClsLogger();
/// var netstatExec = new ClsNetstatExec(logger);
/// netstatExec.Initialize();
/// int ret = netstatExec.ExecuteNetstat();
/// if (ret == 0)
/// {
///     netstatExec.GetListenPortList();
///     netstatExec.GetConnectionList();
///     netstatExec.ShowList();
/// }
/// </code>
/// </example>
public class ClsNetstatExec
{
    private readonly ClsLogger _logger;
    private readonly ClsCmdExec _cmdExec;
    private readonly ClsIPUtils _ipUtils = new();
    private readonly List<string> _includeIpv4AddressList = [];
    private readonly List<string> _excludeIpv4AddressList = [];
    private readonly List<string> _includeIpv6AddressList = [];
    private readonly List<string> _excludeIpv6AddressList = [];
    private List<string> _ipv4LoopbackAddressList = [];
    private List<string> _udpListenStateList = [];
    private DateTime _execNetstatTime;
    private bool _isRecordHostLog;
    private bool _isRecordHostLogIncludeIp;
    private bool _isRecordHostLogExcludeIp;

    /// <summary>
    /// <see cref="ClsNetstatExec"/> クラスの新しいインスタンスを初期化します。
    /// </summary>
    /// <param name="logger">ログ出力を行う <see cref="ClsLogger"/> オブジェクト</param>
    /// <example>
    /// <code>
    /// var logger = new ClsLogger();
    /// var exec = new ClsNetstatExec(logger);
    /// </code>
    /// </example>
    public ClsNetstatExec(ClsLogger logger)
    {
        _logger = logger;
        AppArg = new(logger);
        _cmdExec = new(_logger);
    }

    /// <summary>
    /// アプリケーション実行時パラメータを取得または設定します。
    /// </summary>
    /// <value>コマンドライン引数やオプションを保持する <see cref="ClsAppArg"/> オブジェクト</value>
    /// <example>
    /// <code>
    /// exec.AppArg = appArg;
    /// </code>
    /// </example>
    public ClsAppArg AppArg { get; set; }

    /// <summary>
    /// TCPリッスンポート番号のリストを取得または設定します。
    /// </summary>
    /// <value>リッスン中のTCPポート番号一覧</value>
    /// <example>
    /// <code>
    /// List&lt;int&gt; tcpPorts = exec.TcpListenPortList;
    /// </code>
    /// </example>
    public List<int> TcpListenPortList { get; set; } = [];

    /// <summary>
    /// UDPリッスンポート番号のリストを取得または設定します。
    /// </summary>
    /// <value>リッスン中のUDPポート番号一覧</value>
    /// <example>
    /// <code>
    /// List&lt;int&gt; udpPorts = exec.UdpListenPortList;
    /// </code>
    /// </example>
    public List<int> UdpListenPortList { get; set; } = [];

    /// <summary>
    /// TCPリッスンエンドポイント文字列（IP:ポート）のリストを取得または設定します。
    /// </summary>
    /// <value>TCPリッスンエンドポイント一覧</value>
    /// <example>
    /// <code>
    /// List&lt;string&gt; endpoints = exec.TcpListenPortStringList;
    /// </code>
    /// </example>
    public List<string> TcpListenPortStringList { get; set; } = [];

    /// <summary>
    /// UDPリッスンエンドポイント文字列（IP:ポート）のリストを取得または設定します。
    /// </summary>
    /// <value>UDPリッスンエンドポイント一覧</value>
    /// <example>
    /// <code>
    /// List&lt;string&gt; endpoints = exec.UdpListenPortStringList;
    /// </code>
    /// </example>
    public List<string> UdpListenPortStringList { get; set; } = [];

    /// <summary>
    /// 送信元エンドポイントごとの TCP アウトバウンド接続先エンドポイント一覧を保持するディクショナリを取得または設定します。
    /// </summary>
    /// <value>キーがローカルエンドポイント、値がリモートエンドポイント一覧のソート済みディクショナリ</value>
    /// <example>
    /// <code>
    /// var tcpSends = exec.TcpSendDictionary;
    /// </code>
    /// </example>
    public SortedDictionary<string, List<string>> TcpSendDictionary { get; set; } = [];

    /// <summary>
    /// 受信先エンドポイントごとの TCP インバウンド接続元エンドポイント一覧を保持するディクショナリを取得または設定します。
    /// </summary>
    /// <value>キーがローカルエンドポイント、値が接続元エンドポイント一覧のソート済みディクショナリ</value>
    /// <example>
    /// <code>
    /// var tcpRecvs = exec.TcpRecvDictionary;
    /// </code>
    /// </example>
    public SortedDictionary<string, List<string>> TcpRecvDictionary { get; set; } = [];

    /// <summary>
    /// 送信元エンドポイントごとの UDP アウトバウンド接続先エンドポイント一覧を保持するディクショナリを取得または設定します。
    /// </summary>
    /// <value>キーがローカルエンドポイント、値がリモートエンドポイント一覧のソート済みディクショナリ</value>
    /// <example>
    /// <code>
    /// var udpSends = exec.UdpSendDictionary;
    /// </code>
    /// </example>
    public SortedDictionary<string, List<string>> UdpSendDictionary { get; set; } = [];

    /// <summary>
    /// 受信先エンドポイントごとの UDP インバウンド接続元エンドポイント一覧を保持するディクショナリを取得または設定します。
    /// </summary>
    /// <value>キーがローカルエンドポイント、値が接続元エンドポイント一覧のソート済みディクショナリ</value>
    /// <example>
    /// <code>
    /// var udpRecvs = exec.UdpRecvDictionary;
    /// </code>
    /// </example>
    public SortedDictionary<string, List<string>> UdpRecvDictionary { get; set; } = [];

    /// <summary>
    /// エンドポイントに対応する TCP プロセス・アプリケーション情報を保持するディクショナリを取得または設定します。
    /// </summary>
    /// <value>キーがエンドポイント、値が <see cref="ClsProp"/> のソート済みディクショナリ</value>
    /// <example>
    /// <code>
    /// var apps = exec.TcpAppDictionary;
    /// </code>
    /// </example>
    public SortedDictionary<string, ClsProp> TcpAppDictionary { get; set; } = [];

    /// <summary>
    /// エンドポイントに対応する UDP プロセス・アプリケーション情報を保持するディクショナリを取得または設定します。
    /// </summary>
    /// <value>キーがエンドポイント、値が <see cref="ClsProp"/> のソート済みディクショナリ</value>
    /// <example>
    /// <code>
    /// var apps = exec.UdpAppDictionary;
    /// </code>
    /// </example>
    public SortedDictionary<string, ClsProp> UdpAppDictionary { get; set; } = [];

    /// <summary>
    /// コマンド実行結果のリターンコードを取得または設定します。
    /// </summary>
    /// <value>プロセス終了コード（正常時は 0）</value>
    /// <example>
    /// <code>
    /// int code = exec.ReturnCode;
    /// </code>
    /// </example>
    public int ReturnCode { get; set; }

    /// <summary>
    /// 実行ループのサイクルカウントを取得または設定します。
    /// </summary>
    /// <value>現在のサイクル実行回数</value>
    /// <example>
    /// <code>
    /// ulong count = exec.LoopCount;
    /// </code>
    /// </example>
    public ulong LoopCount { get; set; }

    /// <summary>
    /// 監視処理全体の開始日時を取得または設定します。
    /// </summary>
    /// <value>開始日時</value>
    /// <example>
    /// <code>
    /// DateTime start = exec.StartTime;
    /// </code>
    /// </example>
    public DateTime StartTime { get; set; }

    /// <summary>
    /// ログファイル出力時の文字エンコーディングを取得または設定します。
    /// </summary>
    /// <value>文字エンコーディング（デフォルトは Shift_JIS / CP932）</value>
    /// <example>
    /// <code>
    /// exec.Encoding = Encoding.UTF8;
    /// </code>
    /// </example>
    public Encoding Encoding { get; set; } = Encoding.GetEncoding(932);

    /// <summary>
    /// エフェメラルポートなどの動的ポートを ":ANY" 表記に丸め込むかどうかを取得または設定します。
    /// </summary>
    /// <value>ポート番号を ANY に丸める場合は true、それ以外は false。デフォルトは true です。</value>
    /// <example>
    /// <code>
    /// exec.IsAddPortAny = true;
    /// </code>
    /// </example>
    public bool IsAddPortAny { get; set; } = true;

    /// <summary>
    /// ホスト単位の個別ログ出力先ディレクトリパスを取得または設定します。
    /// </summary>
    /// <value>ホストログディレクトリパス</value>
    /// <example>
    /// <code>
    /// exec.HostLogDirectory = @"C:\Logs\Hosts";
    /// </code>
    /// </example>
    public string HostLogDirectory { get; set; } = "";

    /// <summary>
    /// ホストログ記録対象（Include）とする IP アドレス一覧ファイルのパスを取得または設定します。
    /// </summary>
    /// <value>対象 IP 一覧ファイルパス</value>
    /// <example>
    /// <code>
    /// exec.HostLogIncludeIpListPath = @"C:\Config\include_ips.txt";
    /// </code>
    /// </example>
    public string HostLogIncludeIpListPath { get; set; } = "";

    /// <summary>
    /// ホストログ除外対象（Exclude）とする IP アドレス一覧ファイルのパスを取得または設定します。
    /// </summary>
    /// <value>除外 IP 一覧ファイルパス</value>
    /// <example>
    /// <code>
    /// exec.HostLogExcludeIpListPath = @"C:\Config\exclude_ips.txt";
    /// </code>
    /// </example>
    public string HostLogExcludeIpListPath { get; set; } = "";

    /// <summary>
    /// netstat コマンド実行インスタンスおよび設定の初期化を行います。
    /// </summary>
    /// <example>
    /// <code>
    /// exec.Initialize();
    /// </code>
    /// </example>
    public void Initialize()
    {
        _cmdExec.Timeout = AppArg.Timeout;
        _cmdExec.IsShowEmptyLine = false;
        _cmdExec.IsClearStringBuilder = false;
        _cmdExec.IsSu = AppArg.IsSwitchUser;
        _cmdExec.DomainName = AppArg.DomainName;
        _cmdExec.Username = AppArg.UsernameWithoutDomain;
        _cmdExec.Password = AppArg.Password;
        _cmdExec.Verbose = AppArg.Verbose;
        if (AppArg.Verbose > 3)
        {
            _cmdExec.IsShowCmd = true;
            _cmdExec.IsShowExitCode = true;
            _cmdExec.IsShowOutput = true;
        }
        _cmdExec.IsStackTrace = true;
        _cmdExec.Initialize();
        _cmdExec.CmdPath = Environment.GetEnvironmentVariable("ComSpec") ?? "cmd";
        _cmdExec.CmdArgs = $"/c {AppArg.CommandPath} {AppArg.CommandArgs}";
        _ipv4LoopbackAddressList = ["127.0.0.1", "[::1]"];
        _udpListenStateList = ["*:*"];
        if (!string.IsNullOrEmpty(HostLogDirectory)) _isRecordHostLog = true;
        if (!string.IsNullOrEmpty(HostLogIncludeIpListPath)) _isRecordHostLogIncludeIp = true;
        if (!string.IsNullOrEmpty(HostLogExcludeIpListPath)) _isRecordHostLogExcludeIp = true;
    }

    /// <summary>
    /// 指定されたアドレスがループバックアドレス（127.0.0.1 または [::1]）かどうかを判定します。
    /// </summary>
    /// <param name="address">判定対象の IP アドレス文字列</param>
    /// <returns>ループバックアドレスの場合は true、それ以外は false</returns>
    /// <example>
    /// <code>
    /// bool isLoopback = exec.IsLoopbackAddress("127.0.0.1"); // true
    /// </code>
    /// </example>
    public bool IsLoopbackAddress(string address) => _ipv4LoopbackAddressList.Contains(address);

    /// <summary>
    /// 指定された TCP 状態文字列がリッスン状態（LISTENING / LISTEN）かどうかを判定します。
    /// </summary>
    /// <param name="state">TCP 状態文字列（例: "LISTENING"）</param>
    /// <returns>リッスン状態の場合は true、それ以外は false</returns>
    /// <example>
    /// <code>
    /// bool listening = exec.IsTcpListenState("LISTENING"); // true
    /// </code>
    /// </example>
    public bool IsTcpListenState(string state) => state.ToUpperInvariant() switch
    {
        "LISTENING" or "LISTEN" => true,
        _ => false
    };

    /// <summary>
    /// 指定された UDP エンドポイント状態がリッスン状態（*:*）かどうかを判定します。
    /// </summary>
    /// <param name="state">UDP エンドポイント文字列（例: "*:*"）</param>
    /// <returns>リッスン状態の場合は true、それ以外は false</returns>
    /// <example>
    /// <code>
    /// bool listening = exec.IsUdpListenState("*:*"); // true
    /// </code>
    /// </example>
    public bool IsUdpListenState(string state) => _udpListenStateList.Contains(state);

    /// <summary>
    /// 指定された TCP 接続状態が有効なアクティブ接続状態（ESTABLISHED, TIME_WAIT 等）かどうかを判定します。
    /// </summary>
    /// <param name="state">TCP 接続状態文字列</param>
    /// <returns>有効な接続状態の場合は true、それ以外は false</returns>
    /// <example>
    /// <code>
    /// bool established = exec.IsTcpConnectionState("ESTABLISHED"); // true
    /// </code>
    /// </example>
    public bool IsTcpConnectionState(string state) => state.ToUpperInvariant() switch
    {
        "ESTABLISHED" or "FIN_WAIT_1" or "FIN_WAIT_2" or "CLOSE_WAIT" or "CLOSING" or "LAST_ACK" or "TIME_WAIT" => true,
        _ => false
    };

    /// <summary>
    /// 指定された UDP エンドポイントがアクティブな接続状態（*:* 以外の通信中）かどうかを判定します。
    /// </summary>
    /// <param name="state">UDP エンドポイント状態文字列</param>
    /// <returns>通信中の接続状態の場合は true、それ以外（リッスン状態）は false</returns>
    /// <example>
    /// <code>
    /// bool isConn = exec.IsUdpConnectionState("192.168.1.1:53"); // true
    /// </code>
    /// </example>
    public bool IsUdpConnectionState(string state) => !IsUdpListenState(state);

    /// <summary>
    /// 指定された文字列が IPv4 アドレス（プレフィックス長付き含む）かどうかを判定します。
    /// </summary>
    /// <param name="ipAddress">判定対象の IP アドレス文字列（例: "192.168.1.1" または "192.168.1.0/24"）</param>
    /// <returns>IPv4 アドレスの場合は true、IPv6 または無効な場合は false</returns>
    /// <example>
    /// <code>
    /// bool isV4 = exec.IsIPv4Address("192.168.1.1"); // true
    /// </code>
    /// </example>
    public bool IsIPv4Address(string ipAddress)
    {
        if (string.IsNullOrEmpty(ipAddress)) return false;
        return ClsIPUtils.IPV4 == _ipUtils.JudgeIpVersion(ipAddress.Split('/')[0]);
    }

    /// <summary>
    /// ホストログ記録用の Include / Exclude IP アドレス一覧ファイルを読み込み、内部リストを更新します。
    /// </summary>
    /// <example>
    /// <code>
    /// exec.HostLogIncludeIpListPath = @"C:\Config\include_ips.txt";
    /// exec.LoadValidIpAddresses();
    /// </code>
    /// </example>
    public void LoadValidIpAddresses()
    {
        if (_isRecordHostLogIncludeIp)
        {
            _includeIpv4AddressList.Clear();
            _includeIpv6AddressList.Clear();
            if (MdlFile.PathExists(HostLogIncludeIpListPath))
            {
                foreach (string ipAddress in AppArg.ReadFileLines(HostLogIncludeIpListPath, AppArg.Encoding))
                {
                    if (IsIPv4Address(ipAddress))
                    {
                        _includeIpv4AddressList.Add(ipAddress);
                    }
                    else
                    {
                        _includeIpv6AddressList.Add(ipAddress);
                    }
                }
            }
        }

        if (_isRecordHostLogExcludeIp)
        {
            _excludeIpv4AddressList.Clear();
            _excludeIpv6AddressList.Clear();
            if (MdlFile.PathExists(HostLogExcludeIpListPath))
            {
                foreach (string ipAddress in AppArg.ReadFileLines(HostLogExcludeIpListPath, AppArg.Encoding))
                {
                    if (IsIPv4Address(ipAddress))
                    {
                        _excludeIpv4AddressList.Add(ipAddress);
                    }
                    else
                    {
                        _excludeIpv6AddressList.Add(ipAddress);
                    }
                }
            }
        }
    }

    /// <summary>
    /// 指定された IP アドレスが Include / Exclude フィルタ条件を満たし、記録対象として有効かどうかを判定します。
    /// </summary>
    /// <param name="ipAddress">判定対象の IP アドレス</param>
    /// <returns>フィルタを通過し有効な場合は true、除外対象の場合は false</returns>
    /// <example>
    /// <code>
    /// bool isValid = exec.IsValidIpAddress("192.168.1.100");
    /// </code>
    /// </example>
    public bool IsValidIpAddress(string ipAddress)
    {
        bool isValid = true;
        if (_isRecordHostLogIncludeIp)
        {
            isValid = false;
            if (IsIPv4Address(ipAddress))
            {
                foreach (string includedIp in _includeIpv4AddressList)
                {
                    string networkAddress = includedIp.Contains('/') ? includedIp : $"{includedIp}/32";
                    if (_ipUtils.IsIpInNetwork(ipAddress, networkAddress))
                    {
                        isValid = true;
                        break;
                    }
                }
            }
            else
            {
                if (_includeIpv6AddressList.Contains(ipAddress)) isValid = true;
            }
        }

        if (_isRecordHostLogExcludeIp)
        {
            if (IsIPv4Address(ipAddress))
            {
                foreach (string excludedIp in _excludeIpv4AddressList)
                {
                    string networkAddress = excludedIp.Contains('/') ? excludedIp : $"{excludedIp}/32";
                    if (_ipUtils.IsIpInNetwork(ipAddress, networkAddress))
                    {
                        isValid = false;
                        break;
                    }
                }
            }
            else
            {
                if (_excludeIpv6AddressList.Contains(ipAddress)) isValid = false;
            }
        }

        return isValid;
    }

    /// <summary>
    /// netstat コマンドを非同期スレッド経由で実行し、出力をバッファに格納します。
    /// </summary>
    /// <returns>コマンドの終了ステータスコード（正常時は 0）</returns>
    /// <example>
    /// <code>
    /// int retCode = exec.ExecuteNetstat();
    /// </code>
    /// </example>
    public int ExecuteNetstat()
    {
        _cmdExec.ClearStringBuilderWithLock();
        _execNetstatTime = DateTime.Now;
        ReturnCode = _cmdExec.ExecuteThread(AppArg.Priority);
        if (AppArg.Verbose > 2)
        {
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> === {MdlDate.GetFormattedDate(DateTime.Now, "yyyy/MM/dd HH:mm:ss")} / CYCLE : {LoopCount} ===");
            if (AppArg.Verbose > 4)
            {
                _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.ExecuteNetstat() : _cmdExec.StringBuilder.Length = {_cmdExec.StringBuilder.Length}");
            }
        }
        return ReturnCode;
    }

    /// <summary>
    /// netstat 出力格納用バッファを排他ロック下でクリアします。
    /// </summary>
    /// <example>
    /// <code>
    /// exec.ClearStringBuilderWithLock();
    /// </code>
    /// </example>
    public void ClearStringBuilderWithLock()
    {
        _cmdExec.ClearStringBuilderWithLock();
    }

    /// <summary>
    /// netstat の出力文字列を解析し、TCP / UDP のリッスンポートおよび関連プロセス情報を抽出して各リストに格納します。
    /// </summary>
    /// <example>
    /// <code>
    /// exec.GetListenPortList();
    /// </code>
    /// </example>
    public void GetListenPortList()
    {
        if (AppArg.Verbose > 4)
        {
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetListenPortList() : _cmdExec.StringBuilder.Length = {_cmdExec.StringBuilder.Length}");
        }

        foreach (string line in _cmdExec.StringBuilder.ToString().Split([Environment.NewLine], StringSplitOptions.None))
        {
            if (string.IsNullOrEmpty(line)) continue;

            // TCP
            try
            {
                var regex = new Regex(AppArg.TcpConnectionRegex);
                var match = regex.Match(line);
                if (match.Success)
                {
                    if (AppArg.Verbose > 8)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, $"# -> TCP : match.Success : {line}");
                    }
                    string localAddr = MdlUtil.TrimQuotes(match.Groups["LADDR"].Value);
                    string localPortStr = MdlUtil.TrimQuotes(match.Groups["LPORT"].Value);
                    string state = MdlUtil.TrimQuotes(match.Groups["STATE"].Value);
                    int localPortNo = MdlUtil.ParseInt(localPortStr, MdlConst.INT_NULL);

                    if (IsTcpListenState(state))
                    {
                        if (!IsLoopbackAddress(localAddr))
                        {
                            int portNo = localPortNo;
                            string portElement = $"{localAddr}:{localPortStr}";
                            if (MdlConst.INT_NULL != portNo && !TcpListenPortList.Contains(portNo))
                            {
                                TcpListenPortList.Add(portNo);
                            }
                            if (!TcpListenPortStringList.Contains(portElement))
                            {
                                TcpListenPortStringList.Add(portElement);
                                if (AppArg.ShowPid && !TcpAppDictionary.ContainsKey(portElement))
                                {
                                    TcpAppDictionary.Add(portElement, GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value)));
                                }
                            }
                            if (AppArg.Verbose > 6)
                            {
                                if (AppArg.ShowPid && TcpAppDictionary.TryGetValue(portElement, out var appProp))
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> TCP L {localAddr}:{localPortStr} {appProp.Pid}");
                                }
                                else
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> TCP L {localAddr}:{localPortStr}");
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
                _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
            }

            // UDP
            try
            {
                var regex = new Regex(AppArg.UdpConnectionRegex);
                var match = regex.Match(line);
                if (match.Success)
                {
                    if (AppArg.Verbose > 8)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, $"# -> UDP : match.Success : {line}");
                    }
                    string localAddr = MdlUtil.TrimQuotes(match.Groups["LADDR"].Value);
                    string localPortStr = MdlUtil.TrimQuotes(match.Groups["LPORT"].Value);
                    string remoteAddr = MdlUtil.TrimQuotes(match.Groups["RADDR"].Value);
                    string remotePortStr = MdlUtil.TrimQuotes(match.Groups["RPORT"].Value);
                    string remoteEndpoint = $"{remoteAddr}:{remotePortStr}";
                    int localPortNo = MdlUtil.ParseInt(localPortStr, MdlConst.INT_NULL);

                    if (IsUdpListenState(remoteEndpoint.ToUpperInvariant()))
                    {
                        if (!IsLoopbackAddress(localAddr))
                        {
                            int portNo = localPortNo;
                            string portElement = $"{localAddr}:{localPortStr}";
                            if (MdlConst.INT_NULL != portNo && !UdpListenPortList.Contains(portNo))
                            {
                                UdpListenPortList.Add(portNo);
                            }
                            if (!UdpListenPortStringList.Contains(portElement))
                            {
                                UdpListenPortStringList.Add(portElement);
                                if (AppArg.ShowPid && !UdpAppDictionary.ContainsKey(portElement))
                                {
                                    UdpAppDictionary.Add(portElement, GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value)));
                                }
                            }
                            if (AppArg.Verbose > 6)
                            {
                                if (AppArg.ShowPid && UdpAppDictionary.TryGetValue(portElement, out var appProp))
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> UDP L {localAddr}:{localPortStr} {appProp.Pid}");
                                }
                                else
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> UDP L {localAddr}:{localPortStr}");
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
                _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
            }
        }

        TcpListenPortList.Sort();
        TcpListenPortStringList.Sort();
        UdpListenPortList.Sort();
        UdpListenPortStringList.Sort();

        if (AppArg.Verbose > 5)
        {
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetListenPortList() : _tcpListenPortList.Count = {TcpListenPortList.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetListenPortList() : _tcpListenPortStringList.Count = {TcpListenPortStringList.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetListenPortList() : _udpListenPortList.Count = {UdpListenPortList.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetListenPortList() : _udpListenPortStringList.Count = {UdpListenPortStringList.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetListenPortList() : _cmdExec.StringBuilder.Length = {_cmdExec.StringBuilder.Length}");
        }
    }

    /// <summary>
    /// netstat の出力文字列を解析し、インバウンド／アウトバウンドの TCP / UDP 接続情報を抽出してディクショナリに格納します。
    /// </summary>
    /// <example>
    /// <code>
    /// exec.GetConnectionList();
    /// </code>
    /// </example>
    public void GetConnectionList()
    {
        if (AppArg.Verbose > 4)
        {
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetConnectionList() : _cmdExec.StringBuilder.Length = {_cmdExec.StringBuilder.Length}");
        }
        if (_isRecordHostLog) LoadValidIpAddresses();

        foreach (string line in _cmdExec.StringBuilder.ToString().Split([Environment.NewLine], StringSplitOptions.None))
        {
            if (string.IsNullOrEmpty(line)) continue;

            // TCP
            try
            {
                var regex = new Regex(AppArg.TcpConnectionRegex);
                var match = regex.Match(line);
                if (match.Success)
                {
                    string localAddr = MdlUtil.TrimQuotes(match.Groups["LADDR"].Value);
                    string localPortStr = MdlUtil.TrimQuotes(match.Groups["LPORT"].Value);
                    string remoteAddr = MdlUtil.TrimQuotes(match.Groups["RADDR"].Value);
                    string remotePortStr = MdlUtil.TrimQuotes(match.Groups["RPORT"].Value);
                    string state = MdlUtil.TrimQuotes(match.Groups["STATE"].Value);
                    int localPortNo = MdlUtil.ParseInt(localPortStr, 0);
                    int remotePortNo = MdlUtil.ParseInt(remotePortStr, 0);
                    string localEndpoint = $"{localAddr}:{localPortStr}";
                    string remoteEndpoint = $"{remoteAddr}:{remotePortStr}";

                    if (IsTcpConnectionState(state))
                    {
                        if (!IsLoopbackAddress(localAddr) && !localAddr.Equals(remoteAddr, StringComparison.OrdinalIgnoreCase))
                        {
                            if (TcpListenPortList.Contains(localPortNo))
                            {
                                string remoteValue = remoteAddr;
                                if (IsAddPortAny)
                                {
                                    remoteValue = (remotePortNo > 0 && remotePortNo < 1024)
                                        ? $"{remoteAddr}:{remotePortStr}"
                                        : $"{remoteAddr}:ANY";
                                }

                                if (!TcpRecvDictionary.TryGetValue(localEndpoint, out var recvList))
                                {
                                    recvList = [];
                                    TcpRecvDictionary.Add(localEndpoint, recvList);
                                }

                                if (!recvList.Contains(remoteValue))
                                {
                                    recvList.Add(remoteValue);
                                    if (AppArg.ShowPid && !TcpAppDictionary.ContainsKey(localEndpoint))
                                    {
                                        TcpAppDictionary.Add(localEndpoint, GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value)));
                                    }
                                }
                                else if (AppArg.ShowPid && TcpAppDictionary.TryGetValue(localEndpoint, out var appProp) && appProp.Pid == 0)
                                {
                                    TcpAppDictionary[localEndpoint] = GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value));
                                }

                                if (AppArg.Verbose > 3)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> TCP I {remoteValue}({remotePortStr}) => {localAddr}:{localPortStr}");
                                }

                                if (_isRecordHostLog && IsValidIpAddress(remoteAddr))
                                {
                                    string logPath = Path.Combine(HostLogDirectory, $"{remoteAddr}_{MdlDate.GetFormattedDate(_execNetstatTime, "yyyyMMdd")}.log")
                                        .Replace("[", "").Replace("]", "");
                                    string message = $"{MdlDate.GetFormattedDate(_execNetstatTime, "yyyy/MM/dd HH:mm:ss")} TCP I {line}";
                                    AppArg.WriteFile(logPath, message, AppArg.Encoding, true);
                                }
                            }
                            else
                            {
                                string localValue = localAddr;
                                if (IsAddPortAny)
                                {
                                    localValue = (localPortNo > 0 && (localPortNo < 1024 || TcpListenPortList.Contains(localPortNo)))
                                        ? $"{localAddr}:{localPortStr}"
                                        : $"{localAddr}:ANY";
                                }

                                if (!TcpSendDictionary.TryGetValue(localValue, out var sendList))
                                {
                                    sendList = [];
                                    TcpSendDictionary.Add(localValue, sendList);
                                }

                                if (!sendList.Contains(remoteEndpoint))
                                {
                                    sendList.Add(remoteEndpoint);
                                    if (AppArg.ShowPid && !TcpAppDictionary.ContainsKey(remoteEndpoint))
                                    {
                                        TcpAppDictionary.Add(remoteEndpoint, GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value)));
                                    }
                                }
                                else if (AppArg.ShowPid && TcpAppDictionary.TryGetValue(remoteEndpoint, out var appProp) && appProp.Pid == 0)
                                {
                                    TcpAppDictionary[remoteEndpoint] = GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value));
                                }

                                if (AppArg.Verbose > 3)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> TCP O {localValue}({localPortStr}) => {remoteAddr}:{remotePortStr}");
                                }

                                if (_isRecordHostLog && IsValidIpAddress(remoteAddr))
                                {
                                    string logPath = Path.Combine(HostLogDirectory, $"{remoteAddr}_{MdlDate.GetFormattedDate(_execNetstatTime, "yyyyMMdd")}.log")
                                        .Replace("[", "").Replace("]", "");
                                    string message = $"{MdlDate.GetFormattedDate(_execNetstatTime, "yyyy/MM/dd HH:mm:ss")} TCP O {line}";
                                    AppArg.WriteFile(logPath, message, AppArg.Encoding, true);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
                _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
            }

            // UDP
            try
            {
                var regex = new Regex(AppArg.UdpConnectionRegex);
                var match = regex.Match(line);
                if (match.Success)
                {
                    string localAddr = MdlUtil.TrimQuotes(match.Groups["LADDR"].Value);
                    string localPortStr = MdlUtil.TrimQuotes(match.Groups["LPORT"].Value);
                    string remoteAddr = MdlUtil.TrimQuotes(match.Groups["RADDR"].Value);
                    string remotePortStr = MdlUtil.TrimQuotes(match.Groups["RPORT"].Value);
                    int localPortNo = MdlUtil.ParseInt(localPortStr, 0);
                    int remotePortNo = MdlUtil.ParseInt(remotePortStr, 0);
                    string localEndpoint = $"{localAddr}:{localPortStr}";
                    string remoteEndpoint = $"{remoteAddr}:{remotePortStr}";

                    if (IsUdpConnectionState(remoteEndpoint))
                    {
                        if (!IsLoopbackAddress(localAddr) && !localAddr.Equals(remoteAddr, StringComparison.OrdinalIgnoreCase))
                        {
                            if (UdpListenPortList.Contains(localPortNo))
                            {
                                string remoteValue = remoteAddr;
                                if (IsAddPortAny)
                                {
                                    remoteValue = (remotePortNo > 0 && remotePortNo < 1024)
                                        ? $"{remoteAddr}:{remotePortStr}"
                                        : $"{remoteAddr}:ANY";
                                }

                                if (!UdpRecvDictionary.TryGetValue(localEndpoint, out var recvList))
                                {
                                    recvList = [];
                                    UdpRecvDictionary.Add(localEndpoint, recvList);
                                }

                                if (!recvList.Contains(remoteValue))
                                {
                                    recvList.Add(remoteValue);
                                    if (AppArg.ShowPid && !UdpAppDictionary.ContainsKey(localEndpoint))
                                    {
                                        UdpAppDictionary.Add(localEndpoint, GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value)));
                                    }
                                }
                                else if (AppArg.ShowPid && UdpAppDictionary.TryGetValue(localEndpoint, out var appProp) && appProp.Pid == 0)
                                {
                                    UdpAppDictionary[localEndpoint] = GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value));
                                }

                                if (AppArg.Verbose > 3)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> UDP I {remoteValue}({remotePortStr}) => {localAddr}:{localPortStr}");
                                }

                                if (_isRecordHostLog && IsValidIpAddress(remoteAddr))
                                {
                                    string logPath = Path.Combine(HostLogDirectory, $"{remoteAddr}_{MdlDate.GetFormattedDate(_execNetstatTime, "yyyyMMdd")}.log")
                                        .Replace("[", "").Replace("]", "");
                                    string message = $"{MdlDate.GetFormattedDate(_execNetstatTime, "yyyy/MM/dd HH:mm:ss")} UDP I {line}";
                                    AppArg.WriteFile(logPath, message, AppArg.Encoding, true);
                                }
                            }
                            else
                            {
                                string localValue = localAddr;
                                if (IsAddPortAny)
                                {
                                    localValue = (localPortNo > 0 && (localPortNo < 1024 || UdpListenPortList.Contains(localPortNo)))
                                        ? $"{localAddr}:{localPortStr}"
                                        : $"{localAddr}:ANY";
                                }

                                if (!UdpSendDictionary.TryGetValue(localValue, out var sendList))
                                {
                                    sendList = [];
                                    UdpSendDictionary.Add(localValue, sendList);
                                }

                                if (!sendList.Contains(remoteEndpoint))
                                {
                                    sendList.Add(remoteEndpoint);
                                    if (AppArg.ShowPid && !UdpAppDictionary.ContainsKey(remoteEndpoint))
                                    {
                                        UdpAppDictionary.Add(remoteEndpoint, GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value)));
                                    }
                                }
                                else if (AppArg.ShowPid && UdpAppDictionary.TryGetValue(remoteEndpoint, out var appProp) && appProp.Pid == 0)
                                {
                                    UdpAppDictionary[remoteEndpoint] = GetAppProperties(MdlUtil.TrimQuotes(match.Groups["PID"].Value));
                                }

                                if (AppArg.Verbose > 3)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, $"# -> UDP O {localValue}({localPortStr}) => {remoteAddr}:{remotePortStr}");
                                }

                                if (_isRecordHostLog && IsValidIpAddress(remoteAddr))
                                {
                                    string logPath = Path.Combine(HostLogDirectory, $"{remoteAddr}_{MdlDate.GetFormattedDate(_execNetstatTime, "yyyyMMdd")}.log")
                                        .Replace("[", "").Replace("]", "");
                                    string message = $"{MdlDate.GetFormattedDate(_execNetstatTime, "yyyy/MM/dd HH:mm:ss")} UDP O {line}";
                                    AppArg.WriteFile(logPath, message, AppArg.Encoding, true);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
                _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
            }
        }

        if (AppArg.Verbose > 5)
        {
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetConnectionList() : _tcpRecvDictionary.Count = {TcpRecvDictionary.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetConnectionList() : _tcpSendDictionary.Count = {TcpSendDictionary.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetConnectionList() : _udpRecvDictionary.Count = {UdpRecvDictionary.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetConnectionList() : _udpSendDictionary.Count = {UdpSendDictionary.Count}");
            _logger.WriteLine(MdlConst.LVL_NONE, $"# -> ClsNetstatExec.GetConnectionList() : _cmdExec.StringBuilder.Length = {_cmdExec.StringBuilder.Length}");
        }
    }

    /// <summary>
    /// 収集したネットワーク情報（NIC、リッスンポート、インバウンド・アウトバウンド接続、プロセス情報）をログおよび指定ファイルに出力・表示します。
    /// </summary>
    /// <example>
    /// <code>
    /// exec.ShowList();
    /// </code>
    /// </example>
    public void ShowList()
    {
        StreamWriter? sw = null;
        bool isFileOut = false;
        try
        {
            DateTime endTime = DateTime.Now;
            double elapsedSeconds = (endTime - StartTime).TotalSeconds;

            if (!string.IsNullOrEmpty(AppArg.OutputDir))
            {
                isFileOut = true;
            }

            if (isFileOut)
            {
                try
                {
                    string filePath = Path.Combine(AppArg.OutputDir, AppArg.FileName);
                    sw = new StreamWriter(filePath, false, Encoding);
                }
                catch (Exception ex)
                {
                    isFileOut = false;
                    _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
                }
            }

            void WriteLog(string text, bool verboseOnly = false)
            {
                if (!verboseOnly || AppArg.Verbose > 0)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, text);
                }
                if (isFileOut && sw != null)
                {
                    sw.WriteLine(text);
                }
            }

            void WriteHeader(string title)
            {
                WriteLog("############################################################", true);
                WriteLog($"#■{title}", true);
                WriteLog("############################################################", true);
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteHeader("INFO");
                WriteLog($"# START       : {MdlDate.GetFormattedDate(StartTime, "yyyy/MM/dd HH:mm:ss")}", true);
                WriteLog($"# E N D       : {MdlDate.GetFormattedDate(endTime, "yyyy/MM/dd HH:mm:ss")} : {elapsedSeconds:F3} sec", true);
                WriteLog($"# CYCLE COUNT : {LoopCount} / {AppArg.MaxLoopCount}", true);
                WriteLog($"# PARAM       : ARG EXETIME : {AppArg.ExecutionTimeSeconds} / SLEEP SEC : {AppArg.SleepSeconds}", true);
            }

            foreach (string element in AppArg.NicInfoList)
            {
                WriteLog($"# NIC {element}");
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteHeader("TCP LISTEN PORT LIST");
            }
            foreach (string element in TcpListenPortStringList)
            {
                string buffer = $"TCP L {element}";
                if (AppArg.ShowPid && TcpAppDictionary.TryGetValue(element, out var appProp))
                {
                    buffer += $" {appProp.Pid} {appProp.AppName} {appProp.AppPath}";
                }
                WriteLog(buffer);
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteHeader("UDP LISTEN PORT LIST");
            }
            foreach (string element in UdpListenPortStringList)
            {
                string buffer = $"UDP L {element}";
                if (AppArg.ShowPid && UdpAppDictionary.TryGetValue(element, out var appProp))
                {
                    buffer += $" {appProp.Pid} {appProp.AppName} {appProp.AppPath}";
                }
                WriteLog(buffer);
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteHeader("TCP INBOUND CONNECTION");
            }
            foreach (var (key, value) in TcpRecvDictionary)
            {
                value.Sort();
                foreach (string element in value)
                {
                    string buffer = $"TCP I {element} => {key}";
                    if (AppArg.ShowPid && TcpAppDictionary.TryGetValue(key, out var appProp))
                    {
                        buffer += $" {appProp.Pid} {appProp.AppName} {appProp.AppPath}";
                    }
                    WriteLog(buffer);
                }
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteHeader("TCP OUTBOUND CONNECTION");
            }
            foreach (var (key, value) in TcpSendDictionary)
            {
                value.Sort();
                foreach (string element in value)
                {
                    string buffer = $"TCP O {key} => {element}";
                    if (AppArg.ShowPid && TcpAppDictionary.TryGetValue(element, out var appProp))
                    {
                        buffer += $" {appProp.Pid} {appProp.AppName} {appProp.AppPath}";
                    }
                    WriteLog(buffer);
                }
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteHeader("UDP INBOUND CONNECTION");
            }
            foreach (var (key, value) in UdpRecvDictionary)
            {
                value.Sort();
                foreach (string element in value)
                {
                    string buffer = $"UDP I {element} => {key}";
                    if (AppArg.ShowPid && UdpAppDictionary.TryGetValue(key, out var appProp))
                    {
                        buffer += $" {appProp.Pid} {appProp.AppName} {appProp.AppPath}";
                    }
                    WriteLog(buffer);
                }
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteHeader("UDP OUTBOUND CONNECTION");
            }
            foreach (var (key, value) in UdpSendDictionary)
            {
                value.Sort();
                foreach (string element in value)
                {
                    string buffer = $"UDP O {key} => {element}";
                    if (AppArg.ShowPid && UdpAppDictionary.TryGetValue(element, out var appProp))
                    {
                        buffer += $" {appProp.Pid} {appProp.AppName} {appProp.AppPath}";
                    }
                    WriteLog(buffer);
                }
            }

            if (AppArg.Verbose > 0 || isFileOut)
            {
                WriteLog("############################################################", true);
            }
        }
        catch (Exception ex)
        {
            _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
            _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
        }
        finally
        {
            sw?.Close();
            sw?.Dispose();
        }
    }

    /// <summary>
    /// プロセス ID（文字列）からプロセス情報を取得し、<see cref="ClsProp"/> オブジェクトとして返します。
    /// </summary>
    /// <param name="processId">解析対象のプロセス ID 文字列</param>
    /// <returns>プロセス情報（PID、プロセス名、実行ファイルパス）を格納した <see cref="ClsProp"/> オブジェクト</returns>
    /// <example>
    /// <code>
    /// ClsProp prop = exec.GetAppProperties("1234");
    /// Console.WriteLine($"Name: {prop.AppName}");
    /// </code>
    /// </example>
    public ClsProp GetAppProperties(string processId)
    {
        var appProp = new ClsProp
        {
            Pid = MdlUtil.ParseInt(processId, 0)
        };
        if (appProp.Pid > 0)
        {
            try
            {
                using var process = Process.GetProcessById(appProp.Pid);
                appProp.AppName = process.ProcessName;
                try
                {
                    appProp.AppPath = process.MainModule?.FileName ?? "-";
                }
                catch
                {
                    // プロセス権限等でパス取得不可の場合はデフォルト値
                }
            }
            catch
            {
                // プロセスが既に終了している場合等
            }
        }
        return appProp;
    }
}
