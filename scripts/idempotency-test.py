import argparse
import concurrent.futures
import json
import urllib.request
import urllib.error
from collections import Counter


def make_request(base_url, show_id, seat, user_id, idempotency_key):
    url = f"{base_url}/shows/{show_id}/reserve"

    data = json.dumps({
        "seats": [seat]
    }).encode("utf-8")

    request = urllib.request.Request(
        url,
        data=data,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {user_id}",
            "Idempotency-Key": idempotency_key
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = response.read().decode("utf-8")

            return {
                "status": response.status,
                "body": body
            }

    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8")

        return {
            "status": error.code,
            "body": body
        }

    except Exception as error:
        return {
            "status": 0,
            "body": str(error)
        }


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--base-url",
        default="http://localhost:8080"
    )

    parser.add_argument(
        "--show-id",
        required=True
    )

    parser.add_argument(
        "--seat",
        default="I1"
    )

    parser.add_argument(
        "--requests",
        type=int,
        default=1000
    )

    parser.add_argument(
        "--workers",
        type=int,
        default=100
    )

    args = parser.parse_args()

    print()
    print("Starting idempotency concurrency test...")
    print(f"Show:     {args.show_id}")
    print(f"Seat:     {args.seat}")
    print(f"Requests: {args.requests}")
    print(f"Workers:  {args.workers}")
    print()

    user_id = "idempotency-test-user"
    idempotency_key = "idempotency-concurrency-test-001"

    with concurrent.futures.ThreadPoolExecutor(
            max_workers=args.workers
    ) as executor:

        futures = [
            executor.submit(
                make_request,
                args.base_url,
                args.show_id,
                args.seat,
                user_id,
                idempotency_key
            )
            for _ in range(args.requests)
        ]

        results = [
            future.result()
            for future in concurrent.futures.as_completed(futures)
        ]

    status_counts = Counter(
        result["status"]
        for result in results
    )

    reservation_ids = []

    for result in results:
        if result["status"] != 201:
            continue

        try:
            body = json.loads(result["body"])
            reservation_ids.append(body.get("reservationId"))
        except json.JSONDecodeError:
            pass

    unique_reservation_ids = set(reservation_ids)

    print("RESULTS")
    print("-------")
    print(f"201 responses: {status_counts[201]}")
    print(f"409 responses: {status_counts[409]}")
    print(f"5xx responses: {sum(v for k, v in status_counts.items() if k >= 500)}")
    print(f"Other responses: {sum(v for k, v in status_counts.items() if k not in (201, 409) and k < 500)}")

    print()
    print("IDEMPOTENCY")
    print("-----------")
    print(f"Unique reservation IDs: {len(unique_reservation_ids)}")

    if len(unique_reservation_ids) == 1:
        print("PASS: all successful requests returned the same reservation")
    else:
        print("FAIL: multiple reservation IDs were created")

    print()

    if status_counts[201] == args.requests and len(unique_reservation_ids) == 1:
        print("PASS: exactly-once idempotency holds under concurrency")
    else:
        print("FAIL: exactly-once idempotency invariant violated")


if __name__ == "__main__":
    main()