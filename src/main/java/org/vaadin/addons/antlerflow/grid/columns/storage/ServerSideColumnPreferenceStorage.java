package org.vaadin.addons.antlerflow.grid.columns.storage;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class for storages that keep preferences on the server, per user (a database, a cache, a
 * REST service). Subclasses implement three synchronous methods; this class resolves the current
 * user and turns failures into a logged warning so the grid falls back to its default columns.
 *
 * <p>When the user id supplier returns {@code null} or blank (an anonymous visitor), nothing is
 * read or written.
 */
public abstract class ServerSideColumnPreferenceStorage implements ColumnPreferenceStorage {

    private static final Logger log =
            LoggerFactory.getLogger(ServerSideColumnPreferenceStorage.class);

    private final SerializableSupplier<String> userIdSupplier;

    /**
     * @param userIdSupplier returns the current user's stable id (for example the login name);
     *     called on the UI thread while handling a request
     */
    protected ServerSideColumnPreferenceStorage(SerializableSupplier<String> userIdSupplier) {
        this.userIdSupplier = Objects.requireNonNull(userIdSupplier, "userIdSupplier");
    }

    /**
     * Returns a user id supplier reading the authenticated principal of the current Vaadin request
     * ({@code HttpServletRequest#getUserPrincipal()}), which Spring Security and servlet container
     * security both populate. Anonymous requests yield {@code null}.
     */
    public static SerializableSupplier<String> requestPrincipalName() {
        return () ->
                Optional.ofNullable(VaadinRequest.getCurrent())
                        .map(VaadinRequest::getUserPrincipal)
                        .map(Principal::getName)
                        .orElse(null);
    }

    /** Reads the stored preference; empty when none is stored. */
    protected abstract Optional<Map<String, Boolean>> read(String userId, String key);

    /** Inserts or replaces the preference. */
    protected abstract void write(String userId, String key, Map<String, Boolean> visibility);

    /** Deletes the preference; a missing preference is not an error. */
    protected abstract void delete(String userId, String key);

    @Override
    public final void load(
            ColumnPreferenceContext context, SerializableConsumer<Map<String, Boolean>> onLoaded) {
        Map<String, Boolean> visibility = Map.of();
        Optional<String> userId = currentUserId();
        if (userId.isPresent()) {
            try {
                visibility = read(userId.get(), context.key()).orElse(Map.of());
            } catch (RuntimeException ex) {
                log.warn(
                        "column_preference_load_failed key={} storage={}",
                        context.key(),
                        getClass().getSimpleName(),
                        ex);
            }
        }
        onLoaded.accept(visibility);
    }

    @Override
    public final void save(ColumnPreferenceContext context, Map<String, Boolean> visibility) {
        currentUserId()
                .ifPresent(
                        userId ->
                                runLogged(
                                        "column_preference_save_failed",
                                        context,
                                        () -> write(userId, context.key(), visibility)));
    }

    @Override
    public final void clear(ColumnPreferenceContext context) {
        currentUserId()
                .ifPresent(
                        userId ->
                                runLogged(
                                        "column_preference_clear_failed",
                                        context,
                                        () -> delete(userId, context.key())));
    }

    private Optional<String> currentUserId() {
        return Optional.ofNullable(userIdSupplier.get()).filter(id -> !id.isBlank());
    }

    private void runLogged(String event, ColumnPreferenceContext context, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("{} key={} storage={}", event, context.key(), getClass().getSimpleName(), ex);
        }
    }
}
