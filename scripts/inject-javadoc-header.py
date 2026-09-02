#!/usr/bin/env python3

import os
import re
import sys
from pathlib import Path


def site_prefix(page: Path, site_root: Path) -> str:
    relative = os.path.relpath(site_root, page.parent).replace(os.sep, "/")
    return "" if relative == "." else relative + "/"


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: inject-javadoc-header.py APIDOC_DIR SITE_ROOT", file=sys.stderr)
        return 2

    apidoc_dir = Path(sys.argv[1]).resolve()
    site_root = Path(sys.argv[2]).resolve()

    for page in apidoc_dir.rglob("*.html"):
        html = page.read_text(encoding="utf-8")
        prefix = site_prefix(page, site_root)
        stylesheet = (
            f'<link rel="stylesheet" type="text/css" '
            f'href="{prefix}site-header.css" title="Utool site style">'
        )
        header = f'''\n<header class="utool-site-header">
  <a href="{prefix}index.html"><img src="{prefix}assets/utool.png" alt="Utool"></a>
  <nav aria-label="Main navigation">
    <a href="{prefix}index.html">Home</a>
    <a href="https://github.com/coli-saar/utool/releases">Download</a>
    <a href="{prefix}manual/index.html">Manual</a>
    <a aria-current="page" href="{prefix}api/index.html">API documentation</a>
    <a href="https://github.com/coli-saar/utool">Source code</a>
  </nav>
</header>'''

        html, head_count = re.subn(
            r"</head>", stylesheet + "\n</head>", html, count=1, flags=re.IGNORECASE
        )
        html, body_count = re.subn(
            r"(<body\b[^>]*>)", r"\1" + header, html, count=1, flags=re.IGNORECASE
        )
        if head_count != 1 or body_count != 1:
            print(f"could not inject header into {page}", file=sys.stderr)
            return 1
        page.write_text(html, encoding="utf-8")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
