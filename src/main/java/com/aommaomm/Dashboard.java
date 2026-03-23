package com.aommaomm;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class Dashboard {
    static String outputPath = "./streams/";
    static List<Record> bots = new ArrayList<>();

    public static void main(String[] args) {
        new File("logs").mkdirs(); 
        makeBots();
        startBots();
    }

    protected static void makeBots(){
        File streamerFile = new File("streamers.txt");
        try (Scanner scn = new Scanner(streamerFile)) {
            while (scn.hasNextLine()){
                String line = scn.nextLine().trim();
                if (!line.isEmpty()) {
                    bots.add(new Record(line, outputPath));
                }
            }
        } catch (Exception e) {
            System.err.println("Critical Error: Could not read streamers.txt");
            System.exit(1);
        }
    }

    protected static void startBots(){
        for (Record robot : bots){
            robot.start();
        }
    }
}
