using System;
using System.IO;
using NwConnHostInfo.Class;
using Xunit;

namespace TestProject1;

public class UnitTest_ClsIPUtils : IDisposable
{
    private readonly string _tempDir;
    private readonly ClsIPUtils _ipUtils;

    public UnitTest_ClsIPUtils()
    {
        // 注意事項に基づいた作業ディレクトリの設定
        _tempDir = Path.Combine(Path.GetTempPath(), @"UnitTest", "NwConnHostInfo", "ClsIPUtils");
        if (!Directory.Exists(_tempDir))
        {
            Directory.CreateDirectory(_tempDir);
        }
        _ipUtils = new ClsIPUtils();
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

    #region IsIpInNetwork テスト

    [Theory]
    [InlineData("192.168.1.1", "192.168.1.0/24", true)]
    [InlineData("192.168.1.254", "192.168.1.0/24", true)]
    [InlineData("192.168.2.1", "192.168.1.0/24", false)]
    [InlineData("10.50.100.200", "10.0.0.0/8", true)]
    [InlineData("11.0.0.1", "10.0.0.0/8", false)]
    [InlineData("172.16.0.1", "172.16.0.0/12", true)]
    [InlineData("172.31.255.254", "172.16.0.0/12", true)]
    [InlineData("172.32.0.1", "172.16.0.0/12", false)]
    [InlineData("127.0.0.1", "127.0.0.0/8", true)]
    public void IsIpInNetwork_ValidIpv4_ShouldReturnExpectedResult(string targetIp, string networkCidr, bool expected)
    {
        // Act
        bool actual = _ipUtils.IsIpInNetwork(targetIp, networkCidr);

        // Assert
        Assert.Equal(expected, actual);
    }

    [Theory]
    [InlineData("", "192.168.1.0/24")]
    [InlineData("   ", "192.168.1.0/24")]
    [InlineData("invalid_ip", "192.168.1.0/24")]
    [InlineData("192.168.1.1", "")]
    [InlineData("192.168.1.1", "invalid_network")]
    [InlineData("192.168.1.1", "192.168.1.0/33")]
    public void IsIpInNetwork_InvalidInputs_ShouldReturnFalse(string targetIp, string networkCidr)
    {
        // Act
        bool actual = _ipUtils.IsIpInNetwork(targetIp, networkCidr);

        // Assert
        Assert.False(actual);
    }

    #endregion

    #region JudgePrivateIPv4Address テスト

    [Theory]
    [InlineData("10.0.0.1", ClsIPUtils.PRIVATE_ADDR_10_0_0_0_8)]
    [InlineData("10.255.255.254", ClsIPUtils.PRIVATE_ADDR_10_0_0_0_8)]
    [InlineData("172.16.0.1", ClsIPUtils.PRIVATE_ADDR_172_16_0_0_12)]
    [InlineData("172.31.255.254", ClsIPUtils.PRIVATE_ADDR_172_16_0_0_12)]
    [InlineData("192.168.0.1", ClsIPUtils.PRIVATE_ADDR_192_168_0_0_16)]
    [InlineData("192.168.255.254", ClsIPUtils.PRIVATE_ADDR_192_168_0_0_16)]
    [InlineData("169.254.1.1", ClsIPUtils.LINKLOCAL_ADDR)]
    [InlineData("224.0.0.1", ClsIPUtils.MULTICAST_ADDR)]
    [InlineData("239.255.255.250", ClsIPUtils.MULTICAST_ADDR)]
    [InlineData("127.0.0.1", ClsIPUtils.LOOPBACK_ADDR)]
    [InlineData("8.8.8.8", ClsIPUtils.GLOBAL_ADDR)]
    [InlineData("1.1.1.1", ClsIPUtils.GLOBAL_ADDR)]
    [InlineData("172.32.0.1", ClsIPUtils.GLOBAL_ADDR)]
    [InlineData("::1", ClsIPUtils.GLOBAL_ADDR)]
    [InlineData("invalid", ClsIPUtils.GLOBAL_ADDR)]
    [InlineData("", ClsIPUtils.GLOBAL_ADDR)]
    public void JudgePrivateIPv4Address_ShouldClassifyCorrectly(string ipAddress, int expectedCategory)
    {
        // Act
        int actual = _ipUtils.JudgePrivateIPv4Address(ipAddress);

        // Assert
        Assert.Equal(expectedCategory, actual);
    }

    #endregion

    #region JudgeIpVersion テスト

    [Theory]
    [InlineData("192.168.1.1", ClsIPUtils.IPV4)]
    [InlineData("10.0.0.1", ClsIPUtils.IPV4)]
    [InlineData("127.0.0.1", ClsIPUtils.IPV4)]
    [InlineData("0.0.0.0", ClsIPUtils.IPV4)]
    [InlineData("255.255.255.255", ClsIPUtils.IPV4)]
    [InlineData("::1", ClsIPUtils.IPV6)]
    [InlineData("fe80::1", ClsIPUtils.IPV6)]
    [InlineData("2001:db8::1", ClsIPUtils.IPV6)]
    [InlineData("::ffff:192.168.1.1", ClsIPUtils.IPV6)]
    [InlineData("invalid_ip", 0)]
    [InlineData("999.999.999.999", 0)]
    [InlineData("", 0)]
    [InlineData("   ", 0)]
    public void JudgeIpVersion_ShouldReturnCorrectVersion(string ipAddress, int expectedVersion)
    {
        // Act
        int actual = _ipUtils.JudgeIpVersion(ipAddress);

        // Assert
        Assert.Equal(expectedVersion, actual);
    }

    #endregion

    #region GetSubnetMask テスト

    [Theory]
    [InlineData("192.168.1.0/24", "255.255.255.0")]
    [InlineData("10.0.0.0/8", "255.0.0.0")]
    [InlineData("172.16.0.0/12", "255.240.0.0")]
    [InlineData("192.168.0.0/16", "255.255.0.0")]
    [InlineData("192.168.1.0/28", "255.255.255.240")]
    [InlineData("192.168.1.0/30", "255.255.255.252")]
    [InlineData("192.168.1.1/32", "255.255.255.255")]
    [InlineData("0.0.0.0/0", "0.0.0.0")]
    [InlineData("/24", "255.255.255.0")]
    [InlineData("/8", "255.0.0.0")]
    public void GetSubnetMask_ValidPrefix_ShouldReturnCorrectMask(string networkAddress, string expectedMask)
    {
        // Act
        string actual = _ipUtils.GetSubnetMask(networkAddress);

        // Assert
        Assert.Equal(expectedMask, actual);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("192.168.1.0")]
    [InlineData("192.168.1.0/")]
    [InlineData("192.168.1.0/33")]
    [InlineData("192.168.1.0/-1")]
    [InlineData("192.168.1.0/abc")]
    public void GetSubnetMask_InvalidPrefix_ShouldReturnEmpty(string networkAddress)
    {
        // Act
        string actual = _ipUtils.GetSubnetMask(networkAddress);

        // Assert
        Assert.Equal(string.Empty, actual);
    }

    #endregion

    #region 作業ディレクトリ検証

    [Fact]
    public void TempDirectory_ShouldBeUsableWithinTest()
    {
        // Arrange
        var testFilePath = Path.Combine(_tempDir, "test.txt");

        // Act
        File.WriteAllText(testFilePath, "ClsIPUtils Test");

        // Assert
        Assert.True(File.Exists(testFilePath));
        Assert.Equal("ClsIPUtils Test", File.ReadAllText(testFilePath));
    }

    #endregion
}
