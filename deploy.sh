#!/bin/bash
# ⚠️ BU DOSYA ŞUAN KULLANILMAYACAK - İLERIDE DROPLET'E TAŞININCA KULLANILACAK
# Production deployment + SSL sertifika otomasyon scripti.
# Yerel geliştirme sırasında bu scripti çalıştırmayın.

set -e

echo "========================================="
echo "  TourGuide Backend Deployment Script"
echo "========================================="

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

DOMAIN_NAME=${DOMAIN_NAME:-"your-domain.com"}
EMAIL=${LETSENCRYPT_EMAIL:-"your-email@example.com"}

echo ""
echo "[1/5] Pulling latest code..."
git pull || echo "Not a git repo, skipping pull"

echo ""
echo "[2/5] Creating certbot directories..."
mkdir -p certbot/conf certbot/www

echo ""
echo "[3/5] Building and starting services..."
docker compose -f docker-compose.prod.yml down --remove-orphans
docker compose -f docker-compose.prod.yml build --no-cache
docker compose -f docker-compose.prod.yml up -d

echo ""
echo "[4/5] Waiting for services to start..."
sleep 10

echo ""
echo "[5/5] Obtaining SSL certificate..."

# Check if certificate already exists
if [ -d "certbot/conf/live/$DOMAIN_NAME" ]; then
    echo "Certificate already exists, renewing..."
    docker compose -f docker-compose.prod.yml run --rm certbot certbot renew
else
    echo "Obtaining new certificate for $DOMAIN_NAME..."
    
    # Temporary nginx config for HTTP challenge
    cat > nginx-temp.conf <<EOF
server {
    listen 80;
    server_name $DOMAIN_NAME;
    
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    
    location / {
        return 301 https://\$host\$request_uri;
    }
}
EOF
    
    # Restart nginx with temp config
    docker compose -f docker-compose.prod.yml stop nginx
    docker run --rm -d --name temp-nginx \
        -v "$(pwd)/nginx-temp.conf:/etc/nginx/conf.d/default.conf:ro" \
        -v "$(pwd)/certbot/www:/var/www/certbot" \
        -p 80:80 \
        nginx:alpine
    
    sleep 5
    
    # Obtain certificate
    docker run --rm \
        -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
        -v "$(pwd)/certbot/www:/var/www/certbot" \
        certbot/certbot certonly \
        --webroot \
        --webroot-path=/var/www/certbot \
        --email "$EMAIL" \
        --agree-tos \
        --no-eff-email \
        -d "$DOMAIN_NAME"
    
    # Stop temporary nginx
    docker stop temp-nginx || true
    
    # Remove temp config
    rm -f nginx-temp.conf
    
    # Update nginx.conf with actual domain
    sed -i "s/server_name _;/server_name $DOMAIN_NAME;/g" nginx.conf
    sed -i "s|\${DOMAIN_NAME}|$DOMAIN_NAME|g" nginx.conf
    
    # Restart nginx with proper config
    docker compose -f docker-compose.prod.yml up -d nginx
fi

echo ""
echo "========================================="
echo "  Deployment Complete!"
echo "========================================="
echo ""
echo "Services running:"
docker compose -f docker-compose.prod.yml ps
echo ""
echo "App URL: https://$DOMAIN_NAME"
echo "MinIO Console: http://$DOMAIN_NAME:9001"
echo ""
echo "To view logs: docker compose -f docker-compose.prod.yml logs -f"
echo "To stop: docker compose -f docker-compose.prod.yml down"
