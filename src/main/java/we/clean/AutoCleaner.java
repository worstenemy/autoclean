package we.clean;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class AutoCleaner extends PhantomReference<Object> implements Runnable {
  private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

  private static final Cleaner CLEANER;

  static {
    CLEANER = new Cleaner();
    CLEANER.start();
  }

  private static AutoCleaner head;

  private final Runnable runnable;

  private AutoCleaner next;

  private AutoCleaner prev;

  private AutoCleaner(Object referent, ReferenceQueue<? super Object> q, Runnable runnable) {
    super(referent, q);
    this.runnable = runnable;
  }

  public static void register(Object referent, Runnable runnable) {
    AutoCleaner cleaner = new AutoCleaner(referent, QUEUE, runnable);
    enqueue(cleaner);
  }

  private static void enqueue(AutoCleaner cleaner) {
    synchronized (AutoCleaner.class) {
      if (null != head) {
        cleaner.next = head;
        head.prev = cleaner;
      }
      head = cleaner;
    }
  }

  private static boolean dequeue(AutoCleaner cleaner) {
    if (null == cleaner) {
      return false;
    }
    synchronized (AutoCleaner.class) {
      // already dequeue
      if (cleaner.next == cleaner) {
        return false;
      }

      //adjust head
      if (head == cleaner) {
        if (null != cleaner.next) {
          head = cleaner.next;
        } else {
          head = cleaner.prev;
        }
      }

      if (null != cleaner.next) {
        cleaner.next.prev = cleaner.prev;
      }

      if (null != cleaner.prev) {
        cleaner.prev.next = cleaner.next;
      }

      cleaner.next = cleaner;
      cleaner.prev = cleaner;
      return true;
    }
  }

  @Override
  public void run() {
    if (dequeue(this)) {
      try {
        this.runnable.run();
      } catch (Exception ignored) {
      }
    }
  }

  private static class Cleaner extends Thread {
    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          AutoCleaner cleaner = (AutoCleaner) QUEUE.remove();
          cleaner.run();
          cleaner.clear();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}