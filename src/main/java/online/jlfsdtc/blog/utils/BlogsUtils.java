package online.jlfsdtc.blog.utils;

import online.jlfsdtc.blog.constant.WebConst;
import online.jlfsdtc.blog.controller.AttachController;
import online.jlfsdtc.blog.model.vo.UserVo;
import org.apache.commons.lang3.StringUtils;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.awt.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

public class BlogsUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlogsUtils.class);

    private static final int ONE_MONTH = 30 * 24 * 60 * 60;

    /**
     * email pattern
     */
    private static final Pattern VALID_EMAIL_ADDRESS_REGEX = compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", CASE_INSENSITIVE);

    private static final Pattern SLUG_REGEX = compile("^[A-Za-z0-9_-]{5,100}$", CASE_INSENSITIVE);
    /**
     * 使用双重检查锁的单例方式需要添加 volatile 关键字
     */
    private static volatile DataSource newDataSource;
    /**
     * markdown解析器
     */
    private static Parser parser = Parser.builder().build();
    /**
     * 获取文件所在目录
     */
    private static String location = BlogsUtils.class.getClassLoader().getResource("").getPath();

    /**
     * 判断是否是邮箱
     *
     * @param emailStr email String
     * @return
     */
    public static boolean isEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }

    /**
     * @param fileName 获取jar外部的properties文件
     * @return 返回属性
     */
    private static Properties getPropFromFile(String fileName) {
        Properties properties = new Properties();
        try {
            InputStream resourceAsStream = new FileInputStream(location + fileName);
            properties.load(resourceAsStream);
        } catch (IOException e) {
            LOGGER.error("get properties file fail = {}", e.getMessage());
        }
        return properties;
    }

    /**
     * @param fileName 获取jar外部的yml文件
     * @return 返回属性
     */
    private static Map getYamlFromFile(String fileName) {
        Yaml yaml = new Yaml();
        Map map = null;
        try {
            InputStream resourceAsStream = new FileInputStream(location + fileName);
            map = yaml.load(resourceAsStream);
        } catch (FileNotFoundException e) {
            LOGGER.error("get yml file fail = {}", e.getMessage());
        }
        return map;
    }

    /**
     * md5加密
     *
     * @param source 数据源
     * @return 加密字符串
     */
    public static String md5Encode(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("get messageDigest fail = {}", e.getMessage());
        }
        byte[] encode = messageDigest.digest(source.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte anEncode :
                encode) {
            String hex = Integer.toHexString(0xff & anEncode);
            if (hex.length() == 1) {
                hexString.append("0");
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 获取新的数据源
     *
     * @return 数据源
     */
    public static DataSource getNewDataSource() {
        synchronized (BlogsUtils.class) {
            if (newDataSource == null) {
                Map activeMap = BlogsUtils.getYamlFromFile("application.yml");
                if (activeMap.isEmpty()) {
                    return newDataSource;
                }
                String activeStr = (String) ((Map) ((Map) activeMap.get("spring")).get("profiles")).get("active");
                Map map = BlogsUtils.getYamlFromFile("application-" + activeStr + ".yml");
                DriverManagerDataSource managerDataSource = new DriverManagerDataSource();
                managerDataSource.setUsername((String) ((Map) ((Map) map.get("spring")).get("datasource")).get("username"));
                managerDataSource.setPassword((String) ((Map) ((Map) map.get("spring")).get("datasource")).get("password"));
                managerDataSource.setDriverClassName((String) ((Map) ((Map) map.get("spring")).get("datasource")).get("driver-class-name"));
                managerDataSource.setUrl((String) ((Map) ((Map) map.get("spring")).get("datasource")).get("url"));
                newDataSource = managerDataSource;
            }
        }
        return newDataSource;
    }

    /**
     * 返回当前登录用户
     *
     * @param request HttpServletRequest
     * @return 返回当前登录用户
     */
    public static UserVo getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (null == session) {
            return null;
        }
        return (UserVo) session.getAttribute(WebConst.LOGIN_SESSION_KEY);
    }

    public static Integer getCookieUid(HttpServletRequest request) {
        if (null != request) {
            Cookie cookie = cookieRaw(WebConst.USER_IN_COOKIE, request);
            if (null != cookie && null != cookie.getValue()) {
                try {
                    String uid = Tools.deAes(cookie.getValue(), WebConst.AES_SALT);
                    return StringUtils.isNotBlank(uid) && Tools.isNumber(uid) ? Integer.valueOf(uid) : null;
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | UnsupportedEncodingException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
                    LOGGER.error("get CookieUid fail = {}", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 从cookies中获取指定cookie
     *
     * @param name    名称
     * @param request 请求
     * @return cookie
     */
    private static Cookie cookieRaw(String name, HttpServletRequest request) {
        Cookie[] servletCookies = request.getCookies();
        if (null == servletCookies) {
            return null;
        }
        for (Cookie cookie : servletCookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * 设置记住密码cookie
     *
     * @param response 应答
     * @param uid      uid
     */
    public static void setCookie(HttpServletResponse response, Integer uid) {
        try {
            String value = Tools.enAes(uid.toString(), WebConst.AES_SALT);
            Cookie cookie = new Cookie(WebConst.USER_IN_COOKIE, value);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 30);
            cookie.setSecure(false);
            response.addCookie(cookie);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException e) {
            LOGGER.error("set Cookie fail = {}", e.getMessage());
        }
    }

    /**
     * 提取html中的文字
     *
     * @param html html字符串
     * @return html中的文字
     */
    public static String htmlToText(String html) {
        return StringUtils.isNotBlank(html) ? html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ") : "";
    }

    /**
     * markdown转换为html
     *
     * @param markdown
     * @return
     */
    public static String mdToHtml(String markdown) {
        if (StringUtils.isBlank(markdown)) {
            return "";
        }
        List<Extension> extensions = Arrays.asList(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
        String content = renderer.render(document);
        content = Commons.emoji(content);
        return content;
    }

    /**
     * 退出登录状态
     *
     * @param session  session
     * @param response response
     */
    public static void logout(HttpSession session, HttpServletResponse response) {
        session.removeAttribute(WebConst.LOGIN_SESSION_KEY);
        Cookie cookie = new Cookie(WebConst.USER_IN_COOKIE, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        try {
            response.sendRedirect(Commons.site_url());
        } catch (IOException e) {
            LOGGER.error("logout fail = {}", e.getMessage());
        }
    }

    /**
     * 替换HTML脚本
     *
     * @param value
     * @return
     */
    public static String cleanXSS(String value) {
        //You'll need to remove the spaces from the html entities below
        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
        value = value.replaceAll("'", "&#39;");
        value = value.replaceAll("eval\\((.*)\\)", "");
        value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
        value = value.replaceAll("script", "");
        return value;
    }

    /**
     * 过滤XSS注入
     *
     * @param value
     * @return
     */
    public static String filterXSS(String value) {
        String cleanValue = null;
        if (value != null) {
            cleanValue = Normalizer.normalize(value, Normalizer.Form.NFD);
            // Avoid null characters
            cleanValue = cleanValue.replaceAll("\0", "");

            // Avoid anything between script tags
            Pattern scriptPattern = compile("<script>(.*?)</script>", CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid anything in a src='...' type of expression
            scriptPattern = compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", CASE_INSENSITIVE | MULTILINE | DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            scriptPattern = compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", CASE_INSENSITIVE | MULTILINE | DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Remove any lonesome </script> tag
            scriptPattern = compile("</script>", CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Remove any lonesome <script ...> tag
            scriptPattern = compile("<script(.*?)>", CASE_INSENSITIVE | MULTILINE | DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid eval(...) expressions
            scriptPattern = compile("eval\\((.*?)\\)", CASE_INSENSITIVE | MULTILINE | DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid expression(...) expressions
            scriptPattern = compile("expression\\((.*?)\\)", CASE_INSENSITIVE | MULTILINE | DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid javascript:... expressions
            scriptPattern = compile("javascript:", CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid vbscript:... expressions
            scriptPattern = compile("vbscript:", CASE_INSENSITIVE);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");

            // Avoid onload= expressions
            scriptPattern = compile("onload(.*?)=", CASE_INSENSITIVE | MULTILINE | DOTALL);
            cleanValue = scriptPattern.matcher(cleanValue).replaceAll("");
        }
        return cleanValue;
    }

    /**
     * 判断是否是合法路径
     *
     * @param slug
     * @return
     */
    public static boolean isPath(String slug) {
        if (StringUtils.isNotBlank(slug)) {
            if (slug.contains("/") || slug.contains(" ") || slug.contains(".")) {
                return false;
            }
            Matcher matcher = SLUG_REGEX.matcher(slug);
            return matcher.find();
        }
        return false;
    }

    public static String getFileKey(String name) {
        String prefix = "/upload/" + DateKit.dateFormat(new Date(), "yyyy/MM");
        if (!new File(AttachController.CLASSPATH + prefix).exists()) {
            new File(AttachController.CLASSPATH + prefix).mkdirs();
        }

        name = StringUtils.trimToNull(name);
        if (name == null) {
            return prefix + "/" + BaseUUID.uu32() + "." + null;
        } else {
            name = name.replace('\\', '/');
            name = name.substring(name.lastIndexOf("/") + 1);
            int index = name.lastIndexOf(".");
            String ext = null;
            if (index >= 0) {
                ext = StringUtils.trimToNull(name.substring(index + 1));
            }
            return prefix + "/" + BaseUUID.uu32() + "." + (ext == null ? null : (ext));
        }
    }

    /**
     * 判断文件是否是图片类型
     *
     * @param imageFile
     * @return
     */
    public static boolean isImage(InputStream imageFile) {
        try {
            Image img = ImageIO.read(imageFile);
            if (img == null || img.getWidth(null) <= 0 || img.getHeight(null) <= 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 随机数
     *
     * @param size
     * @return
     */
    public static String getRandomNumber(int size) {
        String num = "";

        for (int i = 0; i < size; ++i) {
            double a = Math.random() * 9.0D;
            a = Math.ceil(a);
            int randomNum = (new Double(a)).intValue();
            num = num + randomNum;
        }

        return num;
    }

    /**
     * 获取保存文件的位置,jar所在目录的路径
     *
     * @return
     */
    public static String getUploadFilePath() {
        String path = BlogsUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.substring(1, path.length());
        try {
            path = java.net.URLDecoder.decode(path, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int lastIndex = path.lastIndexOf("/") + 1;
        path = path.substring(0, lastIndex);
        File file = new File("");
        return file.getAbsolutePath() + "/";
    }

    public static void main(String[] args) {
        LOGGER.info(getNewDataSource().toString());
    }
}
