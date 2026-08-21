using System;
using System.Threading;
using CmnClsLib.Module;
using CmnClsLib.Class;
using NwConnHostInfo.Class;

// 2026/08/15 Gemini 3.6 Flash (High) Review & Modified

namespace NwConnHostInfo;

public class Program
{
    private bool _isCancelled = false;
    private readonly ClsLogger _logger;
    private readonly ClsAppArg _AppArg;
    private readonly ClsNetstatExec _netstatExecutor;
    private readonly DateTime _startTime;

    /// <summary>
    /// キャンセルフラグを取得または設定します。
    /// </summary>
    /// <value>キャンセル要求が発生している場合は <c>true</c>、それ以外は <c>false</c>。</value>
    /// <example>
    /// <code>
    /// var program = new Program();
    /// program.IsCancelled = true;
    /// </code>
    /// </example>
    public bool IsCancelled
    {
        get => _isCancelled;
        set => _isCancelled = value;
    }

    /// <summary>
    /// <see cref="Program"/> クラスの新しいインスタンスを初期化します。
    /// </summary>
    /// <example>
    /// <code>
    /// var program = new Program();
    /// program.Run();
    /// </code>
    /// </example>
    public Program()
    {
        _logger = new ClsLogger();
        _AppArg = new ClsAppArg(_logger);
        _netstatExecutor = new ClsNetstatExec(_logger);
        _startTime = DateTime.Now;
    }

    /// <summary>
    /// 依存オブジェクトを指定して <see cref="Program"/> クラスの新しいインスタンスを初期化します。
    /// </summary>
    /// <param name="logger">ログ出力を行う <see cref="ClsLogger"/> オブジェクト。</param>
    /// <param name="AppArg">コマンドライン引数を保持する <see cref="ClsAppArg"/> オブジェクト。</param>
    /// <param name="netstatExecutor">Netstatコマンドの実行・解析を行う <see cref="ClsNetstatExec"/> オブジェクト。</param>
    /// <param name="startTime">プログラムの開始日時。</param>
    /// <example>
    /// <code>
    /// var logger = new ClsLogger();
    /// var appArg = new ClsAppArg(logger);
    /// var netstatExec = new ClsNetstatExec(logger);
    /// var program = new Program(logger, appArg, netstatExec, DateTime.Now);
    /// </code>
    /// </example>
    public Program(ClsLogger logger, ClsAppArg AppArg, ClsNetstatExec netstatExecutor, DateTime startTime)
    {
        _logger = logger;
        _AppArg = AppArg;
        _netstatExecutor = netstatExecutor;
        _startTime = startTime;
    }

    /// <summary>
    /// プログラムのエントリーポイントです。コマンドライン引数を解析し、ネットワーク接続監視処理を実行します。
    /// </summary>
    /// <param name="args">コマンドライン引数の配列。</param>
    /// <returns>終了コード。正常終了時は 0、使用方法表示・IP表示時は 1、引数エラー等の異常終了時は 2。</returns>
    /// <example>
    /// <code>
    /// string[] args = ["--sleep", "5", "--count", "10"];
    /// int exitCode = Program.Main(args);
    /// </code>
    /// </example>
    public static int Main(string[] args)
    {
        DateTime startTime = DateTime.Now;
        ClsLogger logger = new();
        ClsAppArg AppArg = new(logger);
        ClsNetstatExec netstatExecutor = new(logger);

        bool isSuccess = AppArg.Parse(args);

        if (AppArg.Verbose > 0)
        {
            logger.WriteLine(MdlConst.LVL_NONE, $"===<<< [{AppArg.ExeBaseName}] START : {MdlDate.GetFormattedDate(startTime, "yyyy/MM/dd HH:mm:ss")}>>>===");
        }

        AppArg.LoadIpAddresses();

        if (isSuccess && AppArg.UsageFlag == ClsAppArg.USAGE_NONE)
        {
            netstatExecutor.AppArg = AppArg;
            netstatExecutor.StartTime = startTime;
            netstatExecutor.Encoding = AppArg.Encoding;
            netstatExecutor.IsAddPortAny = AppArg.IsAddPortAny;
            netstatExecutor.HostLogDirectory = AppArg.HostLogDir;
            netstatExecutor.HostLogIncludeIpListPath = AppArg.HostLogIncIpListPath;
            netstatExecutor.HostLogExcludeIpListPath = AppArg.HostLogExcIpListPath;
            netstatExecutor.Initialize();

            var program = new Program(logger, AppArg, netstatExecutor, startTime);
            program.Run();
            netstatExecutor.ShowList();
        }
        else
        {
            switch (AppArg.UsageFlag)
            {
                case ClsAppArg.USAGE_USAGE:
                    AppArg.ReturnCode = MdlConst.LVL_W;
                    AppArg.ShowUsage();
                    break;
                case ClsAppArg.USAGE_IPADDR:
                    foreach (string nicInfo in AppArg.NicInfoList)
                    {
                        logger.WriteLine(MdlConst.LVL_NONE, nicInfo);
                    }
                    break;
                default:
                    AppArg.ReturnCode = MdlConst.LVL_E;
                    break;
            }
        }

        if (AppArg.Verbose > 0)
        {
            DateTime endTime = DateTime.Now;
            double elapsedTime = (endTime - startTime).TotalSeconds;
            logger.WriteLine(MdlConst.LVL_NONE, $"===<<< [{AppArg.ExeBaseName}] EXIT ({AppArg.ReturnCode}) : {MdlDate.GetFormattedDate(endTime, "yyyy/MM/dd HH:mm:ss")} : {elapsedTime:F3} sec>>>===");
        }
        return AppArg.ReturnCode;
    }

    /// <summary>
    /// ネットワーク接続情報の定期収集ループを実行します。
    /// </summary>
    /// <example>
    /// <code>
    /// var program = new Program();
    /// program.Run();
    /// </code>
    /// </example>
    public void Run()
    {
        Console.CancelKeyPress += ConsoleCancelKeyPress;
        try
        {
            DateTime cycleStartTime = DateTime.Now;
            for (ulong i = 0; i < _AppArg.MaxLoopCount; i++)
            {
                if (i > 0)
                {
                    cycleStartTime = DateTime.Now;
                }

                if (_isCancelled)
                {
                    break;
                }

                if (_AppArg.ExecutionTimeSeconds > 0 && (cycleStartTime - _startTime).TotalSeconds >= _AppArg.ExecutionTimeSeconds)
                {
                    break;
                }

                try
                {
                    int netstatExitCode = _netstatExecutor.ExecuteNetstat();

                    if (netstatExitCode == 0)
                    {
                        _netstatExecutor.GetListenPortList();
                        _netstatExecutor.GetConnectionList();
                        _netstatExecutor.LoopCount = i + 1;
                    }
                    else
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, $"[ERR] Cmd Return Code({netstatExitCode}) != 0 : {_AppArg.CommandPath} {_AppArg.CommandArgs}");
                    }
                }
                catch (Exception ex)
                {
                    if (_AppArg.IsStackTrace)
                    {
                        _logger.WriteLine(MdlConst.LVL_E, $"EXCEPTION : {ex.Message}");
                        _logger.WriteLine(MdlConst.LVL_NONE, ex.StackTrace ?? "");
                    }
                }

                if (i < _AppArg.MaxLoopCount - 1)
                {
                    TimeSpan cycleElapsed = DateTime.Now - cycleStartTime;
                    int cycleElapsedSeconds = (int)cycleElapsed.TotalSeconds;
                    int cycleElapsedMilliseconds = cycleElapsed.Milliseconds;
                    int intervalSeconds = (_AppArg.SleepSeconds / 2) > cycleElapsedSeconds
                        ? _AppArg.SleepSeconds - cycleElapsedSeconds
                        : (_AppArg.SleepSeconds / 2);

                    for (int n = 0; n < intervalSeconds; n++)
                    {
                        if (_isCancelled)
                        {
                            break;
                        }

                        if (n == 0)
                        {
                            int firstSleep = 1000 - cycleElapsedMilliseconds;
                            if (firstSleep > 0)
                            {
                                Thread.Sleep(firstSleep);
                            }
                        }
                        else
                        {
                            Thread.Sleep(1000);
                        }
                    }
                }
            }
        }
        finally
        {
            Console.CancelKeyPress -= ConsoleCancelKeyPress;
        }
    }

    /// <summary>
    /// コンソールで Ctrl+C キーが押下されたときのキャンセル処理を実行します。
    /// </summary>
    /// <param name="sender">イベントの送信元オブジェクト。</param>
    /// <param name="e">キャンセルイベントデータ。</param>
    /// <example>
    /// <code>
    /// var program = new Program();
    /// var cancelArgs = new ConsoleCancelEventArgs(ConsoleSpecialKey.ControlC);
    /// program.ConsoleCancelKeyPress(null, cancelArgs);
    /// </code>
    /// </example>
    public void ConsoleCancelKeyPress(object? sender, ConsoleCancelEventArgs e)
    {
        _logger.WriteLine(MdlConst.LVL_W, "★★★ Ctrl+C ★★★");
        _isCancelled = true;
        e.Cancel = true;
    }
}

