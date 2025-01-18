package processtimer;

import java.util.ArrayList;
import java.util.List;

public class ProcessStatus {
	public final int CODE_STARTED = -257;
	public volatile int exitCode;
	public List<String> output = new ArrayList<String>();
}
