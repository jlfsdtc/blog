package online.jlfsdtc.blog.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class Tools {
    private static final Random RANDOM = new Random();

    public static void copyFileUsingFileChannels(File source, File dest) throws IOException {
        try (FileChannel inputChannel = new FileInputStream(source).getChannel(); FileChannel outputChannel = new FileInputStream(dest).getChannel()) {
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        }
    }

    public static int rand(int min, int max) {
        return RANDOM.nextInt(max) % (max - min + 1) + min;
    }

    public static String fiowAutoShow(double value) {
        // Math.round 方法接收 float 和 double 类型,如果参数是 int 的话,会强转为 float,这个时候调用该方法无意义
        int kb = 1024;
        int mb = 1048576;
        int gb = 1073741824;
        double abs = Math.abs(value);
        if (abs > gb) {
            return Math.round(value / gb) + "GB";
        } else if (abs > mb) {
            return Math.round(value / mb) + "MB";
        } else if (abs > kb) {
            return Math.round(value / kb) + "KB";
        }
        return Math.round(value) + "";
    }

    /**
     * BASE64Encoder 加密
     *
     * @param data 加密数据
     * @param key  密钥
     * @return 加密后的数据
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String enAes(String data, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * BASE64Decoder 解密
     *
     * @param data 解密数据
     * @param key  密钥
     * @return 解密后的数据
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    public static String deAes(String data, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] cipherTextBytes = Base64.getDecoder().decode(data);
        byte[] decValue = cipher.doFinal(cipherTextBytes);
        return new String(decValue);
    }

    /**
     * 判断字符串是否为数字和有正确的值
     *
     * @param data 代判断的字符串
     * @return 是->true,否->false
     */
    public static boolean isNumber(String data) {
        return null != data && 0 != data.trim().length() && data.matches("\\d*");
    }
}
