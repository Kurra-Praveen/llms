#!/bin/bash

# Loan Platform - Docker Helper Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

case "$1" in
    start)
        print_status "Starting PostgreSQL and Redis containers..."
        docker compose up -d postgres redis
        print_status "Waiting for services to be healthy..."
        sleep 5
        docker compose ps
        print_status "Services started! Run 'mvn spring-boot:run' to start the application."
        ;;

    start-all)
        print_status "Starting all services including management tools..."
        docker compose --profile tools up -d
        print_status "Services started!"
        echo ""
        echo "Available services:"
        echo "  - PostgreSQL: localhost:5432"
        echo "  - Redis: localhost:6379"
        echo "  - pgAdmin: http://localhost:5050 (admin@loanplatform.com / admin)"
        echo "  - Redis Commander: http://localhost:8081"
        ;;

    stop)
        print_status "Stopping all containers..."
        docker compose --profile tools down
        print_status "All containers stopped."
        ;;

    restart)
        print_status "Restarting containers..."
        docker compose --profile tools down
        docker compose up -d postgres redis
        print_status "Containers restarted."
        ;;

    logs)
        docker compose logs -f "${2:-}"
        ;;

    clean)
        print_warning "This will remove all containers and volumes!"
        read -p "Are you sure? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker compose --profile tools down -v
            print_status "Containers and volumes removed."
        fi
        ;;

    status)
        docker compose ps
        ;;

    psql)
        print_status "Connecting to PostgreSQL..."
        docker compose exec postgres psql -U postgres -d loan_platform
        ;;

    redis-cli)
        print_status "Connecting to Redis..."
        docker compose exec redis redis-cli
        ;;

    build-app)
        print_status "Building application Docker image..."
        docker build -t loan-platform:latest .
        print_status "Image built: loan-platform:latest"
        ;;

    *)
        echo "Loan Platform Docker Helper"
        echo ""
        echo "Usage: $0 {command}"
        echo ""
        echo "Commands:"
        echo "  start       - Start PostgreSQL and Redis only"
        echo "  start-all   - Start all services including pgAdmin and Redis Commander"
        echo "  stop        - Stop all containers"
        echo "  restart     - Restart all containers"
        echo "  logs [svc]  - View logs (optionally for specific service)"
        echo "  clean       - Remove all containers and volumes"
        echo "  status      - Show container status"
        echo "  psql        - Connect to PostgreSQL CLI"
        echo "  redis-cli   - Connect to Redis CLI"
        echo "  build-app   - Build application Docker image"
        echo ""
        exit 1
        ;;
esac
