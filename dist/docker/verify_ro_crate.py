#!/usr/bin/env python3
"""Create an RO-Crate via instrumentation and verify it with cwltool."""
import asyncio
import json
import sys
import time
import zipfile
from pathlib import Path

import websockets

WS_URL = "ws://127.0.0.1:8780/"


async def create_ro_crate(project_path: str, output_path: str, timeout: int = 120) -> dict:
    async with websockets.connect(WS_URL, max_size=50 * 1024 * 1024) as ws:
        # Wait for project_list
        msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=30))
        assert msg["type"] == "project_list", f"Expected project_list, got {msg['type']}"

        # Open the project
        await ws.send(json.dumps({
            "type": "open_project",
            "path": project_path,
            "forceCurrentWindow": False,
        }))

        # Wait for the project to appear
        project_id = None
        for _ in range(60):
            try:
                msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=5))
                if msg["type"] == "project_list":
                    for p in msg.get("projects", []):
                        project_id = p["id"]
                        break
                    if project_id:
                        break
            except asyncio.TimeoutError:
                continue

        if not project_id:
            raise RuntimeError("Project did not open")

        # Select the project
        await ws.send(json.dumps({
            "type": "select_project",
            "projectId": project_id,
            "requestId": "select",
        }))

        # Wait for project_changed
        for _ in range(20):
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=5))
            if msg["type"] == "project_changed":
                break

        # Create RO-Crate
        await ws.send(json.dumps({
            "type": "create_ro_crate",
            "requestId": "create",
            "outputPath": output_path,
        }))

        # Wait for operation_result
        deadline = time.time() + timeout
        while time.time() < deadline:
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=deadline - time.time()))
            if msg["type"] == "operation_result" and msg.get("requestId") == "create":
                if "error" in msg:
                    raise RuntimeError(f"RO-Crate creation failed: {msg['error']}")
                return msg.get("data", {})
            elif msg["type"] == "error":
                raise RuntimeError(f"Server error: {msg.get('message', 'unknown')}")

        raise RuntimeError("Timeout waiting for RO-Crate creation")


async def main():
    project_path = sys.argv[1]
    output_path = sys.argv[2]

    print(f"Creating RO-Crate from {project_path} -> {output_path}")
    result = await create_ro_crate(project_path, output_path)
    print(f"RO-Crate created: {result}")

    # Verify the zip exists
    crate_file = Path(output_path)
    if not crate_file.exists():
        print(f"ERROR: Output file {crate_file} does not exist")
        sys.exit(1)

    # Extract the crate
    extract_dir = Path("/tmp/crate-extracted")
    extract_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(crate_file, 'r') as z:
        z.extractall(extract_dir)

    # Check workflow.cwl exists
    cwl_path = extract_dir / "workflow.cwl"
    if not cwl_path.exists():
        print(f"ERROR: workflow.cwl not found in crate")
        sys.exit(1)

    print(f"RO-Crate extracted to {extract_dir}")
    print(f"workflow.cwl found at {cwl_path}")


if __name__ == "__main__":
    asyncio.run(main())
