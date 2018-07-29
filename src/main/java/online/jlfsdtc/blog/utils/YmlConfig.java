package online.jlfsdtc.blog.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class YmlConfig {
    public static void main(String[] args) {
        ClassLoader classLoader = YmlConfig.class.getClassLoader();
        URL resource = classLoader.getResource("application.yml");
        String path = resource.getPath();
        System.out.println(path);
        try {
            Yaml yaml = new Yaml();
            //获取test.yaml文件中的配置数据，然后转换为obj，
            Object obj = yaml.load(new FileInputStream(path));
            System.out.println(obj);
            //也可以将值转换为Map
            Map map = yaml.load(new FileInputStream(YmlConfig.class.getClassLoader().getResource("application.yml").getPath()));
            System.out.println(map);
            //通过map我们取值就可以了.
            System.out.println(map.get("spring"));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
