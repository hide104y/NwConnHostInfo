using System;
using System.Net;
using System.Net.Sockets;

// 2026/08/15 Gemini 3.6 Flash (High) Review & Modified

namespace NwConnHostInfo.Class;

/// <summary>
/// IPアドレスおよびネットワークアドレスに関する判定・変換などのユーティリティ機能を提供するクラスです。
/// </summary>
/// <example>
/// <code>
/// var ipUtils = new ClsIPUtils();
/// bool inNetwork = ipUtils.IsIpInNetwork("192.168.1.10", "192.168.1.0/24");
/// int version = ipUtils.JudgeIpVersion("192.168.1.10");
/// </code>
/// </example>
public class ClsIPUtils
{
    /// <summary>
    /// IPv4 を表す定数値です。
    /// </summary>
    public const int IPV4 = 4;

    /// <summary>
    /// IPv6 を表す定数値です。
    /// </summary>
    public const int IPV6 = 6;

    /// <summary>
    /// グローバルアドレスを表す定数値です。
    /// </summary>
    public const int GLOBAL_ADDR = 0;

    /// <summary>
    /// クラスA プライベートアドレス範囲 (10.0.0.0/8) を表す定数値です。
    /// </summary>
    public const int PRIVATE_ADDR_10_0_0_0_8 = 110;

    /// <summary>
    /// クラスB プライベートアドレス範囲 (172.16.0.0/12) を表す定数値です。
    /// </summary>
    public const int PRIVATE_ADDR_172_16_0_0_12 = 120;

    /// <summary>
    /// クラスC プライベートアドレス範囲 (192.168.0.0/16) を表す定数値です。
    /// </summary>
    public const int PRIVATE_ADDR_192_168_0_0_16 = 130;

    /// <summary>
    /// リンクローカルアドレス範囲 (169.254.0.0/16) を表す定数値です。
    /// </summary>
    public const int LINKLOCAL_ADDR = 30;

    /// <summary>
    /// マルチキャストアドレス範囲 (224.0.0.0/4 等) を表す定数値です。
    /// </summary>
    public const int MULTICAST_ADDR = 20;

    /// <summary>
    /// ループバックアドレス範囲 (127.0.0.0/8) を表す定数値です。
    /// </summary>
    public const int LOOPBACK_ADDR = 10;

    /// <summary>
    /// <see cref="ClsIPUtils"/> クラスの新しいインスタンスを初期化します。
    /// </summary>
    /// <example>
    /// <code>
    /// var ipUtils = new ClsIPUtils();
    /// </code>
    /// </example>
    public ClsIPUtils()
    {
    }

    /// <summary>
    /// 指定されたIPアドレスが指定されたネットワークアドレス（CIDR表記）の範囲内に含まれているかを判定します。
    /// </summary>
    /// <param name="targetIpAddress">判定対象のIPアドレス文字列（例: "192.168.1.10"）</param>
    /// <param name="networkAddressWithPrefix">ネットワークアドレスとプレフィックス長（例: "192.168.1.0/24"）</param>
    /// <returns>ネットワーク範囲内に含まれる場合は <c>true</c>、それ以外または解析不能な場合は <c>false</c></returns>
    /// <example>
    /// <code>
    /// var ipUtils = new ClsIPUtils();
    /// bool result = ipUtils.IsIpInNetwork("192.168.1.50", "192.168.1.0/24"); // true
    /// </code>
    /// </example>
    public bool IsIpInNetwork(string targetIpAddress, string networkAddressWithPrefix)
    {
        if (string.IsNullOrWhiteSpace(targetIpAddress) || string.IsNullOrWhiteSpace(networkAddressWithPrefix))
        {
            return false;
        }

        if (IPNetwork.TryParse(networkAddressWithPrefix, out IPNetwork network) &&
            IPAddress.TryParse(targetIpAddress, out IPAddress? targetIp))
        {
            return network.Contains(targetIp);
        }

        return false;
    }

    /// <summary>
    /// 指定されたIPv4アドレスのアドレス種別（プライベートアドレス、リンクローカル、マルチキャスト、ループバック、グローバルアドレス等）を判定します。
    /// </summary>
    /// <param name="ipAddress">判定対象のIPv4アドレス文字列（例: "192.168.0.1"）</param>
    /// <returns>アドレス種別を表す定数（<see cref="PRIVATE_ADDR_10_0_0_0_8"/>, <see cref="PRIVATE_ADDR_172_16_0_0_12"/>, <see cref="PRIVATE_ADDR_192_168_0_0_16"/>, <see cref="LINKLOCAL_ADDR"/>, <see cref="MULTICAST_ADDR"/>, <see cref="LOOPBACK_ADDR"/>, <see cref="GLOBAL_ADDR"/>）</returns>
    /// <example>
    /// <code>
    /// var ipUtils = new ClsIPUtils();
    /// int addrType = ipUtils.JudgePrivateIPv4Address("192.168.1.1"); // ClsIPUtils.PRIVATE_ADDR_192_168_0_0_16 (130)
    /// </code>
    /// </example>
    public int JudgePrivateIPv4Address(string ipAddress)
    {
        if (string.IsNullOrWhiteSpace(ipAddress) || JudgeIpVersion(ipAddress) != IPV4)
        {
            return GLOBAL_ADDR;
        }

        if (IsIpInNetwork(ipAddress, "10.0.0.0/8"))
        {
            return PRIVATE_ADDR_10_0_0_0_8;
        }
        if (IsIpInNetwork(ipAddress, "172.16.0.0/12"))
        {
            return PRIVATE_ADDR_172_16_0_0_12;
        }
        if (IsIpInNetwork(ipAddress, "192.168.0.0/16"))
        {
            return PRIVATE_ADDR_192_168_0_0_16;
        }
        if (IsIpInNetwork(ipAddress, "169.254.0.0/16"))
        {
            return LINKLOCAL_ADDR;
        }
        if (IsIpInNetwork(ipAddress, "224.0.0.0/4") || IsIpInNetwork(ipAddress, "239.0.0.0/8"))
        {
            return MULTICAST_ADDR;
        }
        if (IsIpInNetwork(ipAddress, "127.0.0.0/8"))
        {
            return LOOPBACK_ADDR;
        }

        return GLOBAL_ADDR;
    }

    /// <summary>
    /// 指定されたIPアドレス文字列がIPv4かIPv6かを判定します。
    /// </summary>
    /// <param name="ipAddress">判定対象のIPアドレス文字列（例: "192.168.1.1" または "::1"）</param>
    /// <returns>IPv4の場合は <see cref="IPV4"/> (4)、IPv6の場合は <see cref="IPV6"/> (6)、それ以外の場合は 0</returns>
    /// <example>
    /// <code>
    /// var ipUtils = new ClsIPUtils();
    /// int version = ipUtils.JudgeIpVersion("192.168.1.1"); // 4
    /// </code>
    /// </example>
    public int JudgeIpVersion(string ipAddress)
    {
        if (IPAddress.TryParse(ipAddress, out IPAddress? address))
        {
            return address.AddressFamily switch
            {
                AddressFamily.InterNetwork => IPV4,
                AddressFamily.InterNetworkV6 => IPV6,
                _ => 0
            };
        }
        return 0;
    }

    /// <summary>
    /// 指定されたネットワークアドレス（CIDR表記）からサブネットマスク文字列を取得します。
    /// </summary>
    /// <param name="networkAddress">ネットワークアドレスとプレフィックス長（例: "192.168.1.0/24"）</param>
    /// <returns>ドットで区切られたサブネットマスク文字列（例: "255.255.255.0"）。取得に失敗した場合は空文字列</returns>
    /// <example>
    /// <code>
    /// var ipUtils = new ClsIPUtils();
    /// string mask = ipUtils.GetSubnetMask("192.168.1.0/24"); // "255.255.255.0"
    /// </code>
    /// </example>
    public string GetSubnetMask(string networkAddress)
    {
        if (string.IsNullOrWhiteSpace(networkAddress))
        {
            return string.Empty;
        }

        int slashIndex = networkAddress.IndexOf('/');
        if (slashIndex < 0 || slashIndex >= networkAddress.Length - 1)
        {
            return string.Empty;
        }

        ReadOnlySpan<char> prefixSpan = networkAddress.AsSpan(slashIndex + 1);
        if (int.TryParse(prefixSpan, out int maskBits) && maskBits is >= 0 and <= 32)
        {
            uint mask = maskBits == 0 ? 0 : uint.MaxValue << (32 - maskBits);
            byte b0 = (byte)((mask >> 24) & 0xFF);
            byte b1 = (byte)((mask >> 16) & 0xFF);
            byte b2 = (byte)((mask >> 8) & 0xFF);
            byte b3 = (byte)(mask & 0xFF);
            return $"{b0}.{b1}.{b2}.{b3}";
        }

        return string.Empty;
    }
}



