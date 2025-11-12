#!/bin/bash

# Quick Rebuild Script (Skips Tests, Uses Caching)
# Use this for faster development iterations
# Maven dependencies are cached in ~/.m2/repository
# Docker layers are cached for faster rebuilds

set -e

echo "⚡ Quick Rebuild (Cached, Skips Tests)"
echo "========================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

# Stop only app container (keep database running)
print_step "Stopping app container..."
docker compose stop app
print_success "App stopped"
echo ""

# Package without cleaning (uses Maven cache)
print_step "Packaging with Maven cache..."
print_info "Using cached dependencies from ~/.m2/repository"
mvn package -DskipTests -q
print_success "Package complete"
echo ""

# Rebuild Docker image WITH layer caching
print_step "Rebuilding Docker image with cache..."
print_info "Using Docker layer cache for faster builds"
docker compose build app
print_success "Image rebuilt"
echo ""

# Start app container
print_step "Starting app container..."
docker compose up -d app
print_success "App started"

sleep 5
echo ""
print_success "✨ Quick rebuild complete!"
echo ""
echo "📊 Status:"
docker compose ps
echo ""
echo "💡 Speed Tips:"
echo "   • Maven deps cached in ~/.m2/repository"
echo "   • Docker layers reused when possible"
echo "   • Database kept running for faster restart"
echo ""
echo "🔍 Health check: curl http://localhost:8080/api/public/health"
echo "📝 View logs: docker compose logs -f app"
