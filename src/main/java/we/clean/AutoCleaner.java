package we.clean;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoCleaner {
  private AutoCleaner() {
    throw new RuntimeException();
  }

  public interface CleanUp {
    void clean();
  }


  private static final AtomicBoolean STARTED = new AtomicBoolean(false);

  private static final ConcurrentHashMap<Reference<?>, CleanUp> CLEAN_UP_MAPPING =
    new ConcurrentHashMap<>(64);

  private static final ReferenceQueue<Object> REFERENCE_QUEUE = new ReferenceQueue<>();

  private static final CleanThread THREAD = new CleanThread();

  public static void register(Object watcher, CleanUp cleanUp) {
    init();
    PhantomReference<Object> reference = new PhantomReference<>(watcher, REFERENCE_QUEUE);
    CLEAN_UP_MAPPING.putIfAbsent(reference, cleanUp);
  }

  private static void init() {
    if (STARTED.compareAndSet(false, true)) {
      THREAD.start();
    }
  }

  private static class CleanThread extends Thread {
    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          runInternal();
        } catch (InterruptedException e) {
          break;
        }
      }
    }

    private void runInternal() throws InterruptedException {
      Reference<?> reference = REFERENCE_QUEUE.remove();
      if (null != reference) {
        CleanUp cleanUp = CLEAN_UP_MAPPING.remove(reference);
        if (null != cleanUp) {
          cleanUp.clean();
        }
        reference.clear();
      }
    }
  }
}