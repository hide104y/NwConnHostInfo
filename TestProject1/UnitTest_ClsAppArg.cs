using System;
using System.IO;
using System.Text;
using CmnClsLib.Class;
using CmnClsLib.Module;
using NwConnHostInfo.Class;
using Xunit;

namespace TestProject1;

/// <summary>
/// <see cref="ClsAppArg"/> クラスの単体テストクラスです。
/// </summary>
public class UnitTest_ClsAppArg : IDisposable
{
    private readonly string _tempDir;
    private readonly ClsLogger _logger;

    public UnitTest_ClsAppArg()
    {
        // 注意事項に基づく作業ディレクトリの設定
        _tempDir = Path.Combine(Path.GetTempPath(), @"UnitTest", "NwConnHostInfo", "ClsAppArg");
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
    public void DefaultValues_ShouldBeInitializedCorrectly()
    {
        // Arrange & Act
        var appArg = new ClsAppArg(_logger);

        // Assert
        Assert.NotNull(appArg.ExeBaseName);
        Assert.NotNull(appArg.ExeDir);
        Assert.Equal(ClsAppArg.USAGE_NONE, appArg.UsageFlag);
        Assert.Equal(MdlConst.LVL_I, appArg.ReturnCode);
        Assert.Equal(0, appArg.Verbose);
        Assert.False(appArg.IsStackTrace);
        Assert.Equal(Environment.MachineName, appArg.MachineName);
        Assert.Equal(30, appArg.Timeout);
        Assert.Equal(3, appArg.Priority);
        Assert.Empty(appArg.NicInfoList);
        Assert.Equal("", appArg.HostLogDir);
        Assert.Equal("", appArg.HostLogIncIpListPath);
        Assert.Equal("", appArg.HostLogExcIpListPath);
        Assert.Equal("", appArg.OutputDir);
        Assert.Equal("", appArg.FileName);
        Assert.Equal(0, appArg.SleepSeconds);
        Assert.Equal(0, appArg.ExecutionTimeSeconds);
        Assert.Equal(1UL, appArg.MaxLoopCount);
        Assert.Equal("netstat", appArg.CommandPath);
        Assert.Equal("-an", appArg.CommandArgs);
        Assert.Contains("TCP", appArg.TcpConnectionRegex);
        Assert.Contains("UDP", appArg.UdpConnectionRegex);
        Assert.Equal("SJIS", appArg.EncodingName);
        Assert.False(appArg.ShowPid);
        Assert.True(appArg.IsAddPortAny);
        Assert.Equal("", appArg.DomainName);
        Assert.Equal("", appArg.Username);
        Assert.Equal("", appArg.UsernameWithoutDomain);
        Assert.Equal("", appArg.Password);
        Assert.False(appArg.IsSwitchUser);
        Assert.False(appArg.IsLogon);
        Assert.False(appArg.IsLogonAlwaysOk);
    }

    [Fact]
    public void PropertySetters_ShouldUpdateValues()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);

        // Act
        appArg.ExeBaseName = "TestApp";
        appArg.ExeDir = @"C:\TestApp";
        appArg.Pid = 9999;
        appArg.ReturnCode = MdlConst.LVL_E;
        appArg.Verbose = 2;
        appArg.IsStackTrace = true;
        appArg.MachineName = "TEST-PC";
        appArg.Timeout = 60;
        appArg.Priority = 4;
        appArg.NicInfoList = ["NIC1", "NIC2"];
        appArg.HostLogDir = @"C:\Logs\Host";
        appArg.HostLogIncIpListPath = @"C:\Config\inc.txt";
        appArg.HostLogExcIpListPath = @"C:\Config\exc.txt";
        appArg.OutputDir = @"C:\Output";
        appArg.FileName = "output.txt";
        appArg.SleepSeconds = 15;
        appArg.ExecutionTimeSeconds = 300;
        appArg.MaxLoopCount = 20;
        appArg.CommandPath = "custom_netstat.exe";
        appArg.CommandArgs = "-ano -p tcp";
        appArg.TcpConnectionRegex = "^custom_tcp$";
        appArg.UdpConnectionRegex = "^custom_udp$";
        appArg.Encoding = Encoding.UTF8;
        appArg.EncodingName = "UTF-8";
        appArg.ShowPid = true;
        appArg.IsAddPortAny = false;
        appArg.DomainName = "MYDOMAIN";
        appArg.Username = @"MYDOMAIN\admin";
        appArg.UsernameWithoutDomain = "admin";
        appArg.Password = "p@ssword";
        appArg.IsSwitchUser = true;
        appArg.IsLogon = true;
        appArg.IsLogonAlwaysOk = true;

        // Assert
        Assert.Equal("TestApp", appArg.ExeBaseName);
        Assert.Equal(@"C:\TestApp", appArg.ExeDir);
        Assert.Equal(9999, appArg.Pid);
        Assert.Equal(MdlConst.LVL_E, appArg.ReturnCode);
        Assert.Equal(2, appArg.Verbose);
        Assert.True(appArg.IsStackTrace);
        Assert.Equal("TEST-PC", appArg.MachineName);
        Assert.Equal(60, appArg.Timeout);
        Assert.Equal(4, appArg.Priority);
        Assert.Equal(2, appArg.NicInfoList.Count);
        Assert.Equal(@"C:\Logs\Host", appArg.HostLogDir);
        Assert.Equal(@"C:\Config\inc.txt", appArg.HostLogIncIpListPath);
        Assert.Equal(@"C:\Config\exc.txt", appArg.HostLogExcIpListPath);
        Assert.Equal(@"C:\Output", appArg.OutputDir);
        Assert.Equal("output.txt", appArg.FileName);
        Assert.Equal(15, appArg.SleepSeconds);
        Assert.Equal(300, appArg.ExecutionTimeSeconds);
        Assert.Equal(20UL, appArg.MaxLoopCount);
        Assert.Equal("custom_netstat.exe", appArg.CommandPath);
        Assert.Equal("-ano -p tcp", appArg.CommandArgs);
        Assert.Equal("^custom_tcp$", appArg.TcpConnectionRegex);
        Assert.Equal("^custom_udp$", appArg.UdpConnectionRegex);
        Assert.Equal(Encoding.UTF8, appArg.Encoding);
        Assert.Equal("UTF-8", appArg.EncodingName);
        Assert.True(appArg.ShowPid);
        Assert.False(appArg.IsAddPortAny);
        Assert.Equal("MYDOMAIN", appArg.DomainName);
        Assert.Equal(@"MYDOMAIN\admin", appArg.Username);
        Assert.Equal("admin", appArg.UsernameWithoutDomain);
        Assert.Equal("p@ssword", appArg.Password);
        Assert.True(appArg.IsSwitchUser);
        Assert.True(appArg.IsLogon);
        Assert.True(appArg.IsLogonAlwaysOk);
    }

    [Theory]
    [InlineData("UTF8", "System.Text.UTF8Encoding")]
    [InlineData("UTF-8", "System.Text.UTF8Encoding")]
    [InlineData("UNICODE", "System.Text.UnicodeEncoding")]
    [InlineData("ASCII", "System.Text.ASCIIEncoding")]
    [InlineData("DEFAULT", null)]
    [InlineData("JIS", "System.Text.ISO2022Encoding")]
    [InlineData("EUC", "System.Text.EUCJPEncoding")]
    [InlineData("EUCJP", "System.Text.EUCJPEncoding")]
    [InlineData("EUC-JP", "System.Text.EUCJPEncoding")]
    [InlineData("UNKNOWN_ENC", "System.Text.DBCSCodePageEncoding")]
    public void GetEncoding_ShouldReturnExpectedEncoding(string name, string? expectedTypeName)
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);

        // Act
        Encoding enc = appArg.GetEncoding(name);

        // Assert
        Assert.NotNull(enc);
        if (expectedTypeName != null)
        {
            Assert.Contains(expectedTypeName, enc.GetType().FullName);
        }
    }

    [Theory]
    [InlineData("utf8", "UTF8")]
    [InlineData("utf-8", "UTF-8")]
    [InlineData("unicode", "UNICODE")]
    [InlineData("ascii", "ASCII")]
    [InlineData("default", "DEFAULT")]
    [InlineData("jis", "JIS")]
    [InlineData("euc", "EUC")]
    [InlineData("eucjp", "EUCJP")]
    [InlineData("euc-jp", "EUC-JP")]
    [InlineData("other", "SJIS")]
    public void NormalizeEncodingName_ShouldNormalizeCorrectly(string input, string expected)
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);

        // Act
        string normalized = appArg.NormalizeEncodingName(input);

        // Assert
        Assert.Equal(expected, normalized);
    }

    [Fact]
    public void Parse_BasicOptions_ShouldParseSuccessfully()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);
        string outputDir = Path.Combine(_tempDir, "OutDir");
        string hostLogDir = Path.Combine(_tempDir, "HostDir");
        string incIpFile = Path.Combine(_tempDir, "inc_ip.txt");
        string excIpFile = Path.Combine(_tempDir, "exc_ip.txt");

        File.WriteAllText(incIpFile, "192.168.1.1");
        File.WriteAllText(excIpFile, "192.168.1.2");

        string[] args =
        [
            "-o", outputDir,
            "--hl", hostLogDir,
            "--i-ip", incIpFile,
            "--x-ip", excIpFile,
            "-n", "my_custom_log.txt",
            "--cmd", "netstat.exe",
            "--arg", @"\-ano -p tcp",
            "--tcp-regex", @"^\s*TCP.*$",
            "--udp-regex", @"^\s*UDP.*$",
            "-s", "10",
            "-c", "50",
            "-et", "500",
            "--nice", "4",
            "--pid",
            "--add-any", "false",
            "--enc", "UTF8"
        ];

        // Act
        bool result = appArg.Parse(args);

        // Assert
        Assert.True(result);
        Assert.Equal(outputDir, appArg.OutputDir);
        Assert.True(Directory.Exists(outputDir));
        Assert.Equal(hostLogDir, appArg.HostLogDir);
        Assert.True(Directory.Exists(hostLogDir));
        Assert.Equal(incIpFile, appArg.HostLogIncIpListPath);
        Assert.Equal(excIpFile, appArg.HostLogExcIpListPath);
        Assert.Equal("my_custom_log.txt", appArg.FileName);
        Assert.Equal("netstat.exe", appArg.CommandPath);
        Assert.Equal("-ano -p tcp", appArg.CommandArgs);
        Assert.Equal(@"^\s*TCP.*$", appArg.TcpConnectionRegex);
        Assert.Equal(@"^\s*UDP.*$", appArg.UdpConnectionRegex);
        Assert.Equal(10, appArg.SleepSeconds);
        Assert.Equal(500, appArg.ExecutionTimeSeconds);
        Assert.Equal(50UL, appArg.MaxLoopCount); // 500 / 10 = 50
        Assert.Equal(4, appArg.Priority);
        Assert.True(appArg.ShowPid);
        Assert.False(appArg.IsAddPortAny);
        Assert.Equal("UTF8", appArg.EncodingName);
        Assert.Equal(Encoding.UTF8, appArg.Encoding);
    }

    [Fact]
    public void Parse_AuthOptions_ShouldParseSuccessfully()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);
        string[] args =
        [
            "-domain", "DOMAIN1",
            "-u", @"DOMAIN1\testuser",
            "-p", "testpass",
            "-ignore-fail",
            "-su",
            "-logon"
        ];

        // Act
        bool result = appArg.Parse(args);

        // Assert
        Assert.True(result);
        Assert.Equal("DOMAIN1", appArg.DomainName);
        Assert.Equal(@"DOMAIN1\testuser", appArg.Username);
        Assert.Equal("testuser", appArg.UsernameWithoutDomain);
        Assert.Equal("testpass", appArg.Password);
        Assert.True(appArg.IsLogonAlwaysOk);
        Assert.True(appArg.IsSwitchUser);
        Assert.True(appArg.IsLogon);
    }

    [Fact]
    public void Parse_HelpAndShowIp_ShouldSetUsageFlag()
    {
        // Arrange: -h
        var appArgHelp = new ClsAppArg(_logger);
        bool resultHelp = appArgHelp.Parse(["-h"]);
        Assert.True(resultHelp);
        Assert.Equal(ClsAppArg.USAGE_USAGE, appArgHelp.UsageFlag);

        // Arrange: --show-ipaddr
        var appArgIp = new ClsAppArg(_logger);
        bool resultIp = appArgIp.Parse(["--show-ipaddr"]);
        Assert.True(resultIp);
        Assert.Equal(ClsAppArg.USAGE_IPADDR, appArgIp.UsageFlag);
    }

    [Fact]
    public void Parse_InvalidFilePaths_ShouldReturnFalse()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);
        string nonExistentFile = Path.Combine(_tempDir, "non_existent.txt");
        string existingFileAsDir = Path.Combine(_tempDir, "file_as_dir.txt");
        File.WriteAllText(existingFileAsDir, "dummy");

        // Act 1: 存在しないファイルを --i-ip に指定
        bool resultIncIp = appArg.Parse(["--i-ip", nonExistentFile]);
        Assert.False(resultIncIp);
        Assert.Equal("", appArg.HostLogIncIpListPath);

        // Act 2: 存在しないファイルを --x-ip に指定
        bool resultExcIp = appArg.Parse(["--x-ip", nonExistentFile]);
        Assert.False(resultExcIp);
        Assert.Equal("", appArg.HostLogExcIpListPath);

        // Act 3: 既存ファイルをディレクトリ -o に指定
        bool resultOutDir = appArg.Parse(["-o", existingFileAsDir]);
        Assert.False(resultOutDir);
        Assert.Equal("", appArg.OutputDir);

        // Act 4: 既存ファイルをディレクトリ --hl に指定
        bool resultHostDir = appArg.Parse(["--hl", existingFileAsDir]);
        Assert.False(resultHostDir);
        Assert.Equal("", appArg.HostLogDir);
    }

    [Fact]
    public void ReadFileLines_ShouldFilterCommentsAndEmptyLines()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);
        string testFile = Path.Combine(_tempDir, "read_test.txt");
        string fileContent = """
            # This is a comment
            192.168.1.10
               # Indented comment
            
            192.168.1.20
               
            192.168.1.30
            """;
        File.WriteAllText(testFile, fileContent, Encoding.UTF8);

        // Act
        var lines = appArg.ReadFileLines(testFile, Encoding.UTF8);

        // Assert
        Assert.Equal(3, lines.Count);
        Assert.Equal("192.168.1.10", lines[0]);
        Assert.Equal("192.168.1.20", lines[1]);
        Assert.Equal("192.168.1.30", lines[2]);
    }

    [Fact]
    public void ReadFileLines_NonExistentFile_ShouldReturnEmptyListWithoutThrowing()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);
        string nonExistentFile = Path.Combine(_tempDir, "not_found.txt");

        // Act
        var lines = appArg.ReadFileLines(nonExistentFile, Encoding.UTF8);

        // Assert
        Assert.NotNull(lines);
        Assert.Empty(lines);
    }

    [Fact]
    public void WriteFile_ShouldWriteAndAppendContentCorrectly()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);
        string targetFile = Path.Combine(_tempDir, "write_test.txt");

        // Act: 新規書き込み
        appArg.WriteFile(targetFile, "Line 1", Encoding.UTF8, append: false);

        // Assert
        Assert.True(File.Exists(targetFile));
        string content1 = File.ReadAllText(targetFile, Encoding.UTF8);
        Assert.Contains("Line 1", content1);

        // Act: 追記
        appArg.WriteFile(targetFile, "Line 2", Encoding.UTF8, append: true);

        // Assert
        string[] allLines = File.ReadAllLines(targetFile, Encoding.UTF8);
        Assert.Equal(2, allLines.Length);
        Assert.Equal("Line 1", allLines[0]);
        Assert.Equal("Line 2", allLines[1]);
    }

    [Fact]
    public void LoadIpAddresses_ShouldPopulateNicInfoList()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);

        // Act
        appArg.LoadIpAddresses();

        // Assert
        Assert.NotEmpty(appArg.NicInfoList);
        Assert.Contains(appArg.NicInfoList, x => x.StartsWith("HOSTNAME   ="));
        Assert.Contains(appArg.NicInfoList, x => x.StartsWith("USERDOMAIN ="));
    }

    [Fact]
    public void ShowUsage_ShouldExecuteWithoutException()
    {
        // Arrange
        var appArg = new ClsAppArg(_logger);

        // Act & Assert (例外が発生しないことを確認)
        var ex = Record.Exception(() => appArg.ShowUsage());
        Assert.Null(ex);
    }
}
