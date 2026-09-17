package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.AiChatSession;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiChatSessionMapper {

    AiChatSession getByIdAndUid(@Param("id") String id, @Param("uid") String uid);

    List<AiChatSession> listByUid(@Param("uid") String uid);

    void insert(AiChatSession session);

    int softDelete(@Param("id") String id, @Param("uid") String uid);
}
