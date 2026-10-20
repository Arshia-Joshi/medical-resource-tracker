# Starts (or restarts) one node. Usage: scripts\start-node.ps1 <id>   (id = 1, 2 or 3)
param([int]$Id)
$backend = Join-Path (Split-Path -Parent $PSScriptRoot) "backend"
java -cp "$backend\out" mrt.ServerMain $Id
