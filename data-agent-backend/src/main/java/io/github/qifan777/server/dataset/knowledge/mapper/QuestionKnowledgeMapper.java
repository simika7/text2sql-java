package io.github.qifan777.server.dataset.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qifan777.server.dataset.knowledge.domain.QuestionKnowledge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionKnowledgeMapper extends BaseMapper<QuestionKnowledge> {
}
