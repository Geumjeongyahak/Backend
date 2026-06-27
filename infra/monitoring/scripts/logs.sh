#!/usr/bin/env bash
set -euo pipefail

SERVICE="${1:-grafana}"

journalctl --user -u "$SERVICE" -n 120 --no-pager
