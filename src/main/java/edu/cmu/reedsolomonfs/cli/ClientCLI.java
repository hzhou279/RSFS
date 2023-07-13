package edu.cmu.reedsolomonfs.cli;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.cmu.reedsolomonfs.cli.DirectoryTree.Node;
import edu.cmu.reedsolomonfs.client.Client;
import edu.cmu.reedsolomonfs.server.MasterServiceGrpc;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.GRPCMetadata;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.GRPCNode;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenRequest;
import edu.cmu.reedsolomonfs.server.MasterserverOutter.TokenResponse;
import io.grpc.ManagedChannelBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientCLI implements KeyListener {
    public static String localPath = "/A";
    public static String[] lowerDirectory = { "direcory1", "directory2", "file1", "file2" };
    public static String[] upperDirectory;
    static Map<String, LinkedList<Integer>> metaData;
    static DirectoryTree tree = new DirectoryTree();
    static Node root;
    static Client client;

    public static void main(final String[] args) throws Exception {
        // System.setErr(new PrintStream("./log/err.txt"));
        // /dev/null is a null device that discards any data that is written to it
        // System.setErr(new PrintStream("/dev/null"));
        client = new Client(args);
        // client.test(args);

        Scanner scanner = new Scanner(System.in);
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        metaData = new HashMap<>();

        LinkedList<Integer> list1 = new LinkedList<>(Arrays.asList(1, 2, 3, 4));
        LinkedList<Integer> list2 = new LinkedList<>(Arrays.asList(5, 6, 7));
        LinkedList<Integer> list3 = new LinkedList<>(Arrays.asList(8, 9));
        LinkedList<Integer> list4 = new LinkedList<>(Arrays.asList(8, 9, 10));

        metaData.put("A/B/C", list1);
        metaData.put("A/E/M", list2);
        metaData.put("A/E/Z", list3);
        metaData.put("A/E/Z/D", list4);

        TokenResponse tResponse = client.requestToken("read", "/A/B/C");
        // print out tResponse metadata
        System.out.println("tResponse metadata");
        List<GRPCMetadata> m = tResponse.getMetadataList();
        for (GRPCMetadata data : m) {
            System.out.println("File Path: " + data.getFilePath());
            List<GRPCNode> nodes = data.getNodesList();
            for (GRPCNode node : nodes) {
                System.out.println(node.getChunkIdx());
                System.out.println(node.getServerId());
                System.out.println(node.getIsData());
            }
        }

        for (String path : metaData.keySet()) {
            tree.addPath(path);
        }

        root = tree.getRoot();

        options.addOption("cd", "cd", false, "he type of ordering to use, default 'random'");

        while (true) {
            System.out.print(localPath + " % ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("quit")) {
                break;
            }

            String[] words = input.split(" ");

            CommandLine line = parser.parse(options, words);

            if (line.hasOption("cd")) {
                System.out.println("change directory");
            } else if (words[0].equals("cd") && !words[1].equals("..")) {
                Path path = Paths.get(words[1]);
                if (!path.isAbsolute()) {
                    System.out.println("The path is invalid");
                } else {
                    if (pathExists(words[1])) {
                        localPath = words[1];
                    } else {
                        System.out.println("Path does not exist!");
                    }
                }
            } else if (words[0].equals("cd") && words[1].equals("..")) {
                Path path = Paths.get(localPath);
                Path parentPath = path.getParent();
                if (parentPath != null) {
                    String parentDirectory = parentPath.toString();
                    System.out.println(parentDirectory);
                    localPath = parentDirectory;
                } else {
                    System.out.println("No parent directory found.");
                }
            } else if (words[0].equals("pwd")) {
                System.out.println(localPath);
            } else if (words[0].equals("ls")) {
                List<String> lsList = new ArrayList<>();
                lsList = tree.listDirectory(localPath);
                for (String string : lsList) {
                    System.out.print(string + " ");
                }
                System.out.println();
            } else if (words[0].equals("rm")) {
                System.out.println("REMOVE FILE");
            } else if (words[0].equals("mkdir")) {
                System.out.println("MAKE DIRECTORY");
            } else if (words[0].equals("create")) {
                // String filePath = "./ClientClusterCommTestFiles/Files/test.txt";
                // byte[] fileData = Files.readAllBytes(Path.of(filePath));
                // client.create(client.cliClientService, words[1], fileData, client.groupId);
                String clientFileDirectory = "./ClientClusterCommTestFiles/Files/";
                String newFileName = words[1];
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("vim",
                            clientFileDirectory + newFileName);
                    Process process = processBuilder.inheritIO().start();
                    process.waitFor();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                Path newFilePath = Paths.get(clientFileDirectory, newFileName);

                if (Files.exists(newFilePath)) {
                    try {
                        byte[] fileData = Files.readAllBytes(newFilePath);
                        String wholePath = localPath + "/" + words[1];
                        System.out.println(wholePath);
                        client.create(client.cliClientService, words[1], fileData, client.groupId);
                        tree.addPath(wholePath);
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + e.getMessage());
                    }
                } else {
                    System.out.println("There is nothing to create");
                }

                // String filePath = "./ClientClusterCommTestFiles/Files/test.txt";
                // client.create(client.cliClientService, wholePath, fileData, client.groupId);
                System.out.println("!!!!!!");
            } else if (words[0].equals("read")) {
                System.out.println(words[1]);
                byte[] fileDataRead = client.read(client.cliClientService, "read", words[1], 724, client.groupId);

                System.out.println("File read successfully!!!!");

                // write fileDataRead to a file
                Files.write(Path.of("./ClientClusterCommTestFiles/FilesRead/testRead1.txt"), fileDataRead);
            } else {
                System.out.println("Invalid Command");
            }
        }

        scanner.close();
        System.out.println("CLI application exited.");
        client.test(args);
    }

    public String[] findStringsStartingWith(String[] array, char[] startChars) {
        List<String> result = new ArrayList<>();

        for (String str : array) {
            char firstChar = str.charAt(0);
            for (char startChar : startChars) {
                if (firstChar == startChar) {
                    result.add(str);
                    break;
                }
            }
        }

        return result.toArray(new String[0]);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            System.out.println("I love you");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public static boolean pathExists(String path) {
        String[] dirs = path.split("/");
        Node node = root;
        for (int i = 0; i < dirs.length; i++) {
            String dir = dirs[i];
            if (dir.equals("")) {
                dir = dirs[++i];
            }
            if (!node.children.containsKey(dir) && !dir.equals("")) {
                return false;
            }
            node = node.children.get(dir);
        }
        return true;
    }
}
