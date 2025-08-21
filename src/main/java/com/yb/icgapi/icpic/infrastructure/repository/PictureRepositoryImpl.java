package com.yb.icgapi.icpic.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.icpic.domain.picture.repository.PictureRepository;
import com.yb.icgapi.icpic.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
