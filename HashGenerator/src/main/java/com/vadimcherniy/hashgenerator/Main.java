package main.java.com.vadimcherniy.hashgenerator;

import java.io.BufferedReader;
import java.io.File;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Main {
    private static final ProviderService providerService = new ProviderService();
    private static final WorkerService workerService = new WorkerService();

    public static void main(String[] args) throws Exception {
        try {
            System.out.println("\nEnter the directory with files:" );
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String path = reader.readLine();

            File files[] = new File(path).listFiles();
            if (files != null) {
                List<String> listOfFiles = Arrays.stream(files).filter(File::isFile).map(File::getPath).collect(Collectors.toList());

                if (listOfFiles.isEmpty()) {
                    System.out.println("The directory is empty. Application will be close");
                    return;
                }

                List<Future<ProviderService.ProviderResult>> providersFutures = providerService.getContentFomFiles(listOfFiles);
                List<Future<WorkerService.WorkerResult>> workerResults = workerService.generateHash(providersFutures);
                providerService.saveNewContent(workerResults);
            }
        } finally {
            workerService.shutDownService();
            providerService.shutDownService();
        }
    }
}
