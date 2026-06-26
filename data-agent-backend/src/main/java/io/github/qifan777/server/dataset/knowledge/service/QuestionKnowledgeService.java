package io.github.qifan777.server.dataset.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.qifan777.server.dataset.knowledge.domain.QuestionKnowledge;
import io.github.qifan777.server.dataset.knowledge.mapper.QuestionKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionKnowledgeService {

    private final QuestionKnowledgeMapper questionKnowledgeMapper;

    public List<QuestionKnowledge> findByDatabaseId(String databaseId) {
        return questionKnowledgeMapper.selectList(new LambdaQueryWrapper<QuestionKnowledge>()
                .eq(QuestionKnowledge::getDatabaseId, databaseId));
    }

    public List<QuestionKnowledge> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return questionKnowledgeMapper.selectBatchIds(ids);
    }
}
