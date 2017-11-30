package main.java.com.vadimcherniy.hashgenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Class for work with Provider
 */
public class ProviderService {
    private final ExecutorService providerExecutor;

    public ProviderService() {
        providerExecutor = Executors.newFixedThreadPool(3);
        System.out.println("ProviderService starts successfuly");
    }

    /**
     * Method for get contents from files
     *
     * @param files list of files
     * @return list of futures with ProviderResult entity
     */
    public List<Future<ProviderResult>> getContentFomFiles(final List<String> files) {
        final CompletionService<ProviderResult> completionService = new ExecutorCompletionService<>(providerExecutor);
        return files.stream()
                .map(file -> completionService.submit(() -> getProviderResult(file)))
                .collect(Collectors.toList());
    }

    /**
     * Method for save new content with SHA-256 hash to file
     *
     * @param workerResults list of futures with WorkerResult
     */
    public void saveNewContent(List<Future<WorkerService.WorkerResult>> workerResults) {
        List<WorkerService.WorkerResult> workerResultList = new ArrayList<>();
        for (Future<WorkerService.WorkerResult> future : workerResults) {
            try {
                workerResultList.add(future.get(10, TimeUnit.SECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        workerResultList.forEach(this::saveContentToFile);
    }

    /**
     * Method for save new content with SHA-256 hash to file
     *
     * @param workerResult WorkerResult entity
     */
    private void saveContentToFile(WorkerService.WorkerResult workerResult) {
        try {
            FileWriter writer = new FileWriter(new File(workerResult.getFileName()), true);
            writer.write("\n" + workerResult.getHash());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for get content from file
     *
     * @param fileName file
     * @return ProviderResult entity
     */
    private ProviderResult getProviderResult(String fileName) {
        System.out.println(Thread.currentThread().getName() + " read content from file " + fileName);
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ProviderResult(content, fileName);
    }

    /**
     * ProviderResult class
     */
    static class ProviderResult {
        private String content;
        private String fileName;

        private ProviderResult(String content, String fileName) {
            this.content = content;
            this.fileName = fileName;
        }

        public String getContent() {
            return content;
        }

        public String getFileName() {
            return fileName;
        }
    }

    /**
     * Shutdown ProviderService
     */
    public void shutDownService() {
        providerExecutor.shutdown();
    }
}
