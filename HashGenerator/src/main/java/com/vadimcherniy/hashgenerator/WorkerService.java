package main.java.com.vadimcherniy.hashgenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * Class for work with Worker
 */
public class WorkerService {
    private final ExecutorService workerExecutor;

    public WorkerService() {
        workerExecutor = Executors.newSingleThreadExecutor();
        System.out.println("WorkerService starts successfuly");
    }

    /**
     * Method for generating a hash from content
     *
     * @param futures list of futures with ProviderResult
     * @return list of futures with WorkerResult
     */
    public List<Future<WorkerResult>> generateHash(List<Future<ProviderService.ProviderResult>> futures) {
        final CompletionService<WorkerResult> completionService = new ExecutorCompletionService<>(workerExecutor);
        List<ProviderService.ProviderResult> providerResults = new ArrayList<>();
        for (Future<ProviderService.ProviderResult> future : futures) {
            try {
                providerResults.add(future.get(10, TimeUnit.SECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return providerResults.stream()
                .map(result -> completionService.submit(() -> createHashFromContent(result)))
                .collect(Collectors.toList());
    }

    /**
     * Method for generating a hash from content
     *
     * @param providerResult ProviderResult entity
     * @return workerResult entity
     */
    private WorkerResult createHashFromContent(ProviderService.ProviderResult providerResult) {
        WorkerResult workerResult = null;
        System.out.println(Thread.currentThread().getName() + " create hash from file " + providerResult.getFileName());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(providerResult.getContent().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte aHash : hash) {
                String hex = Integer.toHexString(0xff & aHash);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            workerResult = new WorkerResult(hexString.toString(), providerResult.getFileName());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return workerResult;
    }

    /**
     * WorkerResult class
     */
    static class WorkerResult {
        String hash;
        String fileName;

        private WorkerResult(String hash, String fileName) {
            this.hash = hash;
            this.fileName = fileName;
        }

        public String getHash() {
            return hash;
        }

        public String getFileName() {
            return fileName;
        }
    }

    /**
     * Shutdown WorkerService
     */
    public void shutDownService() {
        workerExecutor.shutdown();
    }
}
