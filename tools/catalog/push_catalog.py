#!/usr/bin/env python3
"""Upload curated products to the Firestore `catalog` collection (the app's "New arrivals").

Clients can't write that collection (firestore.rules: `allow write: if false`), so this uses YOUR
Google account's OAuth token, which bypasses rules for project owners/editors:

    gcloud auth login you@gmail.com            # the account that owns the Firebase project
    python3 tools/catalog/push_catalog.py --project amazgone-251d6 \
        shared/src/commonMain/composeResources/files/seed/new_arrivals.json

Each product becomes catalog/{id}; re-running overwrites it (idempotent). Use --dry-run to preview.
"""
import argparse
import json
import subprocess
import sys
import urllib.request

API = "https://firestore.googleapis.com/v1/projects/{project}/databases/(default)/documents/catalog/{id}"


def to_value(value):
    if value is None:
        return {"nullValue": None}
    if isinstance(value, bool):
        return {"booleanValue": value}
    if isinstance(value, int):
        return {"integerValue": str(value)}
    if isinstance(value, float):
        return {"doubleValue": value}
    if isinstance(value, str):
        return {"stringValue": value}
    if isinstance(value, list):
        return {"arrayValue": {"values": [to_value(v) for v in value]}}
    if isinstance(value, dict):
        return {"mapValue": {"fields": {k: to_value(v) for k, v in value.items()}}}
    raise TypeError(f"unsupported value {value!r}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("file", help="JSON file shaped like new_arrivals.json")
    parser.add_argument("--project", required=True, help="Firebase project id")
    parser.add_argument("--account", help="gcloud account to use (defaults to the active one)")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    with open(args.file) as f:
        products = json.load(f)["products"]

    token = None
    if not args.dry_run:
        cmd = ["gcloud", "auth", "print-access-token"] + ([args.account] if args.account else [])
        token = subprocess.check_output(cmd, text=True).strip()

    for product in products:
        pid = product["id"]
        fields = {k: to_value(v) for k, v in product.items() if k != "id"}
        body = json.dumps({"fields": fields}).encode()
        url = API.format(project=args.project, id=pid)
        if args.dry_run:
            print(f"would PATCH catalog/{pid} ({len(fields)} fields)")
            continue
        request = urllib.request.Request(url, data=body, method="PATCH", headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "X-Goog-User-Project": args.project,
        })
        try:
            with urllib.request.urlopen(request) as response:
                print(f"catalog/{pid}: HTTP {response.status}")
        except urllib.error.HTTPError as e:
            print(f"catalog/{pid}: HTTP {e.code} {e.read().decode()[:300]}", file=sys.stderr)
            sys.exit(1)


if __name__ == "__main__":
    main()
