package com.suchet.seat_reservation.service;

import com.suchet.seat_reservation.dto.ReserveRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class RequestHashService {

    public String hash(ReserveRequest request) {

        List<String> sortedSeats = request.seats()
                .stream()
                .sorted()
                .toList();

        String canonicalRequest = String.join(",", sortedSeats);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}