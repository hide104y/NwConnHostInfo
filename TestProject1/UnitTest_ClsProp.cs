using System;
using System.IO;
using NwConnHostInfo.Class;
using Xunit;

namespace TestProject1;

public class UnitTest_ClsProp : IDisposable
{
    private readonly string _tempDir;

    public UnitTest_ClsProp()
    {
        // 注意事項に基づいた作業ディレクトリの設定
        _tempDir = Path.Combine(Path.GetTempPath(), @"UnitTest", "NwConnHostInfo", "ClsProp");
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

    [Fact]
    public void DefaultValues_ShouldBeInitializedCorrectly()
    {
        // Arrange & Act
        var prop = new ClsProp();

        // Assert
        Assert.Equal(0, prop.Pid);
        Assert.Equal("-", prop.AppName);
        Assert.Equal("-", prop.AppPath);
    }

    [Fact]
    public void PropertySetters_ShouldUpdateValues()
    {
        // Arrange
        var prop = new ClsProp();

        // Act
        prop.Pid = 1234;
        prop.AppName = "test.exe";
        prop.AppPath = @"C:\Test\test.exe";

        // Assert
        Assert.Equal(1234, prop.Pid);
        Assert.Equal("test.exe", prop.AppName);
        Assert.Equal(@"C:\Test\test.exe", prop.AppPath);
    }

    [Fact]
    public void TempDirectory_ShouldBeUsableWithinTest()
    {
        // Arrange
        var testFilePath = Path.Combine(_tempDir, "test.txt");

        // Act
        File.WriteAllText(testFilePath, "ClsProp Test");

        // Assert
        Assert.True(File.Exists(testFilePath));
        Assert.Equal("ClsProp Test", File.ReadAllText(testFilePath));
    }
}
