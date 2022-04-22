package palia.algorithm.callback;

import java.util.HashMap;
import java.util.Map;

public class CLIPaliaMinerStatusUpdater implements PaliaMinerStatusUpdater {

	private Map<PaliaMinerStatus, Long> timestamps = new HashMap<>();

	@Override
	public void update(PaliaMinerStatus status, boolean hasStarted) {
		if (hasStarted) {
			System.out.print("Starting " + status + "... ");
			timestamps.put(status, System.currentTimeMillis());
		} else {
			System.out.println("Done! (" + (System.currentTimeMillis() - timestamps.get(status)) + " ms)");
		}
	}
}
