#!/bin/bash

# Build and Restart Script for Task Manager Backend
# This script compiles the application and restarts the Docker container

set -e  # Exit on error

echo "🔧 Task Manager Backend - Build and Restart"
echo "==========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Step 1: Stop running container
print_info "Stopping running containers..."
docker compose down --remove-orphans || {
    print_error "Failed to stop containers"
    exit 1
}
print_success "Containers stopped"
echo ""

# Step 2: Clean and compile
print_info "Cleaning and compiling project..."
mvn clean compile -q || {
    print_error "Maven compilation failed"
    exit 1
}
print_success "Compilation successful"
echo ""

# Step 3: Run tests (optional - comment out if you want to skip)
print_info "Running tests..."
mvn test -q || {
    print_error "Tests failed"
    exit 1
}
print_success "Tests passed"
echo ""

# Step 4: Package application
print_info "Packaging application..."
mvn package -DskipTests -q || {
    print_error "Packaging failed"
    exit 1
}
print_success "Application packaged"
echo ""

# Step 5: Rebuild Docker image
print_info "Rebuilding Docker image..."
docker compose build --no-cache app || {
    print_error "Docker build failed"
    exit 1
}
print_success "Docker image built"
echo ""

# Step 6: Start containers
print_info "Starting containers..."
docker compose up -d || {
    print_error "Failed to start containers"
    exit 1
}
print_success "Containers started"
echo ""

# Step 7: Wait for application to be ready
print_info "Waiting for application to be ready..."
sleep 5

# Check if container is running
if docker compose ps | grep -q "taskmanager-app.*running"; then
    print_success "Application is running!"
    echo ""
    echo "📊 Container Status:"
    docker compose ps
    echo ""
    echo "📝 View logs with: docker compose logs -f app"
    echo "🛑 Stop with: docker compose down"
    echo "🔍 Health check: curl http://localhost:8080/actuator/health"
else
    print_error "Application failed to start"
    echo ""
    echo "View logs with: docker compose logs app"
    exit 1
fi
