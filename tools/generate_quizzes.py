#!/usr/bin/env python3
"""Compatibility entry point: canonical content generator v2, not legacy 56-question output."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name('generate_content_v2.py')),run_name='__main__')
