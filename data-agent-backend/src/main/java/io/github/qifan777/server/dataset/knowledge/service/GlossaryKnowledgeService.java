package io.github.qifan777.server.dataset.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.qifan777.server.dataset.knowledge.domain.GlossaryKnowledge;
import io.github.qifan777.server.dataset.knowledge.mapper.GlossaryKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlossaryKnowledgeService {

    private final GlossaryKnowledgeMapper glossaryKnowledgeMapper;

    public List<GlossaryKnowledge> findByDatabaseId(String databaseId) {
        return glossaryKnowledgeMapper.selectList(new LambdaQueryWrapper<GlossaryKnowledge>()
                .eq(GlossaryKnowledge::getDatabaseId, databaseId));
    }
}
