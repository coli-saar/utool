#!/usr/bin/env python3
"""Send one graph file to a running Utool server for solving."""

from __future__ import annotations

import argparse
import socket
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


CODECS_BY_SUFFIX = (
    (".mrs.pl", "mrs-prolog"),
    (".hs.pl", "holesem-comsem"),
    (".mrs.xml", "mrs-xml"),
    (".dg.xml", "domgraph-gxl"),
    (".clls", "domcon-oz"),
)


def inferred_codec(path: Path) -> str | None:
    name = path.name.lower()
    return next((codec for suffix, codec in CODECS_BY_SUFFIX if name.endswith(suffix)), None)


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Read a graph file and send a solve request to a Utool server."
    )
    parser.add_argument("file", type=Path, help="graph file to solve")
    parser.add_argument("--host", default="localhost", help="server host (default: localhost)")
    parser.add_argument("--port", type=int, default=2802, help="server port (default: 2802)")
    parser.add_argument(
        "--input-codec",
        help="input codec; inferred from the filename when omitted",
    )
    parser.add_argument(
        "--output-codec",
        default="term-prolog",
        help="solution codec (default: term-prolog)",
    )
    parser.add_argument("--limit", type=int, help="maximum number of solutions to return")
    parser.add_argument("--timeout", type=float, default=30, help="socket timeout in seconds")
    return parser.parse_args()


def request_xml(graph: str, input_codec: str, output_codec: str, limit: int | None) -> bytes:
    attributes = {"cmd": "solve", "output-codec": output_codec}
    if limit is not None:
        attributes["limit"] = str(limit)
    request = ET.Element("utool", attributes)
    ET.SubElement(request, "usr", {"codec": input_codec, "string": graph})
    return ET.tostring(request, encoding="utf-8")


def main() -> int:
    options = arguments()
    if not 1 <= options.port <= 65535:
        print("error: --port must be between 1 and 65535", file=sys.stderr)
        return 2
    if options.limit is not None and options.limit < 0:
        print("error: --limit must be nonnegative", file=sys.stderr)
        return 2

    input_codec = options.input_codec or inferred_codec(options.file)
    if input_codec is None:
        print(
            f"error: cannot infer a codec from {options.file.name}; use --input-codec",
            file=sys.stderr,
        )
        return 2

    try:
        graph = options.file.read_text(encoding="utf-8")
        request = request_xml(graph, input_codec, options.output_codec, options.limit)
        with socket.create_connection((options.host, options.port), options.timeout) as connection:
            connection.settimeout(options.timeout)
            connection.sendall(request)
            connection.shutdown(socket.SHUT_WR)
            chunks = []
            while chunk := connection.recv(65536):
                chunks.append(chunk)
        response = b"".join(chunks).decode("utf-8")
    except (OSError, UnicodeError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    print(response, end="" if response.endswith("\n") else "\n")
    try:
        result = ET.fromstring(response)
    except ET.ParseError as error:
        print(f"error: server returned invalid XML: {error}", file=sys.stderr)
        return 1
    if result.tag == "error":
        explanation = result.get("explanation", "unknown server error")
        print(f"server error {result.get('code', '?')}: {explanation}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
