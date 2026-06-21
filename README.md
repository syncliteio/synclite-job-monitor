# SyncLite Job Monitor — Unified Job Management & Scheduling

> Part of the [SyncLite Platform](https://github.com/syncliteio/SyncLite) — Build Anything, Sync Anywhere.

## What is SyncLite Job Monitor?

**SyncLite Job Monitor** is the operations hub for the SyncLite platform. It provides a unified web interface for managing, monitoring, scheduling, and controlling all SyncLite jobs running on a given host — Consolidator jobs, DBReader replication jobs, and QReader IoT connector jobs — from a single pane of glass.

```
SyncLite Consolidator jobs  ─┐
SyncLite DBReader jobs       ├──▶  SyncLite Job Monitor  (web UI)
SyncLite QReader jobs       ─┘
```

## Key Features

- **Unified dashboard** — see all running and stopped jobs across all SyncLite components in one view
- **Job lifecycle control** — start, stop, restart, and configure individual jobs from the UI
- **Scheduling** — set cron-style schedules for batch ETL and migration jobs (DBReader, QReader)
- **Health monitoring** — real-time status indicators, error counts, and throughput metrics per job
- **Alerting** — configure email or webhook notifications on job failure or lag threshold breaches
- **Audit log** — full history of job state changes
- **Multi-job support** — manage dozens of concurrent SyncLite jobs on a single host

## Quick Start

Job Monitor is deployed as part of the standard SyncLite platform release:

```bash
# From the platform bin/ directory
./deploy.sh && ./start.sh
```

Then open: http://localhost:8080/synclite-job-monitor

## UI Overview

| Page | Description |
|---|---|
| Dashboard | All jobs — status, throughput, last run |
| Job Detail | Logs, metrics, start / stop / configure controls |
| Schedules | Create and edit cron schedules for batch jobs |
| Alerts | Configure notification rules |

## Build

```bash
cd synclite-job-monitor/root
mvn -Drevision=1.0.0 clean install
```

Built WAR: `root/web/target/synclite-jobmonitor-oss.war`

## Related Components

| Component | Role |
|---|---|
| [SyncLite Consolidator](https://github.com/syncliteio/synclite-consolidator) | Real-time consolidation jobs managed here |
| [SyncLite DBReader](https://github.com/syncliteio/synclite-dbreader) | ETL/replication jobs managed here |
| [SyncLite QReader](https://github.com/syncliteio/synclite-qreader) | IoT connector jobs managed here |

## Documentation & Community

- Full documentation: https://github.com/syncliteio/SyncLite/blob/main/DOCUMENTATION.md
- Website: https://www.synclite.io
- Community: https://github.com/syncliteio/SyncLite/issues

---

← Back to the [SyncLite Platform README](https://github.com/syncliteio/SyncLite/blob/main/README.md)
