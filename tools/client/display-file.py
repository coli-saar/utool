#!/usr/bin/env python3
"""Send one graph file to a running Utool desktop server for display."""

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
        description="Read a graph file and ask a Utool desktop server to display it."
    )
    parser.add_argument("file", type=Path, help="graph file to display")
    parser.add_argument("--host", default="localhost", help="server host (default: localhost)")
    parser.add_argument("--port", type=int, default=2802, help="server port (default: 2802)")
    parser.add_argument(
        "--input-codec",
        help="input codec; inferred from the filename when omitted",
    )
    parser.add_argument(
        "--name",
        help="graph name shown in the window title (default: input filename)",
    )
    parser.add_argument(
        "-f",
        "--filter",
        type=Path,
        help="rewrite-rule file to install and apply in the opened window",
    )
    parser.add_argument(
        "--filter-name",
        help="name shown for the filter in the opened window",
    )
    parser.add_argument("--timeout", type=float, default=30, help="socket timeout in seconds")
    return parser.parse_args()


def request_xml(
    graph: str,
    input_codec: str,
    name: str | None,
    filter_rules: str | None,
    filter_name: str | None,
) -> bytes:
    request = ET.Element("utool", {"cmd": "display"})
    attributes = {"codec": input_codec, "string": graph}
    if name:
        attributes["name"] = name
    ET.SubElement(request, "usr", attributes)
    if filter_rules is not None:
        filter_attributes = {"rules": filter_rules}
        if filter_name:
            filter_attributes["name"] = filter_name
        ET.SubElement(request, "filter", filter_attributes)
    # XML replaces literal whitespace in attributes with spaces. Numeric
    # character references are expanded after that normalization, preserving
    # the line structure required by // comments in rewrite-rule files.
    return (
        ET.tostring(request, encoding="utf-8")
        .replace(b"\r", b"&#13;")
        .replace(b"\n", b"&#10;")
        .replace(b"\t", b"&#9;")
    )


def main() -> int:
    options = arguments()
    if not 1 <= options.port <= 65535:
        print("error: --port must be between 1 and 65535", file=sys.stderr)
        return 2
    if options.filter_name and options.filter is None:
        print("error: --filter-name requires --filter", file=sys.stderr)
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
        filter_rules = (
            options.filter.read_text(encoding="utf-8")
            if options.filter is not None
            else None
        )
        request = request_xml(
            graph,
            input_codec,
            options.name or options.file.name,
            filter_rules,
            options.filter_name,
        )
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

    try:
        result = ET.fromstring(response)
    except ET.ParseError as error:
        print(f"error: server returned invalid XML: {error}", file=sys.stderr)
        return 1
    if result.tag == "error":
        explanation = result.get("explanation", "unknown server error")
        print(f"server error {result.get('code', '?')}: {explanation}", file=sys.stderr)
        return 1

    filter_note = f" with filter {options.filter}" if options.filter is not None else ""
    print(f"Displayed {options.file}{filter_note} via {options.host}:{options.port}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
