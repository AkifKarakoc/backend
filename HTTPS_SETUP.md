# ⚠️ BU DOSYA ŞUAN KULLANILMAYACAK - İLERIDE DROPLET'E TAŞININCA KULLANILACAK

# HTTPS Setup Guide - TourGuide Backend

## Prerequisites

1. **DigitalOcean Droplet** with Docker and Docker Compose installed
2. **Domain name** pointing to your droplet's IP address (A record)
3. **`.env` file** configured with production values

## Quick Setup

### 1. Configure Environment Variables

Edit `.env` file on your droplet:

```env
# Domain Configuration
DOMAIN_NAME=your-domain.com
LETSENCRYPT_EMAIL=your-email@example.com

# Database
POSTGRES_DB=tourguide
POSTGRES_USER=tourguide
POSTGRES_PASSWORD=your-secure-password

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your-secure-minio-password

# JWT (add your production JWT secret)
JWT_SECRET=your-very-long-and-secure-jwt-secret-key
```

### 2. Point Domain to Droplet

In your domain registrar's DNS settings, create an A record:

```
Type: A
Name: @
Value: YOUR_DROPLET_IP
TTL: 3600
```

Wait for DNS propagation (usually 5-30 minutes).

### 3. Run Deployment Script

```bash
# SSH into your droplet
ssh root@YOUR_DROPLET_IP

# Navigate to backend directory
cd /path/to/backend

# Make script executable
chmod +x deploy.sh

# Run deployment
./deploy.sh
```

The script will:
- Pull latest code (if git repo)
- Create necessary directories
- Build Docker images
- Start all services
- Obtain Let's Encrypt SSL certificate
- Configure nginx with HTTPS

## Manual Setup (Alternative)

If you prefer manual setup:

### 1. Start Infrastructure Services

```bash
docker compose -f docker-compose.prod.yml up -d postgres redis minio
```

### 2. Build and Start App

```bash
docker compose -f docker-compose.prod.yml up -d app
```

### 3. Obtain SSL Certificate

```bash
mkdir -p certbot/conf certbot/www

# Get certificate
docker run --rm -it \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  certbot/certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d your-domain.com
```

### 4. Update nginx.conf

Replace placeholders in `nginx.conf`:
- Change `server_name _;` to `server_name your-domain.com;`
- Change `${DOMAIN_NAME}` to `your-domain.com` in certificate paths

### 5. Start Nginx

```bash
docker compose -f docker-compose.prod.yml up -d nginx
```

## Certificate Auto-Renewal

Let's Encrypt certificates expire after 90 days. The `certbot` service in docker-compose.prod.yml automatically renews certificates every 12 hours.

To manually renew:

```bash
docker compose -f docker-compose.prod.yml run --rm certbot certbot renew
docker compose -f docker-compose.prod.yml restart nginx
```

## Useful Commands

```bash
# View all running services
docker compose -f docker-compose.prod.yml ps

# View logs
docker compose -f docker-compose.prod.yml logs -f
docker compose -f docker-compose.prod.yml logs -f app
docker compose -f docker-compose.prod.yml logs -f nginx

# Stop all services
docker compose -f docker-compose.prod.yml down

# Rebuild and restart
docker compose -f docker-compose.prod.yml up -d --build

# Check certificate expiration
docker run --rm -v "$(pwd)/certbot/conf:/etc/letsencrypt" certbot/certbot certificates
```

## Troubleshooting

### Certificate issuance fails
- Ensure domain DNS is pointing to droplet IP
- Check port 80 is accessible (not blocked by firewall)
- Verify no other service is using port 80

```bash
# Check DNS
dig your-domain.com

# Check firewall
ufw status
ufw allow 80/tcp
ufw allow 443/tcp

# Check port usage
netstat -tulpn | grep :80
```

### Nginx won't start
- Check nginx configuration syntax
- Verify certificate files exist

```bash
# Test nginx config
docker exec tourguide-nginx nginx -t

# Check nginx logs
docker logs tourguide-nginx
```

### App not reachable through nginx
- Verify app is running on port 8080
- Check network connectivity between containers

```bash
# Check app health
curl http://localhost:8080/actuator/health

# Check from nginx container
docker exec tourguide-nginx curl -s http://app:8080/actuator/health
```

## Security Checklist

- [x] HTTPS enabled with Let's Encrypt
- [x] HTTP to HTTPS redirect
- [x] HSTS header enabled
- [x] X-Frame-Options set to DENY
- [x] X-Content-Type-Options set to nosniff
- [x] Rate limiting configured
- [x] Client body size limited (50MB)
- [ ] Change default passwords in `.env`
- [ ] Set strong JWT_SECRET
- [ ] Configure firewall (ufw)
- [ ] Set up automated backups
