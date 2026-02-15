#!/bin/bash

################################################################################
# Improved Docker Deployment Script with Backup, Validation, and Rollback
#
# Usage:
#   ./deploy.sh <GHCR_OWNER_LC> <APP_NAME> <IMAGE_TAG> <ENV_FILE>
#
# Features:
#   - Verifies container existence before operations
#   - Creates backups of old containers and data
#   - Validates new container is running
#   - Automatic rollback on failure
################################################################################

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Log functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*"
}

# Trap errors to run cleanup
trap 'on_error' ERR EXIT

on_error() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        log_error "Deployment failed with exit code $exit_code"
        log_warn "Attempting rollback..."
        rollback_deployment
    fi
}

# Arguments
GHCR_OWNER_LC="${1:-}"
APP_NAME="${2:-}"
IMAGE_TAG="${3:-latest}"
ENV_FILE="${4:-.env.espacogeek}"
CONTAINER_NAME="espacogeek"
BACKUP_DIR="${HOME}/espacogeek-backups"
OLD_CONTAINER_BACKUP="${CONTAINER_NAME}-old"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Validate arguments
if [ -z "$GHCR_OWNER_LC" ] || [ -z "$APP_NAME" ]; then
    log_error "Usage: $0 <GHCR_OWNER_LC> <APP_NAME> [IMAGE_TAG] [ENV_FILE]"
    exit 1
fi

IMAGE="ghcr.io/${GHCR_OWNER_LC}/${APP_NAME}:${IMAGE_TAG}"

################################################################################
# Functions
################################################################################

container_exists() {
    docker ps -a --format '{{.Names}}' | grep -q "^${1}$"
}

container_running() {
    docker ps --format '{{.Names}}' | grep -q "^${1}$"
}

backup_old_container() {
    if ! container_exists "$CONTAINER_NAME"; then
        log_info "No existing container to backup"
        return 0
    fi

    log_info "Creating backup of old container..."

    # Create backup directory
    mkdir -p "$BACKUP_DIR"

    # Export old container to backup
    BACKUP_FILE="${BACKUP_DIR}/${CONTAINER_NAME}_backup_${TIMESTAMP}.tar"
    log_info "Exporting container to ${BACKUP_FILE}..."

    if docker export "$CONTAINER_NAME" > "$BACKUP_FILE"; then
        log_success "Container backup created: ${BACKUP_FILE}"
    else
        log_error "Failed to backup container"
        return 1
    fi

    # Keep only last 5 backups
    log_info "Cleaning up old backups (keeping last 5)..."
    ls -t "${BACKUP_DIR}/${CONTAINER_NAME}_backup_"*.tar 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
}

rename_old_container() {
    if ! container_exists "$CONTAINER_NAME"; then
        log_info "No old container to rename"
        return 0
    fi

    log_info "Renaming old container..."

    # Stop the old container if running
    if container_running "$CONTAINER_NAME"; then
        log_info "Stopping old container..."
        docker stop "$CONTAINER_NAME" || log_warn "Failed to stop container"
    fi

    # Rename old container for rollback purposes
    if docker rename "$CONTAINER_NAME" "${OLD_CONTAINER_BACKUP}" 2>/dev/null; then
        log_success "Old container renamed to ${OLD_CONTAINER_BACKUP}"
    else
        log_warn "Could not rename old container (may have already been removed)"
    fi
}

pull_new_image() {
    log_info "Pulling new image: ${IMAGE}..."

    if docker pull "$IMAGE"; then
        log_success "Image pulled successfully"
        return 0
    else
        log_error "Failed to pull image"
        return 1
    fi
}

start_new_container() {
    log_info "Starting new container..."

    if ! docker run -d \
        --name "$CONTAINER_NAME" \
        -p 8080:8080 \
        --restart unless-stopped \
        --env-file "$ENV_FILE" \
        "$IMAGE"; then
        log_error "Failed to start container"
        return 1
    fi

    log_success "Container started with ID: $(docker ps --filter name=$CONTAINER_NAME -q)"
}

validate_container_health() {
    local max_attempts=30
    local attempt=1
    local wait_seconds=2

    log_info "Validating container health (max ${max_attempts} attempts)..."

    while [ $attempt -le $max_attempts ]; do
        if container_running "$CONTAINER_NAME"; then
            # Check if container is still running (not crashed)
            local status=$(docker inspect "$CONTAINER_NAME" --format='{{.State.Status}}')

            if [ "$status" = "running" ]; then
                # Try to ping the health endpoint (assuming Spring Boot actuator is available)
                if docker exec "$CONTAINER_NAME" wget -q -O- http://localhost:8080/actuator/health &>/dev/null || \
                   docker exec "$CONTAINER_NAME" curl -s http://localhost:8080/actuator/health &>/dev/null; then
                    log_success "Container is healthy"
                    return 0
                elif [ $attempt -eq $max_attempts ]; then
                    # On last attempt, accept if container is just running
                    log_warn "Health endpoint not available, but container is running"
                    log_success "Container is operational"
                    return 0
                fi
            else
                log_warn "Container status is: $status"
            fi
        fi

        log_info "Waiting for container to be ready (attempt ${attempt}/${max_attempts})..."
        sleep $wait_seconds
        attempt=$((attempt + 1))
    done

    log_error "Container failed health check"
    return 1
}

cleanup_old_container() {
    log_info "Cleaning up old container..."

    if container_exists "${OLD_CONTAINER_BACKUP}"; then
        log_info "Removing backed-up container: ${OLD_CONTAINER_BACKUP}"
        if docker rm -f "${OLD_CONTAINER_BACKUP}"; then
            log_success "Old container removed"
        else
            log_warn "Could not remove old container"
        fi
    fi
}

rollback_deployment() {
    log_warn "Starting rollback procedure..."

    # Stop and remove new container
    if container_exists "$CONTAINER_NAME"; then
        log_info "Stopping new container..."
        docker stop "$CONTAINER_NAME" 2>/dev/null || true
        docker rm "$CONTAINER_NAME" 2>/dev/null || true
    fi

    # Restore old container
    if container_exists "${OLD_CONTAINER_BACKUP}"; then
        log_info "Restoring old container from backup: ${OLD_CONTAINER_BACKUP}"

        if docker rename "${OLD_CONTAINER_BACKUP}" "$CONTAINER_NAME" 2>/dev/null; then
            if docker start "$CONTAINER_NAME"; then
                log_success "Rollback successful: Old container restored"
                return 0
            else
                log_error "Failed to start rolled-back container"
                return 1
            fi
        else
            log_error "Failed to rename backed-up container"
            return 1
        fi
    else
        log_error "No backed-up container available for rollback"
        return 1
    fi
}

show_container_status() {
    log_info "=== Container Status ==="
    docker ps -a --filter name="$CONTAINER_NAME" --filter name="${OLD_CONTAINER_BACKUP}" || log_info "No containers found"
    log_info "======================="
}

show_container_logs() {
    log_info "=== Container Logs (last 20 lines) ==="
    if container_exists "$CONTAINER_NAME"; then
        docker logs --tail 20 "$CONTAINER_NAME" || log_info "No logs available"
    fi
    log_info "======================================="
}

################################################################################
# Main Deployment Flow
################################################################################

main() {
    log_info "Starting deployment of ${APP_NAME}:${IMAGE_TAG}"
    log_info "Container name: ${CONTAINER_NAME}"
    log_info "Image: ${IMAGE}"
    log_info "Environment file: ${ENV_FILE}"
    log_info ""

    # Step 1: Verify environment file exists and validate SPRING_DATASOURCE_URL
    if [ ! -f "$ENV_FILE" ]; then
        log_error "Environment file not found: ${ENV_FILE}"
        exit 1
    fi
    log_success "Environment file found"

    # Validate SPRING_DATASOURCE_URL exists and starts with 'jdbc:'
    if ! grep -q "^SPRING_DATASOURCE_URL=" "$ENV_FILE"; then
        log_error "SPRING_DATASOURCE_URL not found in environment file"
        exit 1
    fi
    DATASOURCE_URL=$(grep "^SPRING_DATASOURCE_URL=" "$ENV_FILE" | cut -d'=' -f2)
    if [ -z "$DATASOURCE_URL" ]; then
        log_error "SPRING_DATASOURCE_URL is empty"
        exit 1
    fi
    if [[ ! "$DATASOURCE_URL" =~ ^jdbc: ]]; then
        log_error "SPRING_DATASOURCE_URL must start with 'jdbc:' but got: $DATASOURCE_URL"
        exit 1
    fi
    log_success "SPRING_DATASOURCE_URL validated: $DATASOURCE_URL"

    # Step 2: Create backups
    backup_old_container || exit 1

    # Step 3: Rename old container (for rollback)
    rename_old_container || exit 1

    # Step 4: Pull new image
    pull_new_image || exit 1

    # Step 5: Start new container
    start_new_container || exit 1

    # Step 6: Validate container health
    if ! validate_container_health; then
        log_error "Health check failed, initiating rollback..."
        rollback_deployment || exit 1
        exit 1
    fi

    # Step 7: Cleanup old container
    cleanup_old_container

    # Step 8: Show final status
    log_success ""
    log_success "=== DEPLOYMENT SUCCESSFUL ==="
    show_container_status
    show_container_logs
    log_success "============================="

    # Clean up env file
    rm -f "$ENV_FILE"
}

# Execute main function
main

