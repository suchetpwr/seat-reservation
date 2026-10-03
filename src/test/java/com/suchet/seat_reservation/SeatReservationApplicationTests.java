package com.suchet.seat_reservation;

import com.suchet.seat_reservation.dto.ReserveRequest;
import com.suchet.seat_reservation.exception.ReservationConflictException;
import com.suchet.seat_reservation.service.RequestHashService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatReservationApplicationTests {

	private final RequestHashService requestHashService =
			new RequestHashService();

	@Test
	void sameRequestProducesSameHash() {

		ReserveRequest request1 =
				new ReserveRequest(List.of("A1", "A2"));

		ReserveRequest request2 =
				new ReserveRequest(List.of("A1", "A2"));

		String hash1 = requestHashService.hash(request1);
		String hash2 = requestHashService.hash(request2);

		assertEquals(hash1, hash2);
	}

	@Test
	void seatOrderDoesNotChangeRequestHash() {

		ReserveRequest request1 =
				new ReserveRequest(List.of("A1", "A2"));

		ReserveRequest request2 =
				new ReserveRequest(List.of("A2", "A1"));

		String hash1 = requestHashService.hash(request1);
		String hash2 = requestHashService.hash(request2);

		assertEquals(hash1, hash2);
	}

	@Test
	void differentSeatsProduceDifferentHash() {

		ReserveRequest request1 =
				new ReserveRequest(List.of("A1"));

		ReserveRequest request2 =
				new ReserveRequest(List.of("A2"));

		String hash1 = requestHashService.hash(request1);
		String hash2 = requestHashService.hash(request2);

		assertNotEquals(hash1, hash2);
	}

	@Test
	void reservationConflictContainsCorrectReason() {

		ReservationConflictException exception =
				new ReservationConflictException("seat_taken");

		assertEquals(
				"seat_taken",
				exception.getMessage()
		);
	}
}