import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizLeaderboardSolver {

    private static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final String REG_NO = "RA2311003010826"; 
    private static final int POLLS = 10;
    private static final int DELAY_MS = 5000;

    public static void main(String[] args) throws Exception {
        String regNo = (args.length > 0 && args[0] != null && !args[0].trim().isEmpty())
                ? args[0].trim()
                : REG_NO;

        Map<String, Integer> totalScores = new HashMap<>();
        Set<String> seenEvents = new HashSet<>();

        for (int poll = 0; poll < POLLS; poll++) {
            if (poll > 0) {
                Thread.sleep(DELAY_MS);
            }

            String response = sendGet(regNo, poll);
            List<Event> events = parseEvents(response);

            for (Event e : events) {
                String key = e.roundId + "|" + e.participant;
                if (seenEvents.add(key)) {
                    totalScores.put(e.participant, totalScores.getOrDefault(e.participant, 0) + e.score);
                }
            }
        }

        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>(totalScores.entrySet());
        leaderboard.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            return a.getKey().compareTo(b.getKey());
        });

        int grandTotal = 0;
        StringBuilder submitBody = new StringBuilder();
        submitBody.append("{\"regNo\":\"").append(escapeJson(regNo)).append("\",\"leaderboard\":[");

        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);
            grandTotal += entry.getValue();

            if (i > 0) submitBody.append(",");
            submitBody.append("{\"participant\":\"")
                    .append(escapeJson(entry.getKey()))
                    .append("\",\"totalScore\":")
                    .append(entry.getValue())
                    .append("}");
        }

        submitBody.append("]}");

        System.out.println("Final Leaderboard:");
        for (Map.Entry<String, Integer> entry : leaderboard) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println("Grand Total = " + grandTotal);

        String submitResponse = sendPost(submitBody.toString());
        System.out.println("\nSubmit Response:");
        System.out.println(submitResponse);
    }

    private static String sendGet(String regNo, int poll) throws Exception {
        String url = BASE_URL + "/quiz/messages?regNo="
                + URLEncoder.encode(regNo, "UTF-8")
                + "&poll=" + poll;

        return request("GET", url, null);
    }

    private static String sendPost(String jsonBody) throws Exception {
        return request("POST", BASE_URL + "/quiz/submit", jsonBody);
    }

    private static String request(String method, String urlString, String body) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");

            if ("POST".equals(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String response = readStream(stream);

            if (code < 200 || code >= 300) {
                throw new RuntimeException("HTTP " + code + " for " + method + " " + urlString + "\n" + response);
            }

            return response;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static List<Event> parseEvents(String json) {
        List<Event> events = new ArrayList<>();

        int eventsIndex = json.indexOf("\"events\"");
        if (eventsIndex < 0) return events;

        int arrayStart = json.indexOf('[', eventsIndex);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0 || arrayEnd <= arrayStart) return events;

        String arrayText = json.substring(arrayStart, arrayEnd + 1);
        Matcher objectMatcher = Pattern.compile("\\{([^{}]*)\\}").matcher(arrayText);

        while (objectMatcher.find()) {
            String obj = objectMatcher.group(1);

            String roundId = extractStringField(obj, "roundId");
            String participant = extractStringField(obj, "participant");
            Integer score = extractIntField(obj, "score");

            if (roundId != null && participant != null && score != null) {
                events.add(new Event(roundId, participant, score));
            }
        }

        return events;
    }

    private static String extractStringField(String text, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static Integer extractIntField(String text, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class Event {
        String roundId;
        String participant;
        int score;

        Event(String roundId, String participant, int score) {
            this.roundId = roundId;
            this.participant = participant;
            this.score = score;
        }
    }
}