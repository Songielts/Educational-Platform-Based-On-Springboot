package com.smartedu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("edu_video")
public class Video {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long chapterId;
    private Long courseId;
    private String videoName;
    private String videoUrl;
    private Integer videoDuration;
    private Long videoSize;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}