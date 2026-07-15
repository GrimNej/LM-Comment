# Oracle VPS relay deployment

This target keeps LM-Comment independent from existing applications:

- app-owned Node 22.13.1 runtime under `/opt/lm-comment/runtime`;
- immutable releases under `/opt/lm-comment/releases`;
- root-only secrets in `/etc/lm-comment/relay.env`;
- a sandboxed DynamicUser systemd service;
- a 192 MiB hard memory limit and 50% CPU quota;
- binding only to the existing Docker bridge gateway at `172.18.0.1:8787`;
- HTTPS termination through the existing Caddy container.

The service stores no screenshots, requests, responses, database, or files.
Only system metadata logs are written to journald.

The server-specific hostname is `https://lmcomment-api.grimnej.com`.
Before changing Caddy, validate a candidate configuration. Preserve the
original file, install the candidate atomically, and use `caddy reload`; never
restart the existing application containers for an LM-Comment deployment.
