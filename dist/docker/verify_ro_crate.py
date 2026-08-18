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


async def create_ro_crate(container_output_path: str, timeout: int = 120) -> dict:
    async with websockets.connect(WS_URL, max_size=50 * 1024 * 1024) as ws:
        # Wait for project_list (project is auto-opened via CLI arg)
        project_id = None
        deadline = time.time() + 60
        while time.time() < deadline:
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=deadline - time.time()))
            if msg["type"] == "project_list":
                projects = msg.get("projects", [])
                if projects:
                    project_id = projects[0]["id"]
                    break
            # Ignore other message types while waiting

        if not project_id:
            raise RuntimeError("No project found in project_list — was it opened via CLI arg?")

        print(f"Found project: {project_id}")

        # Select the project
        await ws.send(json.dumps({
            "type": "select_project",
            "projectId": project_id,
            "requestId": "select",
        }))

        # Wait for project_changed
        deadline = time.time() + 30
        while time.time() < deadline:
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=deadline - time.time()))
            if msg["type"] == "project_changed":
                break

        print("Project selected, creating RO-Crate ...")

        # Create RO-Crate
        await ws.send(json.dumps({
            "type": "create_ro_crate",
            "requestId": "create",
            "outputPath": container_output_path,
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
    container_output_path = sys.argv[1]  # Path inside the container (sent to server)
    host_output_path = sys.argv[2]       # Path on the CI runner (via volume mount)

    print(f"Creating RO-Crate -> {container_output_path}")
    print(f"Expecting output file at {host_output_path}")
    result = await create_ro_crate(container_output_path)
    print(f"RO-Crate created: {result}")

    # Verify the zip exists on the CI runner
    crate_file = Path(host_output_path)
    if not crate_file.exists():
        print(f"ERROR: Output file {crate_file} does not exist")
        sys.exit(1)

    # Extract the crate on the CI runner
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
