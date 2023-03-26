package org.example;

import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {
        List<Call> calls = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\revas\\IdeaProjects\\untitled4\\src\\main\\java\\org\\example\\cdr.txt"));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(", ");
            int callType = Integer.parseInt(parts[0]);
            String phoneNumber = parts[1];
            String start = parts[2];
            String end = parts[3];
            int subscriptionType = Integer.parseInt(parts[4]);
            Call call = new Call(callType, phoneNumber, start, end, subscriptionType);
            calls.add(call);
        }
        reader.close();

        Map<String, List<Call>> subscribers = calls.stream()
                .collect(Collectors.groupingBy(Call::getPhoneNumber));


        for (String subscriber : subscribers.keySet()) {

            int tariff = subscribers.get(subscriber).get(0).getSubscriptionType();
            DecimalFormat dF = new DecimalFormat("00");
            StringBuilder res = new StringBuilder();
            res.append("Tariff index: ").append(dF.format(tariff)).append("\n");
            res.append("-----------------------------------------------------------------------------" + "\n");
            res.append("Report for phone number ").append(subscriber).append(":").append("\n");
            res.append("-----------------------------------------------------------------------------" + "\n");
            res.append("| Call Type |     Start Time      |      End Time       | Duration |  Cost  |" + "\n");

            List<Call> callsResult = subscribers.get(subscriber);
            callsResult = callsResult.stream().sorted(Comparator.comparing(c -> {
                String startDateStr = c.getStart();
                DateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                Date date = new Date();
                try {
                    date = inputFormat.parse(startDateStr);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return date.getTime();
            })).collect(Collectors.toList());
            if (tariff == 6) {
                double costMinute = 1.0;
                double totalMinutes = 0;
                for (Call call : callsResult) {
                    double min = call.getTotalMinutes();
                    if (totalMinutes + min <= 300) {
                        res.append(call).append(String.format("%7s", 0.0)).append(" |").append("\n");
                        totalMinutes += min;
                    } else if (totalMinutes - min <= 300) {
                        res.append(call).append(String.format("%7s", (totalMinutes + min - 300) * costMinute)).append(" |").append("\n");
                        totalMinutes += min;
                    } else {
                        res.append(call).append(String.format("%7s", min * costMinute)).append(" |").append("\n");
                        totalMinutes += min;
                    }
                }
                res.append("-----------------------------------------------------------------------------" + "\n");
                if (totalMinutes <= 300) {
                    res.append("|                                           Total Cost: |").append(String.format("%18s", 100.0)).append(" |").append("\n");
                } else {
                    res.append("|                                           Total Cost: |").append(String.format("%18s", (100 + (totalMinutes - 300) * costMinute))).append(" |").append("\n");
                }
                res.append("-----------------------------------------------------------------------------" + "\n");
                createTXT(subscriber, res.toString());
            } else if (tariff == 3) {
                double costMinute = 1.5;
                double totalMinutes = 0;
                for (Call call : callsResult) {
                    double min = call.getTotalMinutes();
                    res.append(call).append(String.format("%7s", min * costMinute)).append(" |").append("\n");
                    totalMinutes += min;
                }
                res.append("-----------------------------------------------------------------------------" + "\n");
                res.append("|                                           Total Cost: |").append(String.format("%18s", totalMinutes * costMinute)).append(" |").append("\n");
                res.append("-----------------------------------------------------------------------------" + "\n");
                createTXT(subscriber, res.toString());

            } else if (tariff == 11) {
                double costMinute = 1.5;
                double costMinute100 = 0.5;
                double totalMinutes = 0;
                for (Call call : callsResult) {
                    if (call.getCallType() == 2) {
                        double min = call.getTotalMinutes();
                        if (totalMinutes + min <= 100) {
                            res.append(call).append(String.format("%7s", min * costMinute100)).append(" |").append("\n");
                            totalMinutes += min;
                        } else if (totalMinutes - min <= 100) {

                            res.append(call).append(String.format("%7s", (100 - min) * costMinute100 + (totalMinutes + min - 100) * costMinute)).append(" |").append("\n");
                            totalMinutes += min;
                        } else {
                            res.append(call).append(String.format("%7s", min * costMinute)).append(" |").append("\n");
                            totalMinutes += min;
                        }
                    } else {
                        res.append(call).append(String.format("%7s", 0.0)).append(" |").append("\n");
                    }
                }
                res.append("-----------------------------------------------------------------------------" + "\n");
                if (totalMinutes <= 100) {
                    res.append("|                                           Total Cost: |").append(String.format("%18s", totalMinutes * costMinute100)).append(" |").append("\n");
                } else {
                    res.append("|                                           Total Cost: |").append(String.format("%18s", (50 + (totalMinutes - 100) * costMinute))).append(" |").append("\n");
                }
                res.append("-----------------------------------------------------------------------------" + "\n");
                createTXT(subscriber, res.toString());
            }
        }
    }
    public static void createTXT(String number, String report) throws Exception {
        File fileD = new File("reports");
        if(!fileD.exists()) {
            fileD.mkdir();
        }
        String filePath = "reports\\report " + number + ".txt";
        File file = new File(filePath);
        if (!file.exists()){
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(report);
        fileWriter.flush();
        fileWriter.close();
    }

}



class Call {
    private int callType;
    private String phoneNumber;
    private String start;
    private String end;
    private int subscriptionType;
    private String duration;
    private double totalMinutes;

    public double getTotalMinutes() {
        return totalMinutes;
    }

    public Call(int callType, String phoneNumber, String start, String end, int subscriptionType) {
        DecimalFormat dF = new DecimalFormat("00");
        this.callType = callType;
        this.phoneNumber = phoneNumber;
        this.start = start;
        this.end = end;
        this.subscriptionType = subscriptionType;
        long l = durationInSeconds(start, end);
        this.totalMinutes = Math.ceil(l / 60.0);
        duration = dF.format(l / 3600) + ":" + dF.format((l % 3600) / 60) + ":" + dF.format(l % 60);
    }

    public int getCallType() {
        return callType;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public int getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(int subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    @Override
    public String toString() {
        try {
            return "|     " + callType + "     | " + timeFormat(start) + " | " + timeFormat(end) + " | " + duration + " |";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private long durationInSeconds(String start, String end) {
        LocalDateTime dt1 = LocalDateTime.parse(start, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDateTime dt2 = LocalDateTime.parse(end, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Duration duration = Duration.between(dt1, dt2);
        return duration.getSeconds();
    }

    private String timeFormat(String d) throws ParseException {
        DateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = inputFormat.parse(d);
        DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return outputFormat.format(date);
    }
}

