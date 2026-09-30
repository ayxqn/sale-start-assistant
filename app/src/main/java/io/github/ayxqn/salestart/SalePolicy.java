package io.github.ayxqn.salestart;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Pure policy code; independent of Android, accounts and network calls. */
public final class SalePolicy {
    public static final long MAX_WAIT_MS = 2 * 60 * 60 * 1000L;
    public static final long CLOCK_DRIFT_LIMIT_MS = 2000L;
    private SalePolicy() {}

    public static String validateLink(String input) {
        if (input == null || input.length() > 1024) throw new IllegalArgumentException("链接太长或没有填写。");
        try {
            URI uri = new URI(input.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getRawUserInfo() != null || uri.getPort() != -1)
                throw new IllegalArgumentException("请使用 B 站官方 HTTPS 活动链接。");
            host = host.toLowerCase(Locale.ROOT);
            if (!host.equals("www.bilibili.com") && !host.equals("m.bilibili.com"))
                throw new IllegalArgumentException("目前只支持 www.bilibili.com 和 m.bilibili.com 的活动页。");
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/blackboard/") || path.contains("%") || path.contains("\\") || path.contains("..") || path.contains("//"))
                throw new IllegalArgumentException("请填写 /blackboard/ 开头的官方活动页链接。");
            if (uri.getRawQuery() != null || uri.getRawFragment() != null)
                throw new IllegalArgumentException("请去掉链接中问号或井号之后的分享参数，别填收银台或带账号信息的链接。");
            return "https://" + host + path;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("链接格式不对，请重新复制官方活动页地址。");
        }
    }

    public static void validateTime(long target, long now) {
        long wait = target - now;
        if (wait < 1000L) throw new IllegalArgumentException("开售时间已到或太近了，请直接打开活动页。");
        if (wait > MAX_WAIT_MS) throw new IllegalArgumentException("请在开售前两小时内启动。长时间亮屏会耗电。");
    }

    public static long remaining(long target, long anchorWall, long anchorMono, long monoNow) {
        return Math.max(0L, target - anchorWall - (monoNow - anchorMono));
    }

    public static boolean clockChanged(long wallNow, long anchorWall, long elapsedMono) {
        return Math.abs(wallNow - anchorWall - elapsedMono) > CLOCK_DRIFT_LIMIT_MS;
    }
}
