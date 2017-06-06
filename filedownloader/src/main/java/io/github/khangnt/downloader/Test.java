package io.github.khangnt.downloader;

import java.util.Scanner;
import java.util.logging.Level;

import io.github.khangnt.downloader.model.Task;

/**
 * Created by Khang NT on 6/4/17.
 * Email: khang.neon.1997@gmail.com
 */

public class Test {
    public static void main(String[] args) throws Exception {
        Log.setLogLevel(Level.ALL);
        FileDownloader fileDownloader = new FileDownloader(new DefaultFileManager(),
                new DefaultHttpClient(), new NonPersistentTaskManager());
        fileDownloader.setMaxWorkers(32);
        fileDownloader.addTask(new Task.Builder("100MB-tokyo.bin", "http://speedtest.tokyo.linode.com/100MB-tokyo.bin")
                .setMaxParallelConnections(16).build());
        Thread printSpeed = null;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.next();
            if ("stop".equals(command)) {
                System.exit(0);
            } else if ("pause".equals(command)) {
                fileDownloader.pause();
                if (printSpeed != null) {
                    printSpeed.interrupt();
                }
            } else if ("start".equals(command)) {
                fileDownloader.start();
                printSpeed = new PrintSpeed(fileDownloader);
                printSpeed.start();
            }
        }
    }

    private static class PrintSpeed extends Thread {
        private FileDownloader fileDownloader;

        public PrintSpeed(FileDownloader fileDownloader) {
            this.fileDownloader = fileDownloader;
        }

        @Override
        public void run() {
            while (true) {
                System.out.println(fileDownloader.getSpeed() / 1024 + " KB/s");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
