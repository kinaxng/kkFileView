package cn.keking.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 轻量级全景图嗅探：仅读取图片头（JPEG SOF / PNG IHDR 等）拿到宽高比，
 * 不做完整解码。对 http(s) URL 使用 64KB Range 请求，避免下载整张全景图。
 *
 * 判定规则：宽高比落在 [1.98, 2.02]（即 2:1 ±1%）。
 * 结果按 URL 在内存中缓存 24h，避免重复嗅探。
 */
public final class PanoramaDetector {

    private static final Logger logger = LoggerFactory.getLogger(PanoramaDetector.class);

    private static final Pattern REMOTE = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE);
    private static final long CACHE_TTL_NANOS = TimeUnit.HOURS.toNanos(24);
    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private PanoramaDetector() {
    }

    /**
     * 判断给定的图片 URL 是否为 2:1 等距全景图。
     * 任何异常或不支持的格式都返回 false（fallback 到普通图片预览）。
     */
    public static boolean isPanorama(String url, String suffix) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String fmt = normalizeFormat(suffix);
        if (fmt == null) {
            return false;
        }

        CacheEntry cached = CACHE.get(url);
        if (cached != null && cached.valid()) {
            return cached.panorama;
        }

        boolean result = sniffAspect(url, fmt);
        CACHE.put(url, new CacheEntry(result));
        if (logger.isDebugEnabled()) {
            logger.debug("Panorama detection url={} fmt={} -> {}", url, fmt, result);
        }
        return result;
    }

    private static String normalizeFormat(String suffix) {
        if (suffix == null) {
            return null;
        }
        switch (suffix.toLowerCase(Locale.ROOT)) {
            case "jpg":
            case "jpeg":
                return "JPEG";
            case "png":
                return "PNG";
            case "gif":
                return "GIF";
            case "bmp":
                return "BMP";
            default:
                return null;
        }
    }

    private static boolean sniffAspect(String url, String format) {
        InputStream in = null;
        ImageInputStream iis = null;
        ImageReader reader = null;
        try {
            in = openStream(url);
            iis = ImageIO.createImageInputStream(in);
            if (iis == null) {
                return false;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
            if (!readers.hasNext()) {
                return false;
            }
            reader = readers.next();
            reader.setInput(iis);
            int w = reader.getWidth(0);
            int h = reader.getHeight(0);
            if (w <= 0 || h <= 0) {
                return false;
            }
            double ratio = (double) w / h;
            return ratio >= 1.98 && ratio <= 2.02;
        } catch (Exception e) {
            logger.warn("Panorama detection failed for {}: {}", url, e.getMessage());
            return false;
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (iis != null) {
                try {
                    iis.close();
                } catch (Exception ignored) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static InputStream openStream(String url) throws Exception {
        if (REMOTE.matcher(url).matches()) {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Range", "bytes=0-65535");
            conn.setRequestProperty("User-Agent", "kkFileView-Panorama/5.0.2");
            return conn.getInputStream();
        }
        return new URL(url).openStream();
    }

    private static final class CacheEntry {
        final boolean panorama;
        final long expiresAtNanos;

        CacheEntry(boolean panorama) {
            this.panorama = panorama;
            this.expiresAtNanos = System.nanoTime() + CACHE_TTL_NANOS;
        }

        boolean valid() {
            return System.nanoTime() < expiresAtNanos;
        }
    }
}
