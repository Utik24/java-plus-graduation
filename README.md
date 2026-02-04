# Explore With Me

## Архитектура

Проект построен на микросервисной архитектуре с инфраструктурным слоем для обнаружения, конфигурации и маршрутизации.

**Инфраструктурные сервисы (infra):**
- **discovery-server** — сервис регистрации и обнаружения (Eureka).
- **config-server** — централизованная конфигурация сервисов (Spring Cloud Config, профиль `native`).
- **gateway-server** — API-шлюз (Spring Cloud Gateway) с маршрутами к доменным сервисам.

**Доменные сервисы (core):**
- **user-service** — управление пользователями.
- **event-service** — управление событиями, категориями, подборками, административными операциями.
- **request-service** — заявки на участие.
- **extra-service** — дополнительные функции (например, комментарии).

**Сервис статистики (stats):**
- **stats-server** — хранение и выдача статистики просмотров/хитов.
- **stats-client** — библиотека клиента для обращения к `stats-server`.

**Взаимодействие сервисов:**
- Внешние запросы поступают в **gateway-server** и маршрутизируются на доменные сервисы через `lb://` (балансировка по сервисам, зарегистрированным в discovery).
- Доменные сервисы используют **stats-client** для записи хитов и получения статистики из **stats-server**.
- Все сервисы регистрируются в **discovery-server** и получают конфигурацию из **config-server**.

## Конфигурация

Централизованные настройки сервисов хранятся в `infra/config-server/src/main/resources/config`:
- `event-service.yml`, `user-service.yml`, `request-service.yml`, `extra-service.yml` — параметры доменных сервисов.
- `stats-server.yml` — параметры сервиса статистики.
- `gateway-server.yml` — маршрутизация внешнего API.

Системные настройки инфраструктуры расположены в:
- `infra/discovery-server/src/main/resources/application.yml`
- `infra/config-server/src/main/resources/application.yml`
- `infra/gateway-server/src/main/resources/application.yml`

## Внутренний API (межсервисное взаимодействие)

1. **Статистика (stats-server):**
    - `POST /hit` — запись события просмотра.
    - `GET /stats?start={start}&end={end}&uris={uris}&unique={unique}` — получение агрегированной статистики.

2. **Маршрутизация на шлюзе (gateway-server):**
    - `/admin/users/**` → `user-service`
    - `/users/*/requests/**` → `request-service`
    - `/events/**`, `/categories/**`, `/compilations/**`, `/admin/**`, `/users/**` → `event-service`
    - `/event/**`, `/comments/**` → `extra-service`

## Внешний API

Спецификация внешнего API:
- **Основной API:** [ewm-main-service-spec.json](./ewm-main-service-spec.json)
- **API статистики:** [ewm-stats-service-spec.json](./ewm-stats-service-spec.json)