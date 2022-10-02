package ru.compot;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    private static final String IN_FILE = "input.txt"; //ссылка
    private static final Pattern DOWNLOAD_URL_PATTERN = Pattern.compile("data-musmeta=.+\"url\":\"(.+?)\""); //ищет все ссылки, по которым можно скачать треки, находящиеся на главной странице

    public static void main(String[] args) {
        String link;
        try (BufferedReader br = new BufferedReader(new FileReader(IN_FILE))) { //получаем ссылку
            link = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        List<String> paths = new ArrayList<>(); //здесь будут храниться все найденные треки
        try {
            URL url = new URL(link); //делаем запрос на доступ к сайту, получаем всю информацию
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String result = br.lines().collect(Collectors.joining("\n"));
            Matcher matcher = DOWNLOAD_URL_PATTERN.matcher(result); //ищем ссылки по паттерну
            while (matcher.find() && paths.size() < 5) { //в лист заносим все найденные ссылки
                paths.add(matcher.group(1).replace("\\", ""));
            }
            System.out.printf("Найдено %d треков\n", paths.size());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        File outputDir = new File("output"); //создаём папку, если её не существует
        if (!outputDir.exists()) outputDir.mkdir();

        Thread[] threads = new Thread[paths.size()]; //сколько потоков будет скачивать музыку
        for (int i = 0; i < paths.size(); i++) { //заполняем массив потоками
            Thread thread = new ThreadDownloader(link + paths.get(i));
            thread.setName("music-" + i + ".mp3"); //задаём потоку имя, т.е. название файла
            thread.start();
            threads[i] = thread; //кладём поток в массив
        }
        while (true) { //бесконечно проверяем
            boolean exit = true;
            for (Thread thread : threads) { //проходимся по каждому потоку
                if (thread.isAlive()) { //если какой-либо поток хотя бы работает, то программа завершаться не будет
                    exit = false;
                    break;
                }
            }
            if (exit) break;
        }
    }

    private static class ThreadDownloader extends Thread {

        private final String link;
        private long startTime; //когда запустили поток

        private ThreadDownloader(String link) {
            this.link = link;
        }

        @Override
        public synchronized void start() {
            super.start();
            System.out.println("Скачивание трека " + link);
            startTime = System.currentTimeMillis();
        }

        @Override
        public void run() { //логика, скачивающая файл
            try {
                URL url = new URL(link);
                ReadableByteChannel byteChannel = Channels.newChannel(url.openStream());
                FileOutputStream stream = new FileOutputStream("output\\" + getName());
                stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
                stream.close();
                byteChannel.close();
                System.out.printf("Файл %s скачан за %.2f секунд\n", getName(), (System.currentTimeMillis() - startTime) / 1000f);
            } catch (Exception e) {
                System.out.println("Произошла ошибка при скачивании файла " + getName());
                e.printStackTrace();
            }

        }
    }

}