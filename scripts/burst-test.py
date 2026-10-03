import argparse
import concurrent.futures
import json
import threading
import urllib.error
import urllib.request
from collections import Counter


def make_request(
        base_url,
        show_id,
        seat,
        user_id,
        idempotency_key
):
    url = f"{base_url}/shows/{show_id}/reserve"

    payload = json.dumps({
        "seats": [seat]
    }).encode("utf-8")

    request = urllib.request.Request(
        url,
        data=payload,
        method="POST",
        headers={
            "Authorization": f"Bearer {user_id}",
            "Idempotency-Key": idempotency_key,
            "Content-Type": "application/json"
        }
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


def run_burst(
        base_url,
        show_id,
        seat,
        requests,
        workers
):
    results = []

    print()
    print("Starting hot-seat burst...")
    print(f"Show:      {show_id}")
    print(f"Seat:      {seat}")
    print(f"Requests:  {requests}")
    print(f"Workers:   {workers}")
    print()

    with concurrent.futures.ThreadPoolExecutor(
            max_workers=workers
    ) as executor:

        futures = []

        for i in range(requests):

            user_id = f"load-test-user-{i}"

            idempotency_key = f"load-test-{seat}-{i}"

            futures.append(
                executor.submit(
                    make_request,
                    base_url,
                    show_id,
                    seat,
                    user_id,
                    idempotency_key
                )
            )

        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    return results


def get_show(
        base_url,
        show_id
):
    url = f"{base_url}/shows/{show_id}"

    request = urllib.request.Request(
        url,
        method="GET"
    )

    with urllib.request.urlopen(
            request,
            timeout=30
    ) as response:

        return json.loads(
            response.read().decode("utf-8")
        )


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
        default="L1"
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

    results = run_burst(
        args.base_url,
        args.show_id,
        args.seat,
        args.requests,
        args.workers
    )

    status_counts = Counter(
        result["status"]
        for result in results
    )

    confirmed_count = status_counts[201]
    declined_count = status_counts[409]

    server_error_count = sum(
        count
        for status, count in status_counts.items()
        if status >= 500
    )

    other_error_count = sum(
        count
        for status, count in status_counts.items()
        if status not in (201, 409) and status < 500
    )

    decline_reasons = Counter()

    for result in results:

        if result["status"] != 409:
            continue

        try:
            body = json.loads(result["body"])

            reason = body.get(
                "error",
                "unknown"
            )

            decline_reasons[reason] += 1

        except json.JSONDecodeError:
            decline_reasons["invalid_response"] += 1

    print()
    print("RESULTS")
    print("-------")
    print(f"201 confirmed: {confirmed_count}")
    print(f"409 declined:  {declined_count}")
    print(f"5xx errors:    {server_error_count}")
    print(f"Other errors:  {other_error_count}")

    print()
    print("DECLINE REASONS")
    print("---------------")

    for reason, count in decline_reasons.items():
        print(f"{reason}: {count}")

    print()
    print("FINAL SHOW STATE")
    print("----------------")

    show = get_show(
        args.base_url,
        args.show_id
    )

    print(
        json.dumps(
            show,
            indent=2
        )
    )

    total = show["totalSeats"]
    available = show["availableSeats"]
    held = show["heldSeats"]
    confirmed = show["confirmedSeats"]

    print()
    print("RECONCILIATION")
    print("--------------")

    reconciliation_total = (
            available
            + held
            + confirmed
    )

    print(
        f"available + held + confirmed = "
        f"{available} + {held} + {confirmed} = "
        f"{reconciliation_total}"
    )

    print(f"total seats = {total}")

    if reconciliation_total == total:
        print("PASS: seat reconciliation invariant holds")
    else:
        print("FAIL: seat reconciliation invariant violated")

if __name__ == "__main__":
    main()