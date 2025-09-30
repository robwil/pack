#!/usr/bin/env python3
"""
ADB Backup Extractor for Android SQLite Database
Extracts SQLite database from .ab backup files
"""

import sys
import tarfile
import zlib
import struct
import os

def extract_ab_file(ab_file_path, output_dir):
    """Extract Android backup file (.ab) to get the SQLite database"""

    print(f"Extracting backup from: {ab_file_path}")

    # Read the .ab file
    with open(ab_file_path, 'rb') as f:
        # Skip the header (usually "ANDROID BACKUP\n" + version info)
        header = f.readline().decode('utf-8').strip()
        print(f"Backup header: {header}")

        if not header.startswith('ANDROID BACKUP'):
            print("ERROR: Not a valid Android backup file")
            return False

        # Skip version, compression, and encryption lines
        version = f.readline().decode('utf-8').strip()
        compression = f.readline().decode('utf-8').strip()
        encryption = f.readline().decode('utf-8').strip()

        print(f"Version: {version}")
        print(f"Compression: {compression}")
        print(f"Encryption: {encryption}")

        if encryption != 'none':
            print("ERROR: Encrypted backups are not supported by this script")
            return False

        # The rest is compressed data
        compressed_data = f.read()

    # Decompress the data
    print("Decompressing backup data...")
    try:
        if compression == '1':  # zlib compression
            decompressed_data = zlib.decompress(compressed_data)
        else:  # no compression
            decompressed_data = compressed_data
    except Exception as e:
        print(f"ERROR: Failed to decompress: {e}")
        return False

    # Write to tar file and extract
    tar_path = os.path.join(output_dir, 'backup.tar')

    print("Writing decompressed data to tar file...")
    with open(tar_path, 'wb') as f:
        f.write(decompressed_data)

    # Extract tar file
    print("Extracting tar file...")
    try:
        with tarfile.open(tar_path, 'r') as tar:
            tar.extractall(output_dir)

        # Clean up tar file
        os.remove(tar_path)

        print(f"Backup extracted to: {output_dir}")

        # Look for SQLite databases
        find_sqlite_databases(output_dir)
        return True

    except Exception as e:
        print(f"ERROR: Failed to extract tar: {e}")
        return False

def find_sqlite_databases(directory):
    """Find SQLite database files in the extracted backup"""

    print("\nLooking for SQLite databases...")

    for root, dirs, files in os.walk(directory):
        for file in files:
            file_path = os.path.join(root, file)

            # Check if it's a SQLite database
            try:
                with open(file_path, 'rb') as f:
                    header = f.read(16)
                    if header.startswith(b'SQLite format 3'):
                        print(f"Found SQLite database: {file_path}")

                        # If it's the pack database, copy it to easy location
                        if 'pack' in file.lower() or 'database' in file.lower():
                            copy_path = os.path.join(os.path.dirname(directory), 'pack_database.db')
                            import shutil
                            shutil.copy2(file_path, copy_path)
                            print(f"Copied pack database to: {copy_path}")

            except:
                continue

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 extract_backup.py <backup_file.ab> <output_directory>")
        print("Example: python3 extract_backup.py pack_backup.ab ./extracted/")
        sys.exit(1)

    backup_file = sys.argv[1]
    output_dir = sys.argv[2]

    # Create output directory if it doesn't exist
    os.makedirs(output_dir, exist_ok=True)

    if os.path.exists(backup_file):
        success = extract_ab_file(backup_file, output_dir)
        if success:
            print("\n✅ Extraction completed successfully!")
            print(f"Check {output_dir} for your extracted files")
        else:
            print("\n❌ Extraction failed")
    else:
        print(f"ERROR: Backup file not found: {backup_file}")
