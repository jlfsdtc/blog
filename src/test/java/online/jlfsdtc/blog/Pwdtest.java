package online.jlfsdtc.blog;

import online.jlfsdtc.blog.model.vo.UserVo;
import online.jlfsdtc.blog.utils.BlogsUtils;

/**
 * Created by 13 on 2017/4/2.
 */
public class Pwdtest {
    public static void main(String args[]){
        UserVo user = new UserVo();
        user.setUsername("admin");
        user.setPassword("mysql");
        String encodePwd = BlogsUtils.md5Encode(user.getUsername() + user.getPassword());
        System.out.println(encodePwd);
    }
}
