package com.smartedu.modules.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.modules.course.entity.Video;
import com.smartedu.modules.course.mapper.VideoMapper;
import com.smartedu.modules.course.service.VideoService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    @Override
    public List<Video> getVideosByChapterId(Long chapterId) {
        return lambdaQuery()
                .eq(Video::getChapterId, chapterId)
                .orderByAsc(Video::getSort)
                .list();
    }
}