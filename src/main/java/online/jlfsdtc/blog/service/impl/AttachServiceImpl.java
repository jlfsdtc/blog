package online.jlfsdtc.blog.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import online.jlfsdtc.blog.dao.AttachVoMapper;
import online.jlfsdtc.blog.model.vo.AttachVo;
import online.jlfsdtc.blog.model.vo.AttachVoExample;
import online.jlfsdtc.blog.service.IAttachService;
import online.jlfsdtc.blog.utils.DateKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by wangq on 2017/3/20.
 */
@Service
public class AttachServiceImpl implements IAttachService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttachServiceImpl.class);

    @Resource
    private AttachVoMapper attachDao;

    @Override
    public PageInfo<AttachVo> getAttachs(Integer page, Integer limit) {
        PageHelper.startPage(page, limit);
        AttachVoExample attachVoExample = new AttachVoExample();
        attachVoExample.setOrderByClause("id desc");
        List<AttachVo> attachVos = attachDao.selectByExample(attachVoExample);
        return new PageInfo<>(attachVos);
    }

    @Override
    public AttachVo selectById(Integer id) {
        if(null != id){
            return attachDao.selectByPrimaryKey(id);
        }
        return null;
    }

    @Override
    @Transactional
    public void save(String fname, String fkey, String ftype, Integer author) {
        AttachVo attach = new AttachVo();
        attach.setFname(fname);
        attach.setAuthorId(author);
        attach.setFkey(fkey);
        attach.setFtype(ftype);
        attach.setCreated(DateKit.getCurrentUnixTime());
        attachDao.insertSelective(attach);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        if (null != id) {
            attachDao.deleteByPrimaryKey( id);
        }
    }
}
