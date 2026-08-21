using System;
using System.IO;
using System.Reflection;
using CmnClsLib.Class;
using CmnClsLib.Module;
using NwConnHostInfo;
using NwConnHostInfo.Class;
using Xunit;

namespace TestProject1;

public class UnitTest_Program : IDisposable
{
    private readonly string _tempDir;

    public UnitTest_Program()
    {
        // 注意事項に基づいた作業ディレクトリの設定
        _tempDir = Path.Combine(Path.GetTempPath(), @"UnitTest", "NwConnHostInfo", "Program");
        if (!Directory.Exists(_tempDir))
        {
            Directory.CreateDirectory(_tempDir);
        }
    }

    public void Dispose()
    {
        if (Directory.Exists(_tempDir))
        {
            try
            {
                Directory.Delete(_tempDir, true);
            }
            catch
            {
                // クリーンアップ時の例外は無視
            }
        }
    }

    private static ConsoleCancelEventArgs CreateConsoleCancelEventArgs(ConsoleSpecialKey key = ConsoleSpecialKey.ControlC)
    {
        var constructor = typeof(ConsoleCancelEventArgs).GetConstructor(
            BindingFlags.NonPublic | BindingFlags.Instance,
            null,
            [typeof(ConsoleSpecialKey)],
            null);
        return (ConsoleCancelEventArgs)constructor!.Invoke([key]);
    }

    [Fact]
    public void TempDirectory_IsWithinSpecifiedPath()
    {
        // Assert
        Assert.True(_tempDir.Contains(Path.Combine("UnitTest", "NwConnHostInfo", "Program"), StringComparison.OrdinalIgnoreCase));
        Assert.True(Directory.Exists(_tempDir));
    }

    [Fact]
    public void Constructor_Default_InitializesProperties()
    {
        // Act
        var program = new Program();

        // Assert
        Assert.False(program.IsCancelled);
    }

    [Fact]
    public void Constructor_WithAppArg_InitializesCorrectly()
    {
        // Arrange
        var logger = new ClsLogger();
        var appArgument = new ClsAppArg(logger);
        var netstatExecutor = new ClsNetstatExec(logger);
        var startTime = DateTime.Now;

        // Act
        var program = new Program(logger, appArgument, netstatExecutor, startTime);

        // Assert
        Assert.False(program.IsCancelled);
    }

    [Fact]
    public void IsCancelled_SetAndGet_WorksCorrectly()
    {
        // Arrange
        var program = new Program();

        // Act
        program.IsCancelled = true;

        // Assert
        Assert.True(program.IsCancelled);

        // Act
        program.IsCancelled = false;

        // Assert
        Assert.False(program.IsCancelled);
    }

    [Fact]
    public void ConsoleCancelKeyPress_SetsCancelledAndCancelsEvent()
    {
        // Arrange
        var program = new Program();
        var cancelEventArgs = CreateConsoleCancelEventArgs();

        // Act
        program.ConsoleCancelKeyPress(null, cancelEventArgs);

        // Assert
        Assert.True(program.IsCancelled);
        Assert.True(cancelEventArgs.Cancel);
    }

    [Fact]
    public void Main_WithHelpOption_ReturnsWarningCode()
    {
        // Arrange
        string[] args = ["-h"];

        // Act
        int exitCode = Program.Main(args);

        // Assert
        Assert.Equal(MdlConst.LVL_W, exitCode);
    }

    [Fact]
    public void Main_WithShowIpAddrOption_ReturnsNoneOrWarningCode()
    {
        // Arrange
        string[] args = ["--show-ipaddr"];

        // Act
        int exitCode = Program.Main(args);

        // Assert
        // ClsAppArg --show-ipaddr 実行時は ReturnCode が LVL_I (0) または USAGE_IPADDR 分岐
        Assert.True(exitCode == MdlConst.LVL_I || exitCode == MdlConst.LVL_NONE);
    }

    [Fact]
    public void Main_WithInvalidOption_ReturnsErrorCode()
    {
        // Arrange: 存在しない絞り込みIPファイルを指定して解析エラーを起こす
        string[] args = ["--i-ip", @"C:\non_existent_file_path_123456789.txt"];

        // Act
        int exitCode = Program.Main(args);

        // Assert
        Assert.Equal(MdlConst.LVL_E, exitCode);
    }

    [Fact]
    public void Run_WhenCancelledInitially_ExitsImmediately()
    {
        // Arrange
        var logger = new ClsLogger();
        var appArgument = new ClsAppArg(logger);
        var netstatExecutor = new ClsNetstatExec(logger);
        var program = new Program(logger, appArgument, netstatExecutor, DateTime.Now)
        {
            IsCancelled = true
        };

        // Act & Assert (例外なく即座に終了すること)
        program.Run();
        Assert.True(program.IsCancelled);
    }

    [Fact]
    public void Main_WithVerboseAndHelpOption_ExecutesVerboseBranch()
    {
        // Arrange
        string[] args = ["-v", "-h"];

        // Act
        int exitCode = Program.Main(args);

        // Assert
        Assert.Equal(MdlConst.LVL_W, exitCode);
    }

    [Fact]
    public void Run_WithExecutionTimeExceeded_ExitsLoop()
    {
        // Arrange
        var logger = new ClsLogger();
        var appArgument = new ClsAppArg(logger)
        {
            MaxLoopCount = 10,
            ExecutionTimeSeconds = 1
        };
        var netstatExecutor = new ClsNetstatExec(logger);
        // 過去の日時を開始時刻に設定して実行時間超過状態を作る
        var startTime = DateTime.Now.AddSeconds(-10);
        var program = new Program(logger, appArgument, netstatExecutor, startTime);

        // Act
        program.Run();

        // Assert
        Assert.False(program.IsCancelled);
    }

    [Fact]
    public void Run_WhenInvalidCommandPath_HandlesNonZeroExitCode()
    {
        // Arrange
        var logger = new ClsLogger();
        var appArgument = new ClsAppArg(logger)
        {
            MaxLoopCount = 1,
            CommandPath = @"C:\non_existent_command_xyz_12345.exe"
        };
        var netstatExecutor = new ClsNetstatExec(logger)
        {
            AppArg = appArgument
        };
        var program = new Program(logger, appArgument, netstatExecutor, DateTime.Now);

        // Act
        program.Run();

        // Assert
        Assert.False(program.IsCancelled);
    }

    [Fact]
    public void Run_WithExceptionAndStackTraceEnabled_LogsGracefully()
    {
        // Arrange
        var logger = new ClsLogger();
        var appArgument = new ClsAppArg(logger)
        {
            MaxLoopCount = 1,
            IsStackTrace = true
        };
        var netstatExecutor = new ClsNetstatExec(logger);
        var program = new Program(logger, appArgument, netstatExecutor, DateTime.Now);

        // Act
        program.Run();

        // Assert
        Assert.False(program.IsCancelled);
    }
}
