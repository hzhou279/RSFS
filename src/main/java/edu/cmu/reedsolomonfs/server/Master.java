package edu.cmu.reedsolomonfs.server;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import edu.cmu.reedsolomonfs.client.ClientMasterServiceGrpc.ClientMasterServiceImplBase;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.Metadata;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenRequest;
import edu.cmu.reedsolomonfs.client.Reedsolomonfs.TokenResponse;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class Master extends ClientMasterServiceImplBase {

    private static String secretKey;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(8080)
                .addService(new Master())
                .build();

        // Generate secret key for master signing JWTs
        generateSecretKey();

        server.start();
        // FileInputStream fileInputStream = new FileInputStream("./as");
        server.awaitTermination();
    }

    @Override
    public void getToken(TokenRequest request, StreamObserver<TokenResponse> responseObserver) {
        // Server-side implementation logic
        boolean isHealthy = true; // Compute the value of isHealthy
        String token = generateJWT(request.getRequestType(), request.getFilePath()); // Compute the value of token
        List<String> ips = new ArrayList<>(); // Compute the value of ips
        Metadata data = Metadata.newBuilder()
                .setFileSize(0)
                .build(); // Compute the value of data

        // Prepare the response
        TokenResponse response = TokenResponse.newBuilder()
                .setIsHealthy(isHealthy)
                .setToken(token)
                .addAllIps(ips)
                .setMetadata(data)
                .build();

        // Send the response to the client
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public String generateJWT(String requestType, String filePath) {
        // Set the token expiration time (e.g., 1 hour from now)
        long expirationTimeMillis = System.currentTimeMillis() + 3600000; // 1 hour
        Date expirationDate = new Date(expirationTimeMillis);

        // Set the JWT claims (e.g., subject and issuer)
        Claims claims = Jwts.claims();
        // claims.setSubject("example_subject"); // this can be the name of the client
        // claims.setIssuer("example_issuer"); // this can be the name of the master
        claims.put("permission", requestType);
        claims.put("filePath", filePath);

        // Build the JWT
        JwtBuilder jwtBuilder = Jwts.builder()
                .setClaims(claims)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secretKey);

        // Generate the JWT token
        return jwtBuilder.compact();
    }

    public static void generateSecretKey() {
        // Generate a secure random key
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256 bits key length
        secureRandom.nextBytes(keyBytes);
        secretKey = Base64.getEncoder().encodeToString(keyBytes);
    }

}
