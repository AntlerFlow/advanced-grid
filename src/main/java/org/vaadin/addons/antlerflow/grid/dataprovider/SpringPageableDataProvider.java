package org.vaadin.addons.antlerflow.grid.dataprovider;

import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A data provider for Vaadin that integrates with Spring Data's a pageable mechanism to retrieve
 * and cache paginated data through a backend service.
 *
 * @param <T> the type of the data items to provide
 * @param <F> the type of the filter used for querying the data
 */
@Slf4j
public class SpringPageableDataProvider<T, F> extends AbstractBackEndDataProvider<T, F> {

    private final BiFunction<Optional<F>, Pageable, Page<T>> dataSupplier;

    /**
     * This provider effectively caches a single "last query" result. A single-entry cache is
     * simpler than a Map and matches the current behavior (clear on new key).
     */
    private volatile CacheEntry<T> cache;

    /**
     * Cache for total count, keyed by filter + sort only (ignores paging), so sizeInBackEnd won't
     * refetch when only pageSize/limit changes.
     */
    private volatile CountCacheEntry countCache;

    private volatile F filter;

    public SpringPageableDataProvider(BiFunction<Optional<F>, Pageable, Page<T>> dataSupplier) {
        this.dataSupplier = Objects.requireNonNull(dataSupplier, "dataSupplier");
    }

    public F getFilter() {
        return filter;
    }

    public void setFilter(F filter) {
        this.filter = filter;
        clearCache();
    }

    @Override
    protected Stream<T> fetchFromBackEnd(Query<T, F> query) {
        return getCacheableResult(query).getContent().stream();
    }

    @Override
    protected int sizeInBackEnd(Query<T, F> query) {
        long total = getCachedTotalElements(query);
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    @Override
    public void refreshAll() {
        clearCache();
        super.refreshAll();
    }

    private void clearCache() {
        cache = null;
        countCache = null;
    }

    private long getCachedTotalElements(Query<T, F> query) {
        final String countKey = generateCountCacheKey(query);

        CountCacheEntry local = countCache;
        if (local != null && local.key().equals(countKey)) {
            log.debug("Loading total count from cache");
            return local.totalElements();
        }

        log.debug("Fetching total count from backend");
        long total = getCacheableResult(query).getTotalElements();

        countCache = new CountCacheEntry(countKey, total);
        return total;
    }

    private Page<T> getCacheableResult(Query<T, F> query) {
        final String key = generateCacheKey(query);

        CacheEntry<T> local = cache;
        if (local != null && local.key().equals(key)) {
            log.debug("Loading data from cache");
            return local.page();
        }

        log.debug("Fetching data from backend");
        Page<T> page = dataSupplier.apply(getEffectiveFilter(query), toSpringPageable(query));
        cache = new CacheEntry<>(key, page);

        // Opportunistically populate the count cache too (same filter/sort, regardless of paging)
        long total = page == null ? 0L : page.getTotalElements();
        countCache = new CountCacheEntry(generateCountCacheKey(query), total);

        return page;
    }

    private Optional<F> getEffectiveFilter(Query<T, F> query) {
        // Query filter wins, otherwise fall back to provider-level filter.
        return Optional.ofNullable(query.getFilter().orElse(filter));
    }

    private Pageable toSpringPageable(Query<T, F> query) {
        return VaadinSpringDataHelpers.toSpringPageRequest(query);
    }

    private String generateCacheKey(Query<T, F> query) {
        String rawKey =
                "p="
                        + query.getPage()
                        + ";ps="
                        + query.getPageSize()
                        + ";f="
                        + stableString(getEffectiveFilter(query).orElse(null))
                        + ";sort="
                        + stableSortString(query.getSortOrders());

        return hashToUrlSafeBase64(rawKey);
    }

    private String generateCountCacheKey(Query<T, F> query) {
        // Ignore paging so sizeInBackEnd isn't invalidated by page/pageSize changes.
        String rawKey =
                "f="
                        + stableString(getEffectiveFilter(query).orElse(null))
                        + ";sort="
                        + stableSortString(query.getSortOrders());

        return hashToUrlSafeBase64(rawKey);
    }

    protected String stableSortString(List<QuerySortOrder> sortOrders) {
        if (sortOrders == null || sortOrders.isEmpty()) {
            return "";
        }
        return sortOrders.stream()
                .map(so -> so.getSorted() + ":" + so.getDirection())
                .collect(Collectors.joining(","));
    }

    protected String stableString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String hashToUrlSafeBase64(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            // Fallback to the raw (still deterministic) key if hashing is unavailable.
            return input;
        }
    }

    private record CacheEntry<T>(String key, Page<T> page) {
        private CacheEntry {
            Objects.requireNonNull(key, "key");
            page = page == null ? Page.empty() : page;
        }
    }

    private record CountCacheEntry(String key, long totalElements) {
        private CountCacheEntry {
            Objects.requireNonNull(key, "key");
        }
    }
}
