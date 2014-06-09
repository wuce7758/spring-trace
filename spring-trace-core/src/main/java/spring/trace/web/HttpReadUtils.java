package spring.trace.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * User: HolyEyE
 * Date: 13. 8. 29. Time: 오후 9:58
 */

public class HttpReadUtils {

    private static Logger log = LoggerFactory.getLogger(HttpReadUtils.class);

    public final static String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

    public static final Set<String> MULTI_READ_HTTP_METHODS = new HashSet<String>() {{
        add("PUT");
        add("POST");
    }};

    public static final Set<String> SPRING_MOCK_REQUESTS = new HashSet<String>() {{
        add("MockHttpServletRequest"); //spring 3.x
        add("Servlet3MockHttpServletRequest"); //spring 4.x
    }};

    public static String getHttpBody(HttpServletRequest request, String encoding) {

        if (!isReadableHttpBody(request.getMethod())) return null;

        if (isFormUrlencoded(request)) return readParameters(request);
        if (isSpringMVCMockTest(request)) return readBody(request, encoding);

        if (!(request instanceof MultiReadHttpServletRequest)) {
            log.warn("MultiReadHttpServletRequest 타입이 아닙니다. 필터를 설정하세요. requestType={} URL={}", request.getClass(), getRequestURLWithQueryString(request));
            return null;
        }

        return readBody(request, encoding);
    }


    public static boolean isReadableHttpBody(String method) {
        return MULTI_READ_HTTP_METHODS.contains(method);
    }

    private static boolean isFormUrlencoded(HttpServletRequest request) {
        return request.getContentType() != null && request.getContentType().equals(APPLICATION_FORM_URLENCODED_VALUE);
    }

    private static boolean isSpringMVCMockTest(HttpServletRequest request) {
        return SPRING_MOCK_REQUESTS.contains(request.getClass().getSimpleName());
    }

    private static String readParameters(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        Iterator<Map.Entry<String, String[]>> iterator = request.getParameterMap().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String[]> entry = iterator.next();

            String[] value = entry.getValue();
            for (int i = 0; i < value.length; i++) {
                sb.append(entry.getKey()).append("=").append(value[i]);
                if (i < value.length - 1) {
                    sb.append("&");
                }
            }

            if (iterator.hasNext()) {
                sb.append("&");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String getRequestURLWithQueryString(HttpServletRequest request) {

        StringBuilder sb = new StringBuilder(128);

        sb.append(request.getRequestURL());
        if (null != request.getQueryString()) {
            sb.append("?");
            sb.append(request.getQueryString());
        }
        return sb.toString();
    }

    private static String readBody(HttpServletRequest request, String encoding) {
        try {
            if (request.getInputStream() == null) return null;

            return toString(new InputStreamReader(request.getInputStream(), Charset.forName(encoding)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String toString(Readable r) throws IOException {
        return toStringBuilder(r).toString();
    }

    private static StringBuilder toStringBuilder(Readable r) throws IOException {
        StringBuilder sb = new StringBuilder();
        copy(r, sb);
        return sb;
    }

    private static final int BUF_SIZE = 0x800; // 2K chars (4K bytes)
    public static long copy(Readable from, Appendable to) throws IOException {

        Assert.notNull(from);
        Assert.notNull(to);

        CharBuffer buf = CharBuffer.allocate(BUF_SIZE);
        long total = 0;
        while (from.read(buf) != -1) {
            buf.flip();
            to.append(buf);
            total += buf.remaining();
            buf.clear();
        }
        return total;
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        Assert.notNull(from);
        Assert.notNull(to);
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

}