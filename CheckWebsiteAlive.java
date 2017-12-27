import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class CheckWebsiteAlive {
    private static final String[] DEAD_PHRASES = new String[]{
            "is expired and be suspended",
            "hosting for this domain is not configured",
            "cannot connect"
    };

    private static ArrayList<String> listUrl = new ArrayList<>();
    private static ArrayList<String > listOfDeadPhrases = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("******");
        System.out.println("This program will help you to check if a website from a list is dead or alive. To see the instruction, please visit: https://github.com/scorta/CheckWebsiteAlive");
        System.out.println("******");
        System.out.println("Start working...");
        String inputFile = "input.txt";
        String deadPhrasesFile = "dead_phrases.txt";
        StringBuilder result = new StringBuilder();
        if(args.length > 0){
            inputFile = args[0];
        }

        if(args.length > 1){
            deadPhrasesFile = args[1];
        }

        listUrl = importList(inputFile);
        listOfDeadPhrases = importList(deadPhrasesFile);

        for(String url : listUrl){
            result.append(url);
            result.append("\t");
            result.append(isDead(url));
            result.append("\n");
        }
        writeOutput(inputFile, result.toString());
    }

    private static void forceExit(){
        System.out.println("This program has encountered an error and will now exit.");
        System.exit(1);
    }

    private static void writeOutput(String fileName, String content){
        try{
            PrintWriter output = new PrintWriter(fileName + "_out.txt");
            output.print(content);
            output.close();
            System.out.println("Done. Please check the output file. It is named " + fileName + "_out.txt");
        } catch (Exception e){
            System.out.println("Error while writing result: " + e.toString());
            forceExit();
        }
    }

    private static ArrayList<String> importList(String fileName){
        try{
            ArrayList<String> list = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            for(String line; (line = br.readLine()) != null;){
                list.add(line);
            }
            return list;
        } catch (Exception e){
            System.out.println("Error while reading list: " + e.toString());
            forceExit();
            return null;
        }
    }

    private static boolean isDead(String url) {
        if (SocketConnection.htmlStatusCode(url, 5) >= 300 || SocketConnection.htmlStatusCode(url, 5) < 200)
            return true;
        String urlContent = SocketConnection.getURLSource(url, 10).toLowerCase();
        for (String deadPhrase : listOfDeadPhrases) {
            if (urlContent.contains(deadPhrase))
                return true;
        }
        return false;
    }
}

class SocketConnection {
    public static int htmlStatusCode(String url, int timeOut){
        try {
            URL urlObject = new URL(url);
            HttpURLConnection huc = (HttpURLConnection) urlObject.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            huc.setConnectTimeout(timeOut * 1000);
            huc.setRequestMethod("HEAD");  //OR  huc.setRequestMethod ("HEAD");
            huc.connect();
            return  huc.getResponseCode();
        } catch (Exception e){
            return 404;
        }
    }

    public static String getURLSource(String url, int timeOut){
        try {
            URL urlObject = new URL(url);
            HttpURLConnection huc = (HttpURLConnection) urlObject.openConnection();
            HttpURLConnection.setFollowRedirects(false);
            huc.setConnectTimeout(timeOut * 1000);
            huc.setRequestMethod("GET");
            huc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
            huc.connect();
            return toString(huc.getInputStream());
        } catch (Exception e){
            return "cannot connect. Error: " + e.toString();
        }
    }

    private static String toString(InputStream inputStream){
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String inputLine;
            StringBuilder stringBuilder = new StringBuilder();
            while ((inputLine = bufferedReader.readLine()) != null) {
                stringBuilder.append(inputLine);
            }

            return stringBuilder.toString();
        } catch (Exception e){
            return "Error: " + e.toString();
        }
    }
}
