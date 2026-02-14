#!/bin/bash

################################################################################
# Deployment Diagnostic Script
#
# Usage:
#   ./diagnose.sh
#
# Purpose:
#   Provides comprehensive information about deployment status, containers,
#   and system health.
################################################################################

set -euo pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Functions
print_header() {
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"
}

print_section() {
    echo -e "\n${CYAN}► $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${MAGENTA}ℹ $1${NC}"
}

# Main diagnostics
main() {
    print_header "EspacoGeek Deployment Diagnostics"

    # 1. Docker daemon status
    print_section "Docker Daemon Status"
    if docker info > /dev/null 2>&1; then
        print_success "Docker daemon is running"
        DOCKER_VERSION=$(docker version --format '{{.Server.Version}}')
        print_info "Docker version: $DOCKER_VERSION"
    else
        print_error "Docker daemon is not running or not accessible"
        return 1
    fi

    # 2. Container status
    print_section "Container Status"

    if docker ps -a --filter name="espacogeek" | grep -q "espacogeek"; then
        RUNNING=$(docker ps --filter name="espacogeek" -q)
        if [ -n "$RUNNING" ]; then
            print_success "Container 'espacogeek' is running"
            CONTAINER_ID=$(docker inspect -f '{{.ID}}' espacogeek)
            print_info "Container ID: ${CONTAINER_ID:0:12}"

            # Container status details
            STATUS=$(docker inspect espacogeek --format='{{.State.Status}}')
            print_info "Status: $STATUS"

            UPTIME=$(docker inspect espacogeek --format='{{.State.StartedAt}}')
            print_info "Started at: $UPTIME"

            # Memory usage
            MEMORY=$(docker stats espacogeek --no-stream --format '{{.MemUsage}}')
            print_info "Memory usage: $MEMORY"
        else
            print_warn "Container 'espacogeek' exists but is stopped"
            docker ps -a --filter name="espacogeek"
        fi
    else
        print_error "Container 'espacogeek' not found"
    fi

    # 3. Backup container status
    print_section "Backup Container Status"
    if docker ps -a --filter name="espacogeek-old" | grep -q "espacogeek-old"; then
        print_warn "Backup container 'espacogeek-old' exists (rollback available)"
        docker ps -a --filter name="espacogeek-old"
    else
        print_success "No backup container found (last deployment was successful)"
    fi

    # 4. Images
    print_section "Docker Images"
    if docker images --filter reference="espacogeek*" | grep -q "espacogeek"; then
        print_success "EspacoGeek images found:"
        docker images --filter reference="espacogeek*" --format "  {{.Repository}}:{{.Tag}} ({{.Size}})"
    else
        print_warn "No local EspacoGeek images found"
    fi

    # 5. Container logs (if running)
    print_section "Container Logs (last 20 lines)"
    if docker ps --filter name="espacogeek" -q > /dev/null 2>&1; then
        docker logs --tail 20 espacogeek 2>/dev/null || print_warn "Could not retrieve logs"
    else
        print_warn "Container is not running, cannot retrieve logs"
    fi

    # 6. Application health
    print_section "Application Health Check"
    if docker ps --filter name="espacogeek" -q > /dev/null 2>&1; then
        HEALTH=$(docker exec espacogeek curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "")
        if [ -n "$HEALTH" ]; then
            print_success "Health endpoint responded:"
            echo "$HEALTH" | grep -o '"status":"[^"]*"' || echo "$HEALTH" | head -n 5
        else
            print_warn "Health endpoint did not respond or application not ready"
        fi
    else
        print_warn "Container is not running, cannot check health"
    fi

    # 7. Port status
    print_section "Port Status"
    if netstat -tuln 2>/dev/null | grep -q ":8080 "; then
        print_success "Port 8080 is in use"
    else
        print_warn "Port 8080 is not in use"
    fi

    # 8. Backup directory
    print_section "Backup Directory"
    BACKUP_DIR="${HOME}/espacogeek-backups"
    if [ -d "$BACKUP_DIR" ]; then
        BACKUP_COUNT=$(ls -1 "$BACKUP_DIR"/*.tar 2>/dev/null | wc -l)
        print_success "Backup directory found: $BACKUP_DIR"
        print_info "Number of backups: $BACKUP_COUNT"

        if [ $BACKUP_COUNT -gt 0 ]; then
            print_info "Latest 3 backups:"
            ls -lh "$BACKUP_DIR"/*.tar 2>/dev/null | tail -3 | awk '{print "  " $9 " (" $5 ")"}'
        fi
    else
        print_warn "Backup directory not found: $BACKUP_DIR"
    fi

    # 9. Disk space
    print_section "Disk Space"
    DISK_USAGE=$(df -h / | tail -1)
    print_info "Root filesystem:"
    echo "  $DISK_USAGE" | awk '{print "    Used: " $3 " / Total: " $2 " (" $5 ")"}'

    if [ -d "$BACKUP_DIR" ]; then
        BACKUP_SIZE=$(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1)
        print_info "Backup directory size: $BACKUP_SIZE"
    fi

    # 10. Network connectivity
    print_section "Network Connectivity"
    if docker ps --filter name="espacogeek" -q > /dev/null 2>&1; then
        if docker exec espacogeek ping -c 1 8.8.8.8 > /dev/null 2>&1; then
            print_success "Container has internet connectivity"
        else
            print_warn "Container may not have internet connectivity"
        fi
    else
        print_warn "Container is not running, cannot test connectivity"
    fi

    # 11. Recent git commits (if in repo)
    print_section "Git Information"
    if [ -d .git ]; then
        LAST_COMMIT=$(git log -1 --format="%h - %s (%ar)")
        BRANCH=$(git rev-parse --abbrev-ref HEAD)
        print_info "Branch: $BRANCH"
        print_info "Last commit: $LAST_COMMIT"
    else
        print_info "Not in a git repository"
    fi

    # Summary
    print_header "Diagnostics Complete"

    if docker ps --filter name="espacogeek" -q > /dev/null 2>&1; then
        print_success "System appears to be healthy"
    else
        print_error "System may have issues - container is not running"
    fi
}

# Run main function
main "$@"

