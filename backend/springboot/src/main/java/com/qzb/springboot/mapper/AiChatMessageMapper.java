package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.AiChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiChatMessageMapper {

    List<AiChatMessage> listBySid(@Param("sid") String sid);

    AiChatMessage getById(@Param("id") String id);

    void insertBatch(@Param("messages") List<AiChatMessage> messages);

    void deleteBySid(@Param("sid") String sid);

    void deleteByIds(@Param("ids") List<String> ids);

    int countBySid(@Param("sid") String sid);
}
