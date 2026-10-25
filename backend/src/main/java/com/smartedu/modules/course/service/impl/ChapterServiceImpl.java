package com.smartedu.modules.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.modules.course.entity.Chapter;
import com.smartedu.modules.course.mapper.ChapterMapper;
import com.smartedu.modules.course.service.ChapterService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    @Override
    public List<Chapter> getChaptersByCourseId(Long courseId) {
        // 使用 Mapper 中的自定义查询
        return baseMapper.selectByCourseId(courseId);
    }
}