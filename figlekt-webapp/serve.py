#!/usr/bin/env python3
"""Serves the Kotlin/Wasm web page and proxies the FIGlet font database.

    ./gradlew :figlekt-webapp:wasmJsBrowserDistribution
    python3 figlekt-webapp/serve.py --port 8085
"""

import argparse
import http.server
import os
import re
import socketserver
import sys
import threading
import urllib.error
import urllib.request

DEFAULT_DIST = os.path.join(os.path.dirname(__file__), "build", "dist", "wasmJs", "productionExecutable")
UPSTREAM = "https://www.figlet.org/fonts/"
FILENAME = re.compile(r"^[A-Za-z0-9._-]+\.(flf|tlf)$")
CACHE = os.path.join(os.path.dirname(__file__), "build", "font-cache")

_lock = threading.Lock()


def fetch(filename: str) -> bytes:
    """Returns the font, from the cache when available."""
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, filename)
    with _lock:
        if os.path.isfile(path):
            with open(path, "rb") as cached:
                return cached.read()
    request = urllib.request.Request(UPSTREAM + filename, headers={"User-Agent": "figlekt-font-generator"})
    with urllib.request.urlopen(request, timeout=30) as response:
        content = response.read()
    if not content.startswith(b"flf2a"):
        raise ValueError("not a FIGlet font")
    with _lock:
        with open(path, "wb") as cached:
            cached.write(content)
    return content


class Handler(http.server.SimpleHTTPRequestHandler):
    dist = DEFAULT_DIST

    def do_GET(self) -> None:
        if self.path.startswith("/figlet/"):
            filename = self.path[len("/figlet/"):].split("?")[0]
            if not FILENAME.match(filename):
                self.send_error(400, "Invalid font name")
                return
            try:
                content = fetch(filename)
            except (urllib.error.URLError, OSError, ValueError) as e:
                self.send_error(404, f"Unable to download {filename}: {e}")
                return
            self.send_response(200)
            self.send_header("Content-Type", "application/octet-stream")
            self.send_header("Content-Length", str(len(content)))
            self.send_header("Cache-Control", "public, max-age=86400")
            self.end_headers()
            self.wfile.write(content)
            return
        super().do_GET()

    def end_headers(self) -> None:
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write("%s %s\n" % (self.address_string(), fmt % args))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8085)
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--dist", default=DEFAULT_DIST)
    args = parser.parse_args()

    if not os.path.isfile(os.path.join(args.dist, "index.html")):
        sys.exit(f"No built web page in {args.dist}, run :webapp:wasmJsBrowserDistribution first.")

    Handler.dist = args.dist

    class Server(socketserver.ThreadingTCPServer):
        allow_reuse_address = True
        daemon_threads = True

    with Server((args.host, args.port), lambda *a, **kw: Handler(*a, directory=args.dist, **kw)) as server:
        print(f"figlekt on http://{args.host}:{args.port} serving {args.dist}", flush=True)
        server.serve_forever()


if __name__ == "__main__":
    main()