import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CheckWebsiteAlive {
    // this DEAD_PHRASES is obsoleted, for reference only. The program will use a dead phrases from text file
	private static final String[] DEAD_PHRASES = new String[]{
			"is expired and be suspended",
			"hosting for this domain is not configured",
			"cannot connect"
	};

	private static List<String> listUrl = new ArrayList<>();
	private static List<String> listOfDeadPhrases = new ArrayList<>();

	public static void main(String[] args) {
		System.out.println("******");
		System.out.println("This program will help you to check if a website from a list is dead or alive. To see the instruction, please visit: https://github.com/scorta/CheckWebsiteAlive");
		System.out.println("******");
		System.out.println("Start working...");

		String inputFile = "input.txt";
		String deadPhrasesFile = "dead_phrases.txt";
		StringBuilder result = new StringBuilder();

		if (args.length != 4) {
			System.out.println("Must have 4 params: file name that contains list of urls, " +
					"and file name that contains list of dead phrases, number of thread(s), " +
					"and choice (0 == checking url alive, 1 == get page source)");
			return;
		}

		inputFile = args[0];
		listUrl = importList(inputFile);

		if (listUrl.size() == 0) {
			System.out.println("NO URLS, will exit now");
			return;
		}

		deadPhrasesFile = args[1];
		listOfDeadPhrases = importList(deadPhrasesFile);

		if (listOfDeadPhrases.size() == 0) {
			System.out.println("WARN: no dead phrases imported, are you sure?");
		}
		CheckUrl.listOfDeadPhrases = listOfDeadPhrases;

		int numberOfThreads = Integer.parseInt(args[2]);
		if (numberOfThreads < 1) {
			System.out.println("WARN: 0 thread? Will use 1 now");
			numberOfThreads = 1;
		}

		int choice = Integer.parseInt(args[3]);
		Thread[] listThread = new Thread[numberOfThreads];
		{
			int start, end = 0, step;
			step = 1 + listUrl.size() / numberOfThreads;

			for (int i = 0; i < numberOfThreads; ++i) {
				start = end;
				end = start + step;
				if (end >= listUrl.size()) {
					end = listUrl.size() - 1;
				}
				System.out.println("Thread " + i + " covers " + start + " " + end);
				if (choice == 0) {
					listThread[i] = new Thread(new CheckUrlAliveThread("Thread " + i, listUrl, result, start, end));
				} else {
					listThread[i] = new Thread(new GetPageSourceThread("Thread " + i, listUrl, result, start, end));
				}
				listThread[i].start();
			}
		}

		for (int i = 0; i < numberOfThreads; ++i) {
			try {
				listThread[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		writeOutput(inputFile, result.toString());
	}

	private static void forceExit() {
		System.out.println("This program has encountered an error and will now exit.");
		System.exit(1);
	}

	private static void writeOutput(String fileName, String content) {
		try {
			if (content.isEmpty()) {
				System.out.println("WARN: Empty content");
				return;
			}

			PrintWriter output = new PrintWriter(fileName + "_out.txt");
			output.print(content);
			output.close();
			System.out.println("Done. Please check the output file. It is named " + fileName + "_out.txt");
		} catch (Exception e) {
			System.out.println("Error while writing result: " + e.toString());
			forceExit();
		}
	}

	private static ArrayList<String> importList(String fileName) {
		try {
			ArrayList<String> list = new ArrayList<>();
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			for (String line; (line = br.readLine()) != null; ) {
				list.add(line);
			}
			return list;
		} catch (Exception e) {
			System.out.println("Error while reading list: " + e.toString());
			forceExit();
			return null;
		}
	}
}

class CheckUrlAliveThread implements Runnable {
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
			System.out.println(threadName + " is checking: " + listUrl.get(i));
			String livingStatus = CheckUrl.isDead(listUrl.get(i)) ? "DEAD" : "ALIVE";
			System.out.println(threadName + " is DONE checking: " + listUrl.get(i));
			sbTemp.append(livingStatus);
			sbTemp.append("\n");
			synchronized (res) {
				res.append(sbTemp);
			}
		}
		System.out.println(threadName + " done!");
	}

	public CheckUrlAliveThread(String name, List<String> listUrl, StringBuilder res, int start, int end) {
		this.threadName = name;
		this.listUrl = listUrl;
		this.res = res;
		this.start = start;
		this.end = end;
	}
}

class CheckUrl {
	public static List<String> listOfDeadPhrases;

	public static boolean isDead(String url) {
		boolean isDeadHttps = isDeadHelper("https://" + url);
		boolean isDeadHttp = isDeadHelper("http://" + url);

		return isDeadHttps && isDeadHttp;
	}

	public static boolean isDeadHelper(String url) {
		int htmlStatusCode = SocketConnection.htmlStatusCode(url);
		if (htmlStatusCode >= 301 || htmlStatusCode < 200) {
			return true;
		}
		String urlContent = SocketConnection.getURLSource(url);
		if (urlContent.length() < 10) {
			return true;
		}
		for (String deadPhrase : listOfDeadPhrases) {
			if (urlContent.contains(deadPhrase)) {
				return true;
			}
		}
		return false;
	}
}

class SocketConnection {
	public static final int timeOut = 3;

	public static int htmlStatusCode(String url) {
		try {
			URL urlObject = new URL(url);
			HttpURLConnection huc = (HttpURLConnection) urlObject.openConnection();
			HttpURLConnection.setFollowRedirects(false);
			huc.setConnectTimeout(timeOut * 1000);
			huc.setReadTimeout(timeOut * 1000);
			huc.setRequestMethod("HEAD");
			huc.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.87 Safari/537.36");
			huc.connect();
			return huc.getResponseCode();
		} catch (Exception e) {
			e.printStackTrace();
			return 999;
		}
	}

	public static String getURLSource(String url) {
		try {
			URL urlObject = new URL(url);
			HttpURLConnection huc = (HttpURLConnection) urlObject.openConnection();
			HttpURLConnection.setFollowRedirects(false);
			huc.setConnectTimeout(timeOut * 1000);
			huc.setRequestMethod("GET");
			huc.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.87 Safari/537.36");
			huc.connect();
			return toString(huc.getInputStream());
		} catch (Exception e) {
			return "cannot connect. Error: " + e.toString();
		}
	}


	private static String toString(InputStream inputStream) {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
			String inputLine;
			StringBuilder stringBuilder = new StringBuilder();
			while ((inputLine = bufferedReader.readLine()) != null) {
				stringBuilder.append(inputLine);
				stringBuilder.append("\n");
			}
			replaceAll(stringBuilder, "\n", " ");

			return stringBuilder.toString().toLowerCase();
		} catch (Exception e) {
			return "Error: " + e.toString();
		}
	}

	private static void replaceAll(StringBuilder builder, String from, String to) {
		int index = builder.indexOf(from);
		while (index != -1) {
			builder.replace(index, index + from.length(), to);
			index += to.length(); // Move to the end of the replacement
			index = builder.indexOf(from, index);
		}
	}
}
