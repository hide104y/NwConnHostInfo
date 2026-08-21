using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;
using CmnClsLib.Class;
using NwConnHostInfo.Class;
using Xunit;

namespace TestProject1;

public class UnitTest_ClsNetstatExec : IDisposable
{
    private readonly string _tempDir;
    private readonly ClsLogger _logger;

    public UnitTest_ClsNetstatExec()
    {
        // 注意事項に基づいた作業ディレクトリの設定
        _tempDir = Path.Combine(Path.GetTempPath(), @"UnitTest", "NwConnHostInfo", "ClsNetstatExec");
        if (!Directory.Exists(_tempDir))
        {
            Directory.CreateDirectory(_tempDir);
        }
        _logger = new ClsLogger();
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

    [Fact]
    public void Constructor_ShouldInitializeDefaults()
    {
        // Arrange & Act
        var exec = new ClsNetstatExec(_logger);

        // Assert
        Assert.NotNull(exec.AppArg);
        Assert.NotNull(exec.TcpListenPortList);
        Assert.NotNull(exec.UdpListenPortList);
        Assert.NotNull(exec.TcpListenPortStringList);
        Assert.NotNull(exec.UdpListenPortStringList);
        Assert.NotNull(exec.TcpSendDictionary);
        Assert.NotNull(exec.TcpRecvDictionary);
        Assert.NotNull(exec.UdpSendDictionary);
        Assert.NotNull(exec.UdpRecvDictionary);
        Assert.NotNull(exec.TcpAppDictionary);
        Assert.NotNull(exec.UdpAppDictionary);
        Assert.Equal(0, exec.ReturnCode);
        Assert.Equal(0UL, exec.LoopCount);
        Assert.True(exec.IsAddPortAny);
        Assert.Equal("", exec.HostLogDirectory);
        Assert.Equal("", exec.HostLogIncludeIpListPath);
        Assert.Equal("", exec.HostLogExcludeIpListPath);
    }

    [Fact]
    public void PropertySetters_ShouldUpdateValues()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        var now = DateTime.Now;
        var customAppArg = new ClsAppArg(_logger);

        // Act
        exec.AppArg = customAppArg;
        exec.ReturnCode = 10;
        exec.LoopCount = 5;
        exec.StartTime = now;
        exec.Encoding = Encoding.UTF8;
        exec.IsAddPortAny = false;
        exec.HostLogDirectory = @"C:\Logs";
        exec.HostLogIncludeIpListPath = @"C:\Config\inc.txt";
        exec.HostLogExcludeIpListPath = @"C:\Config\exc.txt";

        // Assert
        Assert.Same(customAppArg, exec.AppArg);
        Assert.Equal(10, exec.ReturnCode);
        Assert.Equal(5UL, exec.LoopCount);
        Assert.Equal(now, exec.StartTime);
        Assert.Equal(Encoding.UTF8, exec.Encoding);
        Assert.False(exec.IsAddPortAny);
        Assert.Equal(@"C:\Logs", exec.HostLogDirectory);
        Assert.Equal(@"C:\Config\inc.txt", exec.HostLogIncludeIpListPath);
        Assert.Equal(@"C:\Config\exc.txt", exec.HostLogExcludeIpListPath);
    }

    [Theory]
    [InlineData("127.0.0.1", true)]
    [InlineData("[::1]", true)]
    [InlineData("192.168.1.1", false)]
    [InlineData("::1", false)]
    [InlineData("", false)]
    public void IsLoopbackAddress_ShouldJudgeCorrectly(string address, bool expected)
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        exec.Initialize();

        // Act & Assert
        Assert.Equal(expected, exec.IsLoopbackAddress(address));
    }

    [Theory]
    [InlineData("LISTENING", true)]
    [InlineData("LISTEN", true)]
    [InlineData("listening", true)]
    [InlineData("listen", true)]
    [InlineData("ESTABLISHED", false)]
    [InlineData("CLOSE_WAIT", false)]
    [InlineData("", false)]
    public void IsTcpListenState_ShouldJudgeCorrectly(string state, bool expected)
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);

        // Act & Assert
        Assert.Equal(expected, exec.IsTcpListenState(state));
    }

    [Theory]
    [InlineData("*:*", true)]
    [InlineData("192.168.1.1:53", false)]
    [InlineData("", false)]
    public void IsUdpListenState_ShouldJudgeCorrectly(string state, bool expected)
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        exec.Initialize();

        // Act & Assert
        Assert.Equal(expected, exec.IsUdpListenState(state));
    }

    [Theory]
    [InlineData("ESTABLISHED", true)]
    [InlineData("FIN_WAIT_1", true)]
    [InlineData("FIN_WAIT_2", true)]
    [InlineData("CLOSE_WAIT", true)]
    [InlineData("CLOSING", true)]
    [InlineData("LAST_ACK", true)]
    [InlineData("TIME_WAIT", true)]
    [InlineData("established", true)]
    [InlineData("time_wait", true)]
    [InlineData("LISTENING", false)]
    [InlineData("UNKNOWN", false)]
    public void IsTcpConnectionState_ShouldJudgeCorrectly(string state, bool expected)
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);

        // Act & Assert
        Assert.Equal(expected, exec.IsTcpConnectionState(state));
    }

    [Theory]
    [InlineData("*:*", false)]
    [InlineData("192.168.1.1:53", true)]
    [InlineData("10.0.0.1:123", true)]
    public void IsUdpConnectionState_ShouldJudgeCorrectly(string state, bool expected)
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        exec.Initialize();

        // Act & Assert
        Assert.Equal(expected, exec.IsUdpConnectionState(state));
    }

    [Theory]
    [InlineData("192.168.1.1", true)]
    [InlineData("10.0.0.0/8", true)]
    [InlineData("255.255.255.255", true)]
    [InlineData("::1", false)]
    [InlineData("fe80::1", false)]
    [InlineData("", false)]
    [InlineData(null, false)]
    public void IsIPv4Address_ShouldJudgeCorrectly(string? ipAddress, bool expected)
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);

        // Act & Assert
        Assert.Equal(expected, exec.IsIPv4Address(ipAddress!));
    }

    [Fact]
    public void LoadValidIpAddresses_And_IsValidIpAddress_WithIncludeFilter()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        string incFilePath = Path.Combine(_tempDir, "include_ips.txt");
        File.WriteAllLines(incFilePath,
        [
            "# コメント行",
            "192.168.1.0/24",
            "10.0.0.1",
            "fe80::1"
        ]);

        exec.HostLogIncludeIpListPath = incFilePath;
        exec.Initialize();
        exec.LoadValidIpAddresses();

        // Act & Assert
        Assert.True(exec.IsValidIpAddress("192.168.1.50"));
        Assert.True(exec.IsValidIpAddress("10.0.0.1"));
        Assert.True(exec.IsValidIpAddress("fe80::1"));
        Assert.False(exec.IsValidIpAddress("192.168.2.1"));
        Assert.False(exec.IsValidIpAddress("10.0.0.2"));
        Assert.False(exec.IsValidIpAddress("fe80::2"));
    }

    [Fact]
    public void LoadValidIpAddresses_And_IsValidIpAddress_WithExcludeFilter()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        string excFilePath = Path.Combine(_tempDir, "exclude_ips.txt");
        File.WriteAllLines(excFilePath,
        [
            "192.168.100.0/24",
            "fe80::99"
        ]);

        exec.HostLogExcludeIpListPath = excFilePath;
        exec.Initialize();
        exec.LoadValidIpAddresses();

        // Act & Assert
        Assert.False(exec.IsValidIpAddress("192.168.100.10"));
        Assert.False(exec.IsValidIpAddress("fe80::99"));
        Assert.True(exec.IsValidIpAddress("192.168.1.1"));
        Assert.True(exec.IsValidIpAddress("fe80::1"));
    }

    [Fact]
    public void GetListenPortList_ShouldParseTcpAndUdpListeningLines_WithoutPid()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        exec.AppArg.Parse([]);
        exec.Initialize();

        // PIDなし netstat 出力
        string dummyOutput =
            "  TCP    0.0.0.0:80             0.0.0.0:0              LISTENING\r\n" +
            "  TCP    127.0.0.1:8080         0.0.0.0:0              LISTENING\r\n" + // ループバックは除外
            "  TCP    [::]:443               [::]:0                 LISTENING\r\n" +
            "  UDP    0.0.0.0:53             *:*\r\n" +
            "  UDP    127.0.0.1:123          *:*\r\n"; // ループバックは除外

        exec.ClearStringBuilderWithLock();
        var cmdExecField = typeof(ClsNetstatExec).GetField("_cmdExec", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
        var cmdExec = (ClsCmdExec)cmdExecField!.GetValue(exec)!;
        cmdExec.StringBuilder.Append(dummyOutput);

        // Act
        exec.GetListenPortList();

        // Assert
        Assert.Contains(80, exec.TcpListenPortList);
        Assert.Contains(443, exec.TcpListenPortList);
        Assert.DoesNotContain(8080, exec.TcpListenPortList); // 127.0.0.1 は除外
        Assert.Contains("0.0.0.0:80", exec.TcpListenPortStringList);
        Assert.Contains("[::]:443", exec.TcpListenPortStringList);

        Assert.Contains(53, exec.UdpListenPortList);
        Assert.DoesNotContain(123, exec.UdpListenPortList); // 127.0.0.1 は除外
        Assert.Contains("0.0.0.0:53", exec.UdpListenPortStringList);
    }

    [Fact]
    public void GetListenPortList_ShouldParseTcpAndUdpListeningLines_WithPid()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        exec.AppArg.Parse(["--pid"]);
        exec.Initialize();

        // PIDあり netstat 出力
        string dummyOutput =
            "  TCP    0.0.0.0:8088           0.0.0.0:0              LISTENING       4\r\n" +
            "  UDP    0.0.0.0:5353           *:*                                    4\r\n";

        exec.ClearStringBuilderWithLock();
        var cmdExecField = typeof(ClsNetstatExec).GetField("_cmdExec", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
        var cmdExec = (ClsCmdExec)cmdExecField!.GetValue(exec)!;
        cmdExec.StringBuilder.Append(dummyOutput);

        // Act
        exec.GetListenPortList();

        // Assert
        Assert.Contains(8088, exec.TcpListenPortList);
        Assert.Contains("0.0.0.0:8088", exec.TcpListenPortStringList);
        Assert.True(exec.TcpAppDictionary.ContainsKey("0.0.0.0:8088"));

        Assert.Contains(5353, exec.UdpListenPortList);
        Assert.Contains("0.0.0.0:5353", exec.UdpListenPortStringList);
        Assert.True(exec.UdpAppDictionary.ContainsKey("0.0.0.0:5353"));
    }

    [Fact]
    public void GetConnectionList_ShouldParseInboundAndOutboundConnections()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        exec.AppArg.Parse([]);
        exec.HostLogDirectory = _tempDir;
        exec.Initialize();

        // 80番と53番をリッスンポートとして登録
        exec.TcpListenPortList = [80];
        exec.UdpListenPortList = [53];

        string dummyOutput =
            "  TCP    192.168.1.10:80        192.168.1.100:54321    ESTABLISHED\r\n" + // Inbound (ポート80)
            "  TCP    192.168.1.10:50001     93.184.216.34:443      ESTABLISHED\r\n" + // Outbound (ポート50001)
            "  UDP    192.168.1.10:53        192.168.1.100:61234\r\n" +                // UDP Inbound
            "  UDP    192.168.1.10:50002     8.8.8.8:53\r\n";                          // UDP Outbound

        var cmdExecField = typeof(ClsNetstatExec).GetField("_cmdExec", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
        var cmdExec = (ClsCmdExec)cmdExecField!.GetValue(exec)!;
        cmdExec.StringBuilder.Append(dummyOutput);

        // Act
        exec.GetConnectionList();

        // Assert
        // TCP Inbound
        Assert.True(exec.TcpRecvDictionary.ContainsKey("192.168.1.10:80"));
        Assert.Contains("192.168.1.100:ANY", exec.TcpRecvDictionary["192.168.1.10:80"]);

        // TCP Outbound
        Assert.True(exec.TcpSendDictionary.ContainsKey("192.168.1.10:ANY"));
        Assert.Contains("93.184.216.34:443", exec.TcpSendDictionary["192.168.1.10:ANY"]);

        // UDP Inbound
        Assert.True(exec.UdpRecvDictionary.ContainsKey("192.168.1.10:53"));
        Assert.Contains("192.168.1.100:ANY", exec.UdpRecvDictionary["192.168.1.10:53"]);

        // UDP Outbound
        Assert.True(exec.UdpSendDictionary.ContainsKey("192.168.1.10:ANY"));
        Assert.Contains("8.8.8.8:53", exec.UdpSendDictionary["192.168.1.10:ANY"]);
    }

    [Fact]
    public void ShowList_ShouldWriteSummaryToFile()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        string outputFileName = "netstat_summary.txt";
        exec.AppArg.OutputDir = _tempDir;
        exec.AppArg.FileName = outputFileName;
        exec.AppArg.NicInfoList = ["Ethernet (192.168.1.10)"];
        exec.StartTime = DateTime.Now;
        exec.LoopCount = 1;
        exec.TcpListenPortStringList = ["0.0.0.0:80"];
        exec.UdpListenPortStringList = ["0.0.0.0:53"];
        exec.TcpRecvDictionary.Add("192.168.1.10:80", ["192.168.1.100:ANY"]);
        exec.TcpSendDictionary.Add("192.168.1.10:ANY", ["93.184.216.34:443"]);
        exec.UdpRecvDictionary.Add("192.168.1.10:53", ["192.168.1.100:ANY"]);
        exec.UdpSendDictionary.Add("192.168.1.10:ANY", ["8.8.8.8:53"]);

        // Act
        exec.ShowList();

        // Assert
        string outPath = Path.Combine(_tempDir, outputFileName);
        Assert.True(File.Exists(outPath));

        string content = File.ReadAllText(outPath, exec.Encoding);
        Assert.Contains("#■INFO", content);
        Assert.Contains("# NIC Ethernet (192.168.1.10)", content);
        Assert.Contains("#■TCP LISTEN PORT LIST", content);
        Assert.Contains("TCP L 0.0.0.0:80", content);
        Assert.Contains("#■UDP LISTEN PORT LIST", content);
        Assert.Contains("UDP L 0.0.0.0:53", content);
        Assert.Contains("#■TCP INBOUND CONNECTION", content);
        Assert.Contains("TCP I 192.168.1.100:ANY => 192.168.1.10:80", content);
        Assert.Contains("#■TCP OUTBOUND CONNECTION", content);
        Assert.Contains("TCP O 192.168.1.10:ANY => 93.184.216.34:443", content);
        Assert.Contains("#■UDP INBOUND CONNECTION", content);
        Assert.Contains("UDP I 192.168.1.100:ANY => 192.168.1.10:53", content);
        Assert.Contains("#■UDP OUTBOUND CONNECTION", content);
        Assert.Contains("UDP O 192.168.1.10:ANY => 8.8.8.8:53", content);
    }

    [Fact]
    public void GetAppProperties_ShouldReturnProcessInfoForCurrentProcess()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        int currentPid = Process.GetCurrentProcess().Id;

        // Act
        var prop = exec.GetAppProperties(currentPid.ToString());

        // Assert
        Assert.Equal(currentPid, prop.Pid);
        Assert.NotEmpty(prop.AppName);
        Assert.NotEqual("-", prop.AppName);
    }

    [Theory]
    [InlineData("0")]
    [InlineData("-1")]
    [InlineData("invalid")]
    [InlineData("99999999")]
    public void GetAppProperties_WithInvalidPid_ShouldReturnDefault(string pidStr)
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);

        // Act
        var prop = exec.GetAppProperties(pidStr);

        // Assert
        Assert.NotNull(prop);
        if (prop.Pid <= 0)
        {
            Assert.Equal("-", prop.AppName);
            Assert.Equal("-", prop.AppPath);
        }
    }

    [Fact]
    public void ClearStringBuilderWithLock_ShouldClearBuffer()
    {
        // Arrange
        var exec = new ClsNetstatExec(_logger);
        var cmdExecField = typeof(ClsNetstatExec).GetField("_cmdExec", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
        var cmdExec = (ClsCmdExec)cmdExecField!.GetValue(exec)!;
        cmdExec.StringBuilder.Append("temporary output text");

        // Act
        exec.ClearStringBuilderWithLock();

        // Assert
        Assert.Equal(0, cmdExec.StringBuilder.Length);
    }
}
