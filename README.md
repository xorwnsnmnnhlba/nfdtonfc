# nfdtonfc

A command-line tool that converts filenames from NFD to NFC Unicode normalization.

Useful when dealing with files created on macOS (HFS+), which stores filenames in NFD form, causing compatibility issues on Linux and Windows systems that expect NFC.

## Installation

Download the installer for your platform from the [Releases](https://github.com/xorwnsnmnnhlba/nfdtonfc/releases) page.

### macOS

Open the `.dmg` file and follow the installation instructions.

### Windows

Run the `.msi` file and follow the installation instructions.

### Ubuntu / WSL

```bash
sudo apt install ./nfdtonfc-<version>-ubuntu.deb
```

## Usage

```
nfdtonfc <file-or-directory> [file-or-directory ...]
```

### Examples

```bash
# Convert a single file
nfdtonfc file.txt

# Convert all files in a directory recursively
nfdtonfc ~/Downloads

# Convert multiple targets at once
nfdtonfc dir1 dir2 file.txt
```

### Options

| Option | Description |
|--------|-------------|
| `--help` | Show help message and exit |
| `--version` | Show version and exit |

## Build

Requires JDK 25 (Zulu).

```bash
# Build installer for current OS
./gradlew jpackageInstaller
```

| OS | Output |
|----|--------|
| macOS | `build/jpackage/nfdtonfc-<version>.dmg` |
| Windows | `build/jpackage/nfdtonfc-<version>.msi` |
| Ubuntu | `build/jpackage/nfdtonfc_<version>_amd64.deb` |

## Uninstallation

### macOS

Drag `nfdtonfc` from `/Applications` to Trash.

### Windows

Uninstall via **Settings > Apps**.

### Ubuntu / WSL

```bash
sudo apt remove nfdtonfc
```
