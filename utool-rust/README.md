# Utool 4 for Rust

Utool is the Swiss Army Knife of Underspecification. It parses, solves,
converts, classifies, lays out, and filters dominance graphs and related
underspecified semantic representations. 

This directory contains the source code for Utool 4, the Rust reimplementation of 2026. It consists of a command-line tool called `utool` and a desktop application calles `utool-display`. The command-line tool is intended as a drop-in replacement for the Utool 3 command-line program; the desktop application is a streamlined version of the Utool 3 GUI.

Utool 3.x, the older Java implementation, remains in the repository root. The
Rust and Java builds are independent. Commands in this README assume that the
current directory is `utool-rust/` unless stated otherwise.

## Install a release

Prebuilt command-line programs and desktop packages are published on the
[GitHub Releases page](https://github.com/coli-saar/utool/releases). Release
artifacts are built for these targets:

<!-- release-downloads:start version=4.0.0-alpha9 -->
| Platform | Desktop application | Command line program |
| --- | --- | --- |
| macOS, Apple Silicon | [DMG](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/Utool_4.0.0-alpha9_aarch64.dmg) · [app archive](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/Utool_aarch64.app.tar.gz) | [tar.gz](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/utool-4.0.0-alpha9-aarch64-apple-darwin.tar.gz) |
| macOS, Intel | [DMG](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/Utool_4.0.0-alpha9_x64.dmg) · [app archive](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/Utool_x64.app.tar.gz) | [tar.gz](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/utool-4.0.0-alpha9-x86_64-apple-darwin.tar.gz) |
| Windows x64 | [installer](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/Utool_4.0.0-alpha9_x64-setup.exe) | [zip](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/utool-4.0.0-alpha9-x86_64-pc-windows-msvc.zip) |
| Linux x64 | [AppImage](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/Utool_4.0.0-alpha9_amd64.AppImage) · [Debian package](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/Utool_4.0.0-alpha9_amd64.deb) | [tar.gz](https://github.com/coli-saar/utool/releases/download/v4.0.0-alpha9/utool-4.0.0-alpha9-x86_64-unknown-linux-gnu.tar.gz) |
<!-- release-downloads:end -->

The packages are not developer-signed or notarized. Windows SmartScreen and
macOS Gatekeeper may therefore ask you to approve the application before its
first launch. Download releases only from the repository's Releases page and
verify that you selected the expected version and architecture.

The binaries you have to approve are called `utool` and `utool-display`.


### Install the command-line program

For the most similar experience with Utool 3.x, install the command-line program. These are a drop-in replacement: Instead of `java -jar utool.jar ...`, you simply run `utool ...`, and everything else should mostly be the same.

On macOS or Linux, extract the archive for your target and place both `utool`
and `utool-display` in the same directory on `PATH`. For a per-user installation:

```sh
tar -xzf utool-VERSION-TARGET.tar.gz
mkdir -p "$HOME/.local/bin"
install -m 755 utool utool-display "$HOME/.local/bin/"
```

Ensure that `$HOME/.local/bin` is on `PATH`, then verify the installation:

```sh
utool --version
utool --display-codecs
```

On Windows, extract `utool.exe` and `utool-display.exe` from the x64 ZIP archive,
keep them together, and either run them there or add that directory to `PATH`.


### Install the desktop application

If you primarily work with the GUI desktop application, you can also download a prepackaged app.

On macOS, download the DMG for your processor, open it, and copy `Utool.app` to
`Applications`. If Gatekeeper blocks the first launch, use the normal macOS
Privacy & Security controls to approve the downloaded application.

On Windows, download and run the x64 NSIS setup executable. Stable releases may
also provide an MSI. Alpha and other textual prerelease versions do not provide
an MSI because Windows Installer accepts only a numeric prerelease identifier.

On Debian or Ubuntu, install the downloaded package with:

```sh
sudo apt install ./Utool_VERSION_amd64.deb
```

On other compatible Linux distributions, download the AppImage, make it
executable, and run it:

```sh
chmod +x Utool_VERSION_amd64.AppImage
./Utool_VERSION_amd64.AppImage
```

Release filenames contain the actual version in place of `VERSION` and may
vary slightly with the Tauri bundler version.

## Use the command-line program

Display the available commands, global options, and codecs with:

```sh
utool --help
utool --help-options
utool --display-codecs
```

The main operations are `solve`, `solvable`, `convert`, `classify`, `display`,
and `server`. The `display` command starts the bundled `utool-display` companion,
which must remain in the same directory as `utool`.

Utool normally infers the input codec from a compound filename suffix. For
example, from the repository checkout:

```sh
utool solvable ../src/main/resources/examples/chain3.clls
utool solve --limit 2 ../src/main/resources/examples/chain3.clls
utool convert -O domgraph-dot \
  -o chain3.dg.dot \
  ../src/main/resources/examples/chain3.clls
utool solve -s -n ../src/main/resources/examples/rondane-1.mrs.pl
```

Use `-I` when reading from standard input or when a filename does not identify
its format:

```sh
utool convert -I domcon-oz -O domgraph-dot - < graph.clls
```

Apply a rewrite-system filter before enumerating solutions with `-f`:

```sh
utool solve -s -n \
  -f ../src/test/resources/server/filter-rules.txt \
  ../src/main/resources/examples/chain3.clls
```

`-s` prints timing and chart statistics to standard error. `-n` suppresses
solution output while retaining solving, filtering, enumeration, and
statistics. `--limit N` stops enumeration after `N` solutions.

For compatibility with earlier Utool versions, `solve` and `solvable` return
status `1` when the graph is solvable and `0` when it is unsolvable. Parse,
codec, I/O, and solver failures use other nonzero status codes. Do not interpret
status `1` as a generic process failure for these two operations.

### Supported codecs

The command-line program currently recognizes these input codecs:

- `chain`, a generated chain specified by its numeric length;
- `domcon-oz` (`.clls`);
- `domgraph-gxl` (`.dg.xml`);
- `holesem-comsem` (`.hs.pl`);
- `mrs-prolog` (`.mrs.pl`);
- `mrs-xml` (`.mrs.xml`).

Output codecs are `domcon-oz`, `domgraph-gxl`, `domgraph-dot`,
`domgraph-udraw`, `plugging-oz`, `plugging-lkb`, `term-prolog`, `term-oz`,
`domgraph-codegen`, and `plugging-groovy`. Not every output codec can represent
both underspecified graphs and sequences of solved forms. Utool reports an
error if an operation and output codec are incompatible.

## Differences from Java Utool 3

Utool 4 covers the main parse, solve, filter, enumerate, convert, server, and
desktop workflows, but it does not reproduce every historical Utool 3 feature.
In particular:

- `--input-codec-options` and `--output-codec-options` are accepted for command-line
  compatibility, but their values are currently ignored. Thus the Java options
  for MRS normalization and label style, DOT edge ordering, and uDraw pipe mode
  are not available in Rust.
- The experimental Java input codecs `glue` and `rmrs-domcon` are not implemented.
  The six production input codecs listed above and all ten Java output codecs
  are available.
- Quiet, unfiltered command-line `solvable` calls use the chart-free check
  whether or not `--nochart` is present, while
  `solvable --nochart --display-statistics` still constructs and reports a full
  chart. The XML server honors `nochart` independently of statistics.
- Rust implements the documented `classify` bit for compactifiability (value 8)
  and reports `compactifiable` in server responses. The Java executable defines
  and documents this bit but omits it from its command-line and server results,
  so classification exit codes and XML attributes can differ.
- Java's `ex:name` references to examples configured through `ExampleManager`
  are not supported. Pass an example's filename instead.
- The XML server implements the Java request and response shapes for solving,
  conversion, classification, filtering, help, codec discovery, and version
  information. A server `display` request only returns the legacy success
  acknowledgement; it does not open a desktop window. Server codec-option
  attributes are also not implemented.
- The Rust desktop focuses on opening graphs, browsing built-in examples,
  solving and filtering them, inspecting charts, browsing solutions, moving
  fragments, zooming, copying or exporting in the supported codecs, SVG export,
  pasting clipboard text with any graph input codec, and closing all graph
  windows. Unlike the Java workbench, it does not currently provide tab
  duplication, PDF/raster export and printing, node-name/label display modes,
  layout selection/reset, preferences and server controls, or manual deletion
  of chart splits. Rust uses a separate window per graph rather than Java's
  document tabs.
- The Rust crate provides a streamlined API for graph construction, codecs,
  solving, filtering, layouts, and the server. It is not source-compatible with
  the Java library and does not port the extensible codec manager, RTG parser and
  interchange APIs, weighted grammar packages, the full mutable graph API, or
  the Swing chart and layout class hierarchy. Graph layouts preserve the key
  structural invariants, but exact visual compatibility with every Java tower
  heuristic is not guaranteed.

## Build the command-line program and library

### Prerequisites

Install a stable Rust toolchain with
[`rustup`](https://www.rust-lang.org/tools/install/). The core crate declares a
minimum supported Rust version of 1.86. A current stable toolchain is the normal
choice:

```sh
rustup toolchain install stable
rustup default stable
rustc --version
cargo --version
```

You also need the platform's native linker. On macOS, install the Xcode Command
Line Tools with `xcode-select --install`. On Linux, install GCC or Clang; Debian
and Ubuntu provide the usual linker and compiler through `build-essential`. On
Windows, use the MSVC Rust toolchain and install Visual Studio Build Tools with
the **Desktop development with C++** workload.

The parser sources are generated automatically by `build.rs`. You do not need
to install Parol separately.

### Development build

Clone the repository and build the core crate:

```sh
git clone https://github.com/coli-saar/utool.git
cd utool/utool-rust
cargo build --locked
./target/debug/utool --version
```

The debug profile is suitable for development and debugging. It is much slower
than the optimized solver on large charts.

### Optimized build

Build the command-line program for normal use with:

```sh
cargo build --locked --release
./target/release/utool --version
```

Install that program into Cargo's binary directory directly from the checkout
with:

```sh
cargo install --locked --path .
```

This is a source installation. The `utool` crate is not assumed to be available
from crates.io.

### Use the library from another Rust project

During development, add the checkout as a path dependency:

```toml
[dependencies]
utool = { path = "../utool/utool-rust" }
```

The public API exposes graph parsing, codec selection, HNC graph validation,
solving, lazy solution enumeration, filtering, and graph/chart layout. Generate
local API documentation with:

```sh
cargo doc --no-deps --open
```

## Build the desktop application

The desktop application combines the Rust library with a React/TypeScript
frontend and Tauri 2. Building it requires Rust 1.88 or newer, Node.js LTS,
npm, and Tauri's platform dependencies. Consult the
[current Tauri prerequisite guide](https://v2.tauri.app/start/prerequisites/)
when a package name differs on your operating system.

### Platform prerequisites

On macOS, install the Xcode Command Line Tools:

```sh
xcode-select --install
```

On Windows, install:

1. Microsoft Visual Studio Build Tools with **Desktop development with C++**;
2. the MSVC Rust toolchain;
3. Microsoft Edge WebView2, unless it is already supplied by Windows.

Select the MSVC toolchain if necessary:

```powershell
rustup default stable-msvc
```

On Debian or Ubuntu, install the native Tauri dependencies:

```sh
sudo apt update
sudo apt install \
  libwebkit2gtk-4.1-dev \
  build-essential \
  curl \
  wget \
  file \
  libxdo-dev \
  libssl-dev \
  libayatana-appindicator3-dev \
  librsvg2-dev \
  patchelf
```

The Tauri prerequisite guide lists equivalent packages for Fedora, Arch Linux,
openSUSE, Gentoo, Alpine, and related distributions.

Install the current Node.js LTS release, then check both tools:

```sh
node --version
npm --version
```

### Run in development mode

Install the exact frontend dependencies recorded in `package-lock.json` and
start Tauri:

```sh
cd desktop
npm ci
npm run tauri dev
```

The default development profile retains debuggable application code while the
workspace configuration compiles the solver and its dependencies with
optimization. For a release-profile engine during interactive development:

```sh
npm run dev:release
```

To open graphs and apply a filter at startup, pass application arguments after
the npm and Tauri separators:

```sh
npm run tauri dev -- -- \
  -f ../../src/test/resources/server/filter-rules.txt \
  ../../src/main/resources/examples/chain3.clls
```

The desktop recognizes `.clls`, `.dg.xml`, `.hs.pl`, `.mrs.pl`, and `.mrs.xml`
input files. Each graph opens in its own window. **Edit → Paste as** opens
clipboard text as a new graph using the selected graph input codec, and
**File → Close All** closes every graph window. See
[`desktop/README.md`](desktop/README.md) for interaction details.

### Build the frontend only

Compile TypeScript and generate the Vite production assets with:

```sh
npm run build
```

This writes `desktop/dist/`. It does not compile Rust or create a desktop
application.

### Build a standalone desktop executable

From `utool-rust/desktop/`, run:

```sh
npm run build:standalone
```

The executable is written below `src-tauri/target/release/`. This command embeds
the frontend but deliberately skips application bundles and installers. Use the
Tauri bundle commands below for something intended for distribution.

Do not substitute a plain `cargo build` in `desktop/src-tauri/`: a plain Cargo
build does not run the complete Tauri frontend and asset packaging workflow.

### Build native application packages

Build packages on the operating system on which they will run. The release
workflow uses these commands and bundle selections.

macOS:

```sh
npm run tauri -- build --bundles app,dmg
```

Windows prerelease, including `4.0.0-alpha`:

```powershell
npm run tauri -- build --bundles nsis
```

Windows stable release:

```powershell
npm run tauri -- build --bundles nsis,msi
```

Linux:

```sh
npm run tauri -- build --bundles appimage,deb
```

Packages are written under
`desktop/src-tauri/target/release/bundle/`. Textual prerelease identifiers such
as `alpha` are valid for Cargo, npm, Tauri, NSIS, and the other configured
packages, but not for MSI. Attempting to bundle an MSI with this version fails
before publication.

To build both Mac architectures from macOS, install the second target and pass
it explicitly. For example, on Apple Silicon:

```sh
rustup target add x86_64-apple-darwin
npm run tauri -- build \
  --target x86_64-apple-darwin \
  --bundles app,dmg
```

## Test and check changes

Run the core Rust test suite from `utool-rust/`:

```sh
cargo test --locked
cargo fmt --check
cargo clippy --all-targets --all-features
```

Some compatibility tests compare against the Java Utool JAR when that JAR is
present in the repository's top-level `target/` directory. They skip that
comparison when the JAR is unavailable.

Check both halves of the desktop application from `utool-rust/desktop/`:

```sh
npm run build
cargo test --manifest-path src-tauri/Cargo.toml --lib
cargo check --manifest-path src-tauri/Cargo.toml
```

A successful frontend-only build does not prove that native bundling works.
Before changing release packaging, build at least one native bundle or exercise
the GitHub Actions matrix.

## Publish a GitHub release

The repository workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml)
runs when a tag beginning with `v` is pushed. It validates that the tag matches
the version in the core Cargo manifest, desktop Cargo manifest, npm manifest,
and Tauri configuration. For the current prerelease, the matching tag is:

```sh
v4.0.0-alpha
```

After committing the desired source and version changes, create and push the
tag:

```sh
git tag v4.0.0-alpha
git push origin master
git push origin v4.0.0-alpha
```

The workflow builds CLI archives and desktop packages on native GitHub-hosted
Linux, Windows, and macOS runners. Versions containing `-` become GitHub
prereleases. The Windows job omits MSI for those versions and restores it for
stable versions.

Each About dialog identifies an official build as
`RUN[.ATTEMPT]-SHORT_COMMIT`, using GitHub Actions' run number, optional retry
number, and commit hash. Local builds use `local-SHORT_COMMIT` and append
`-dirty` when tracked files differ from the current commit. Set
`UTOOL_BUILD_ID` while building to override this identifier.

## Troubleshooting

### Cargo reports that the compiler is too old

Update the stable toolchain:

```sh
rustup update stable
```

The core crate requires Rust 1.86; the desktop crate requires Rust 1.88.

### Linux cannot find WebKitGTK or another native library

Install the development packages listed under
[Platform prerequisites](#platform-prerequisites). If `pkg-config` still fails,
confirm that your distribution provides WebKitGTK 4.1 rather than only the
older 4.0 API.

### The desktop executable opens without the current frontend

Rebuild through the Tauri CLI. Running `cargo build` inside `src-tauri/` alone
can leave you with frontend assets from an earlier Vite build:

```sh
cd desktop
npm run tauri -- build --bundles app
```

Use an appropriate bundle selection for non-macOS platforms.

### A Windows prerelease fails while creating an MSI

Build only the NSIS package until the version is stable:

```powershell
npm run tauri -- build --bundles nsis
```

The tag-release workflow makes this choice automatically.

### A tag push does not produce a release

Confirm that the tag points to a commit containing the release workflow and
that the tag is exactly `v` followed by the version in all four manifests. A
workflow rerun uses the workflow definition from the tagged commit; committing
a repair only on `master` does not alter an already tagged run.

## License and further documentation

Utool is licensed under GPL-2.0-only. See the repository's
[`rootfiles/COPYING`](../rootfiles/COPYING) for the license text.

The [Utool website](https://coli-saar.github.io/utool/) contains the user manual
and background material. Repository-level history and instructions for the
Java implementation are in the [top-level README](../README.md).
