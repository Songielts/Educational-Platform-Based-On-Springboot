package com.smartedu.modules.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.modules.course.entity.Video;
import java.util.List;

public interface VideoService extends IService<Video> {
    List<Video> getVideosByChapterId(Long chapterId);
}