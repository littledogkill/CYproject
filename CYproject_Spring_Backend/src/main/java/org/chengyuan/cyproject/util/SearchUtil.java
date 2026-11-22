package org.chengyuan.cyproject.util;

import org.chengyuan.cyproject.entity.WorkEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearchUtil {

    public List<WorkEntity> doSearch(List<WorkEntity> workEntities, String q) {
        if (workEntities == null || workEntities.isEmpty() || q == null || q.isBlank()) {
            return workEntities == null ? List.of() : workEntities;
        }
        String keyword = q.trim().toLowerCase();
        List<WorkEntity> result = new ArrayList<>();
        for (WorkEntity work : workEntities) {
            String topic = work.getWorktopic() == null ? "" : work.getWorktopic().toLowerCase();
            String body = work.getBody() == null ? "" : work.getBody().toLowerCase();
            if (topic.contains(keyword) || body.contains(keyword)) {
                result.add(work);
            }
        }
        return result;
    }
}
