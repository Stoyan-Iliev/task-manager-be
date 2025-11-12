#!/bin/bash

# Script to start the application in debug mode
# This script starts both postgres and the app with debugging enabled

echo "🐛 Starting Task Manager in debug mode..."
echo ""

# Stop any running containers
echo "📦 Stopping existing containers..."
docker compose down

# Start with debug configuration
echo "🚀 Starting containers with debug configuration..."
docker compose -f docker-compose.yml -f docker-compose.debug.yml up -d

# Wait for containers to start
echo ""
echo "⏳ Waiting for containers to be healthy..."
sleep 5

# Check if containers are running
if docker ps | grep -q taskmanager-app; then
    echo ""
    echo "✅ Application started successfully!"
    echo ""
    echo "📋 Service Information:"
    echo "   Application: http://localhost:8080"
    echo "   Debug Port:  localhost:5005"
    echo "   PostgreSQL:  localhost:5432"
    echo ""
    echo "🔍 To attach debugger:"
    echo "   1. Open IntelliJ IDEA"
    echo "   2. Select 'Debug Docker Container' from the run configurations dropdown"
    echo "   3. Click the Debug button (or press Shift+F9)"
    echo ""
    echo "📊 View logs:"
    echo "   docker compose logs -f app"
    echo ""
    echo "🛑 Stop containers:"
    echo "   docker compose down"
else
    echo ""
    echo "❌ Failed to start application. Check logs:"
    echo "   docker compose logs app"
    exit 1
fi
