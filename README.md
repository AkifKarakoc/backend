# Smart Tourism Guide Backend

Bu proje, akıllı turizm rehberlik sistemi için Spring Boot tabanlı bir RESTful API backend uygulamasıdır.

## Teknolojiler

- **Backend Framework**: Spring Boot
- **Database**: PostgreSQL + PostGIS (coğrafi veriler için)
- **Cache**: Redis
- **ORM**: JPA/Hibernate
- **API Documentation**: Postman Collection (TourGuide_API.postman_collection.json)
- **Database Migration**: Liquibase

## Proje Yapısı

```
src/main/java/com/tourguide/
├── admin/              # Admin işlemleri
├── auth/               # Kimlik doğrulama (JWT)
├── badge/              # Rozet sistemi
├── chat/               # Chat işlemleri
├── common/             # Ortak yapılandırma ve utilities
├── image/              # Resim yönetimi
├── notification/       # Bildirim sistemi
├── place/              # Yerler
├── quest/              # Görevler
├── review/             # Yorumlar
├── route/              # Rotalar
├── todo/               # Yapılacaklar
└── user/               # Kullanıcı yönetimi
```

## Başlamak

### Gereksinimler
- Java 11+
- Maven
- PostgreSQL
- Redis
- Docker (opsiyonel)

### Kurulum

1. Bağımlılıkları yükleyin:
```bash
mvn install
```

2. Docker ile servisleri başlatın:
```bash
docker-compose up -d
```

3. Uygulamayı çalıştırın:
```bash
mvn spring-boot:run
```

## API Kaynakları

API endpoints için `TourGuide_API.postman_collection.json` dosyasını Postman'e import edin.

## Grafana ile Log Takibi

Uygulama logları `logs/tourguide-backend.log` dosyasına yazılır. Docker compose içindeki yeni servislerle akış şu şekildedir:

- Spring Boot log dosyasını yazar.
- Promtail bu dosyayı okuyup Loki'ye gönderir.
- Grafana Loki datasource'u üzerinden logları sorgular.

### Servisleri başlatma

```bash
docker-compose up -d
mvn spring-boot:run
```

### Erişim bilgileri

- Grafana: `http://localhost:3000`
- Kullanıcı adı: `admin`
- Şifre: `admin123`
- Loki: `http://localhost:3100`

Grafana açıldıktan sonra `Explore` ekranında şu LogQL sorgusu ile backend logları görülebilir:

```logql
{application="tourguide-backend"}
```

Sadece hata logları için:

```logql
{application="tourguide-backend", level="ERROR"}
```

## Lisans

Bu proje Dokuz Eylül Üniversitesi Bilgisayar Mühendisliği Bölümü Bitirme Tezi kapsamındadır.
