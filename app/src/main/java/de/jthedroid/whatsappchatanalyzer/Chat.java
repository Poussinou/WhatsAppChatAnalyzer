package de.jthedroid.whatsappchatanalyzer;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import de.jthedroid.whatsappchatanalyzer.bintree.BinTree;

class Chat {
    private static final int MAX_GRAPH_POINTS = 500;

    final HashMap<String, Sender> senders = new HashMap<>();
    private final ArrayList<Message> messages = new ArrayList<>();
    ArrayList<Sender> sortedSenders;
    private GraphData totalMessagesGraph;
    private boolean valid = true;

    void init(BufferedReader br) throws IOException {
        ArrayList<String> lines = readLines(br);
        ArrayList<String> strings = createMessageStrings(lines);
        if (!valid) {
            return;
        }
        addMessages(strings);
        if (messages.isEmpty()) {
            valid = false;
            return;
        }
        sortedSenders = createSortedSenderList();
        if (sortedSenders.isEmpty()) {
            valid = false;
            return;
        }
        totalMessagesGraph = createTotalMessagesGraph();
    }

    private ArrayList<String> readLines(@NonNull BufferedReader br) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        while (br.ready()) {
            lines.add(br.readLine());
        }
        br.close();
        return lines;
    }

    private ArrayList<String> createMessageStrings(@NonNull ArrayList<String> lines) {
        ArrayList<String> strings = new ArrayList<>();
        for (String l : lines) {
            //                   1/1/17, 05:55 -
            //                   12/12/17, 05:55 -
            //                   05.04.19, 16:53 -
            if (l.matches("^\\d+.*\\d+, \\d+.* - .*")) {
                strings.add(l);
            } else {
                if (!strings.isEmpty()) {
                    String line = strings.get(strings.size() - 1);
                    strings.remove(line);
                    strings.add(line + "\n" + l);
                }
            }
        }
        if (strings.isEmpty()) {
            valid = false;
        }
        return strings;
    }

    private void addMessages(ArrayList<String> strings) {
        int consecutiveParseEx = 0;
        for (String s : strings) {
            try {
                Message m = new Message(s, this);
                messages.add(m);
                consecutiveParseEx = 0;
            } catch (ParseException e) {
                Log.e("Chat ParseException", e.toString());
                consecutiveParseEx++;
                if (consecutiveParseEx > 50) {
                    valid = false;
                    return;
                }
            }
        }
    }

    private ArrayList<Sender> createSortedSenderList() {
        ArrayList<Sender> senderList = new ArrayList<>(senders.values());
        ArrayList<Sender> sorted = new ArrayList<>();
        if (!senders.isEmpty()) {
            BinTree<Sender> senderTree = new BinTree<>(senderList.get(0));
            for (int i = 1; i < senderList.size(); i++) {
                Sender sender = senderList.get(i);
                senderTree.addContent(sender);
            }
            ArrayList<Sender> tempList = senderTree.sort();
            sorted = new ArrayList<>(tempList);
            int size = tempList.size();
            sorted.ensureCapacity(size);
            for (int i = size - 1; i >= 0; i--) {
                sorted.set(size - 1 - i, tempList.get(i));
            }
        }
        return sorted;
    }

    private GraphData createTotalMessagesGraph() {  //TODO: improve scaling (now, the end is always cut off (<500))
        float[] xData, yData;
        int msgCount = messages.size();
        if (msgCount == 0) return null;
        int step;
        if (msgCount <= MAX_GRAPH_POINTS) {
            xData = new float[msgCount];
            yData = new float[msgCount];
            step = 1;
        } else {
            xData = new float[MAX_GRAPH_POINTS];
            yData = new float[MAX_GRAPH_POINTS];
            step = msgCount / MAX_GRAPH_POINTS;
        }
        for (int i = 0; i < xData.length; i++) {
            Message msg = messages.get(i * step);
            xData[i] = msg.getDate().getTime(); //timecode
            yData[i] = i * step;  //total messages at this point
        }
        GraphData gD = new GraphData(xData, yData);
        gD.scale();
        return gD;
    }

    ArrayList<Sender> getSortedSenders() {
        return sortedSenders;
    }

    GraphData getTotalMessagesGraph() {
        return totalMessagesGraph;
    }

    int getMaxMsgCount() {
        return sortedSenders.get(0).getMsgCount();
    }

    int getMsgCount() {
        return messages.size();
    }

    boolean isValid() {
        return valid;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (Sender sender : sortedSenders) {
            s.append(sender.getName());
            s.append(", ");
            s.append(sender.getMsgCount());
            s.append("\n");
        }
        return s.toString();
    }
}
