package com.finsphere.frontend.web.support;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.finsphere.frontend.client.BackendCallContext;
import com.finsphere.frontend.client.BackendCallContextResolver;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.BackendUnavailableException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Loads independent sections of a page in parallel on virtual threads, so a dashboard that needs four back-end
 * calls waits for the slowest one instead of the sum of all four. Each task runs with the browser request's
 * {@link BackendCallContext} and logging context. A failed section becomes a {@link Loaded} with a message; an
 * expired session is rethrown so the page redirects to sign-in.
 */
@Component
public class SectionLoader implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SectionLoader.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final BackendCallContextResolver contextResolver;

    public SectionLoader(BackendCallContextResolver contextResolver) {
        this.contextResolver = contextResolver;
    }

    public <T> Pending<T> start(String section, Supplier<T> call) {
        BackendCallContext context = this.contextResolver.current();
        Map<String, String> logContext = MDC.getCopyOfContextMap();
        Future<T> future = this.executor.submit(() -> {
            if (logContext != null) {
                MDC.setContextMap(logContext);
            }
            try {
                return ScopedValue.where(BackendCallContext.CURRENT, context).call(call::get);
            }
            finally {
                MDC.clear();
            }
        });
        return new Pending<>(section, future);
    }

    @Override
    public void destroy() {
        this.executor.shutdownNow();
    }

    /** A section being loaded. */
    public static final class Pending<T> {

        private final String section;

        private final Future<T> future;

        Pending(String section, Future<T> future) {
            this.section = section;
            this.future = future;
        }

        public Loaded<T> join() {
            try {
                return Loaded.of(this.future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            }
            catch (ExecutionException ex) {
                return failure(ex.getCause());
            }
            catch (TimeoutException ex) {
                this.future.cancel(true);
                log.warn("Loading {} took longer than {}", this.section, TIMEOUT);
                return Loaded.failed("Loading " + this.section + " is taking too long. Refresh to try again.");
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Loaded.failed("Loading " + this.section + " was interrupted.");
            }
        }

        private Loaded<T> failure(Throwable cause) {
            if (cause instanceof BackendException backend) {
                if (backend.is(HttpStatus.UNAUTHORIZED)) {
                    throw backend;
                }
                log.warn("Could not load {}: {} {}", this.section, backend.getStatus(), backend.getMessage());
                return Loaded.failed(backend.userMessage());
            }
            if (cause instanceof BackendUnavailableException unavailable) {
                log.warn("Could not load {}: {}", this.section, unavailable.getMessage());
                return Loaded.failed("We could not reach the " + Services.describe(unavailable.getService())
                        + " to load " + this.section + ". Refresh to try again.");
            }
            log.error("Unexpected error while loading {}", this.section, cause);
            return Loaded.failed("Something went wrong while loading " + this.section + ".");
        }
    }
}
