# Smart Home Ecosystem – Central VPS Backend API

![Status](https://img.shields.io/badge/Status-Completed-brightgreen)
![Component](https://img.shields.io/badge/System-Central%20Cloud%20API-blue)

The central orchestrator and cloud API for the **Smart Home Ecosystem**. Deployed on a Linux VPS, it processes telemetry incoming from Raspberry Pi edge nodes, persists historical data, handles user authentication, and provides real-time control endpoints for web and mobile clients.

---

## Architecture & Features

- **RESTful & WebSocket API:** Low-latency bi-directional communication with client applications and IoT hardware.
- **Authentication & Security:** JWT-based user authorization ensuring encrypted and secure control of physical devices.
- **Telemetry Storage:** Data persistence for historical sensor analytics and system logs.
- **Central Dispatcher:** Receives trigger commands from Android/Web clients and routes them instantly to the edge nodes.

---

## Tech Stack

- **Server Environment:** Linux VPS (Nginx / Systemd)
- **Backend Infrastructure:** Node.js / Python *(wybierz właściwe)*
- **Data Storage:** PostgreSQL / MongoDB / SQLite *(wybierz właściwe)*

---

## Author

- **Gabriel ([@CrimsonGabriel](https://github.com/CrimsonGabriel))** – Backend architecture, API design, database modeling & server deployment.

---

## Related Repositories

- [Raspberry Pi Node](https://github.com/CrimsonGabriel/RaspberryPI)
- [Web Dashboard Frontend](https://github.com/CrimsonGabriel/VPS-frontend)
- [Android App Repository](https://github.com/CrimsonGabriel/Android-SmartHome)

  ```mermaid
graph TD
    subgraph Clients["📱 & 💻 Client Layer"]
        APP["📱 Android App<br/>(Mobile Client)"]
        WEB["💻 Web Dashboard<br/>(VPS Frontend)"]
    end

    subgraph Cloud["☁️ Cloud Infrastructure"]
        VPS["⚡ Central VPS Backend<br/>(REST API / WebSockets / DB)"]
    end

    subgraph Edge["🔌 Edge & Hardware Layer"]
        RPI["🔌 Raspberry Pi<br/>(IoT Edge Node)"]
        SENSORS["🌡️ Sensors & Actuators<br/>(Relays, Temp, Motion)"]
    end

    %% Connections
    APP <-->|"REST API / WebSockets"| VPS
    WEB <-->|"REST API / WebSockets"| VPS
    VPS <-->|"Telemetry / Commands (MQTT/REST)"| RPI
    RPI <-->|"GPIO / Serial"| SENSORS

    %% Styling
    style VPS fill:#1e293b,stroke:#3b82f6,stroke-width:2px,color:#fff
    style RPI fill:#1e293b,stroke:#f97316,stroke-width:2px,color:#fff
    style APP fill:#1e293b,stroke:#a855f7,stroke-width:2px,color:#fff
    style WEB fill:#1e293b,stroke:#22c55e,stroke-width:2px,color:#fff
    style SENSORS fill:#0f172a,stroke:#64748b,stroke-width:1px,color:#94a3b8
