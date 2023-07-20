package course.concurrency.m3_shared.collections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class RestaurantService {

    private Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>() {{
        put("A", new Restaurant("A"));
        put("B", new Restaurant("B"));
        put("C", new Restaurant("C"));
    }};

    private final Map<String, LongAdder> stat = new ConcurrentHashMap<>();
//            Collections.unmodifiableMap(restaurantMap.keySet().stream()
//            .collect(Collectors.toMap(key -> key, key -> new LongAdder())));

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public void addToStat(String restaurantName) {
        stat.computeIfAbsent(restaurantName, k -> new LongAdder()).increment();
//        stat.get(restaurantName).increment();
    }

    public Set<String> printStat() {
        return stat.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue().longValue())
                .collect(Collectors.toUnmodifiableSet());
    }
}
