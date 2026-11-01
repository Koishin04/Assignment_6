package social;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class ProducerRunner {
    private final ExecutorService executor;

    ProducerRunner(ExecutorService executor) {
        this.executor = executor;
    }

    void run(List<Runnable> producers) throws Exception {
        List<Future<?>> results = new ArrayList<>();

        try {
            for (Runnable producer : producers) {
                results.add(executor.submit(producer));
            }

            executor.shutdown();

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();

                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Executor did not terminate after cancellation");
                }

                throw new IllegalStateException("Producers timed out");
            }

            for (Future<?> result : results) {
                result.get();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();

            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    exception.addSuppressed(new IllegalStateException(
                            "Executor did not terminate after interruption"));
                }
            } catch (InterruptedException second) {
                exception.addSuppressed(second);
            }

            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }
}