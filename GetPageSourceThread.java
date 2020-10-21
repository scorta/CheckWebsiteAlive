import java.util.List;

public class GetPageSourceThread implements Runnable {
	private final String threadName;
	private final List<String> listUrl;
	private final StringBuilder res;
	private final int start;
	private final int end;

	@Override
	public void run() {
		for (int i = start; i < end; ++i) {
			StringBuilder sbTemp = new StringBuilder();
			sbTemp.append(listUrl.get(i));
			sbTemp.append("\t");
			System.out.println(threadName + " is getting: " + listUrl.get(i));
			String pageSrc = "";
			String urlHttps = "https://" + listUrl.get(i);
			String urlHttp = "http://" + listUrl.get(i);
			if (!CheckUrl.isDeadHelper(urlHttps)) {
				pageSrc = SocketConnection.getURLSource(urlHttps);
			} else if (!CheckUrl.isDeadHelper(urlHttp)) {
				pageSrc = SocketConnection.getURLSource(urlHttp);
			}

			System.out.println(threadName + " is DONE getting: " + listUrl.get(i));
			sbTemp.append(pageSrc);
			sbTemp.append("\n");
			synchronized (res) {
				res.append(sbTemp);
			}
		}
		System.out.println(threadName + " done!");
	}

	public GetPageSourceThread(String name, List<String> listUrl, StringBuilder res, int start, int end) {
		this.threadName = name;
		this.listUrl = listUrl;
		this.res = res;
		this.start = start;
		this.end = end;
	}
}
