package we.clean;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoCleaner {
	private AutoCleaner() {
		throw new RuntimeException();
	}

	public interface CleanUp {
		void clean();
	}

	private static class Node extends PhantomReference<Object> implements CleanUp {
		private CleanUp cleanUp;

		public Node(Object referent, ReferenceQueue<? super Object> q, CleanUp cleanUp) {
			super(referent, q);
			this.cleanUp = Objects.requireNonNull(cleanUp);
		}

		@Override
		public void clean() {
			this.cleanUp.clean();
			this.cleanUp = null;
			// clear referent
			super.clear();
		}
	}

	private static final AtomicBoolean STARTED = new AtomicBoolean(false);

	private static final ReferenceQueue<Node> QUEUE = new ReferenceQueue<>();

	private static void init() {
		if (STARTED.compareAndSet(false, true)) {

		}
	}
}