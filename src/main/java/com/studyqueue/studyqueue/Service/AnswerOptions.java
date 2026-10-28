package com.studyqueue.studyqueue.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class AnswerOptions {
    private AnswerOptions() {}

    // Null preserves all existing choices; an explicit count enables distractor pools.
    public static Map<String, String> present(Map<String, String> pool, String correctId, Integer displayCount, Random random) {
        if (!pool.containsKey(correctId)) throw new IllegalArgumentException("Missing correct option");
        if (displayCount != null && displayCount < 2) throw new IllegalArgumentException("At least two choices required");
        int count = displayCount == null ? pool.size() : Math.min(displayCount, pool.size());
        var distractors = new ArrayList<>(pool.keySet());
        distractors.remove(correctId);
        Collections.shuffle(distractors, random);
        var ids = new ArrayList<>(distractors.subList(0, Math.min(count - 1, distractors.size())));
        ids.add(correctId);
        Collections.shuffle(ids, random);
        Map<String, String> result = new LinkedHashMap<>();
        for (String id : ids) result.put(id, pool.get(id));
        return result;
    }
}
