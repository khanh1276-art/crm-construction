"""
Launcher for Construction CRM Application.
Runs Uvicorn server on port 8088 (configurable to avoid conflicts with PM Luong).
"""
import sys
import io
import os
import argparse

# Ensure UTF-8 output on Windows console
if sys.platform == "win32":
    try:
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')
    except Exception:
        pass

import uvicorn

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Start Construction CRM Server")
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", os.environ.get("CRM_PORT", 8088))), help="Port to run server on")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Host interface")
    args = parser.parse_args()

    port = args.port
    host = args.host

    print("=" * 65)
    print("  [OK] KHOI DONG HE THONG CRM & CHAM SOC KHACH HANG XAY DUNG")
    print("  [*] Da nganh: Dan dung - Giao thong - Ha tang - Nang luong")
    print(f"  [*] Web Admin Portal:                  http://localhost:{port}")
    print(f"  [*] Mobile App Hien truong & Khach:    http://localhost:{port}/mobile")
    print(f"  [*] API Documentation (Swagger):       http://localhost:{port}/docs")
    print("=" * 65)
    uvicorn.run("app.main:app", host=host, port=port, reload=False)
