package cleaner.core;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class AutoCleaner extends PhantomReference<Object> {
    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

    private static final Thread CLEANER_THREAD = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AutoCleaner cleaner = (AutoCleaner) QUEUE.remove();
                cleaner.clean();
                cleaner.clear();
            } catch (Exception ignored) {

            }
        }
    });

    static {
        CLEANER_THREAD.setName("AutoCleanerThread");
        CLEANER_THREAD.start();
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

    private void clean() {
        if (dequeue(this)) {
            try {
                this.runnable.run();
            } catch (Exception ignored) {
            }
        }
    }
}
