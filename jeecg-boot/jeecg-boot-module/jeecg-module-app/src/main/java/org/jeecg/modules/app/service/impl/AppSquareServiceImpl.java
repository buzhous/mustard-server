package org.jeecg.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.app.entity.Topic;
import org.jeecg.modules.app.bean.vo.fields.FieldElementVO;
import org.jeecg.modules.app.bean.vo.square.ItemTopicVO;
import org.jeecg.modules.app.mapper.TopicMapper;
import org.jeecg.modules.app.service.IAppSquareService;
import org.jeecg.modules.app.service.ITopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
public class AppSquareServiceImpl extends ServiceImpl<TopicMapper, Topic> implements IAppSquareService {

    @Resource
    private RedisUtil redisUtil;

    @Autowired
    private ITopicService topicService;

    @Override
    public IPage<ItemTopicVO> getTopicPage(String searchKey, Integer page, Integer size) {
        // 创建分页对象，仅返回启用主题（0启用 1停用），热门优先、创建时间升序
        IPage<Topic> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Topic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Topic::getTopicStatus, 0);
        if (StrUtil.isNotEmpty(searchKey)) {
            queryWrapper.and(wrapper -> wrapper.like(Topic::getTopicDesc, searchKey)
                    .or().like(Topic::getTopicName, searchKey));
        }
        queryWrapper.orderByDesc(Topic::getIsHot)
                .orderByAsc(Topic::getCreateTime);

        // 执行分页查询
        IPage<Topic> iPage = topicService.page(pageObj, queryWrapper);

        // 重新处理分页，并返回ItemTopicVO
        List<ItemTopicVO> topicList = new ArrayList<>();
        iPage.getRecords().forEach(item -> {
            // fields 为 JSON 字符串，无法由 hutool 直接转为 List，拷贝时忽略、下方手动解析
            ItemTopicVO itemTopicVO = BeanUtil.copyProperties(item, ItemTopicVO.class, "fields");
            // 解析字段元素（真实配置，按 sort 升序）
            itemTopicVO.setFields(parseFields(item.getFields()));
            // 填充默认值
            itemTopicVO.setTopicIcon(item.getTopicIcon() != null ? item.getTopicIcon() : "");
            itemTopicVO.setTopicName(item.getTopicName() != null ? item.getTopicName() : "");
            itemTopicVO.setTopicDesc(item.getTopicDesc() != null ? item.getTopicDesc() : "");

            topicList.add(itemTopicVO);
        });

        IPage<ItemTopicVO> topicPage = new Page<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal());
        topicPage.setRecords(topicList);
        topicPage.setPages(iPage.getPages());

        return topicPage;
    }

    private List<FieldElementVO> parseFields(String fieldsJson) {
        if (StrUtil.isBlank(fieldsJson)) {
            return new ArrayList<>();
        }
        try {
            List<FieldElementVO> elements = JSON.parseArray(fieldsJson, FieldElementVO.class);
            if (elements == null) {
                return new ArrayList<>();
            }
            elements.sort(Comparator.comparing(
                    FieldElementVO::getSort,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            return elements;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}