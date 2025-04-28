package ru.hpclab.hl.additional.cache;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.hpclab.hl.additional.model.Visitor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class VisitorCache {
    private final Map<UUID, Visitor> cache = new HashMap<>();
    private Long hits = 0L;
    private Long misses = 0L;

    public Visitor get(UUID visitorID) {
        Visitor visitor = cache.get(visitorID);
        if (visitor != null) {
            hits++;
            return visitor;
        }
        misses++;
        return null;
    }

    public void put(UUID visitorID, Visitor visitor) {
        cache.put(visitorID, visitor);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public void printStatus() {
        System.out.println("Visitor Cache Status:");
        System.out.println("Size: " + size());
    }
}
