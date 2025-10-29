console.log("--- START WERSJI Z GOOGLE AUTH --- JESTEM NOWYM PLIKIEM ---");

// --- Ustawienie Serwera Express (Node.js) ---
const express = require('express');
const bodyParser = require('body-parser');
const http = require('http');
const https = require('https');
const { DateTime } = require('luxon');

// [NOWY KOD] Importowanie biblioteki Google Auth
const { OAuth2Client } = require('google-auth-library');

const app = express();
const PORT = 3000;
const PASSWORD = "ZMIEN_TO_HASLO_XD"; // Hasło dla RPi

// [NOWY KOD] Konfiguracja Google Auth
// WAŻNE: Wklej tutaj Client ID (Web application) z Google Cloud Console
const GOOGLE_CLIENT_ID = "79063316759-iva8uesd0vlj3in6eaeralk2kdkgv5or.apps.googleusercontent.com"; // STARY WEB ID
const client = new OAuth2Client(GOOGLE_CLIENT_ID);

// [NOWY KOD] Lista autoryzowanych użytkowników - startuje pusta!
// Serwer będzie dopisywał nowe e-maile po udanym logowaniu Google
let AUTHORIZED_USERS = [
    // Pusta na starcie
];

// DANE GLOBALNE: Przechowywanie stanów
let registeredRPiIp = 'Brak IP RPi';
let registeredAndroidIp = 'Brak IP Androida';
let lastReportText = 'Brak ostatniego meldunku czasu';
let lastSensorData = { sensors: [] };

// Ustawienia middleware
app.use(bodyParser.json());

// --- ENDPOINT STATUSU (JSON) ---
app.get('/status/json', (req, res) => {
    res.json({
        lastReportText: lastReportText,
        registeredRPiIp: registeredRPiIp,
        registeredAndroidIp: registeredAndroidIp,
        // [ZMIANA] Dodajemy listę autoryzowanych użytkowników
        authorizedUsers: AUTHORIZED_USERS
    });
});

// --- ENDPOINT STRONY GŁÓWNEJ (HTML ze statusem) ---
app.get('/', (req, res) => {
    // Prosta strona HTML do wyświetlenia stanu
    const htmlContent = `
        <!DOCTYPE html>
        <html lang="pl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Status Bazunia Serwer</title>
            <style>
                body { font-family: sans-serif; padding: 20px; background: #f4f4f4; color: #333; }
                .status-box { background: white; padding: 15px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); margin-bottom: 10px; }
                .alert { color: red; font-weight: bold; }
                .ok { color: green; }
                ul { list-style: none; padding: 0; }
                li { background: #eee; margin: 5px 0; padding: 8px; border-radius: 3px; }
            </style>
        </head>
        <body>
            <h1>Status Serwera Bazunia (v2 z Google Auth)</h1>
            <div class="status-box">
                <p>Ostatni meldunek (czas): <strong id="lastReport">Ładowanie...</strong></p>
            </div>
            <div class="status-box">
                <p>Zarejestrowane RPi IP: <span id="rpiIp">Ładowanie...</span></p>
                <p>Zarejestrowane Android IP: <span id="androidIp">Ładowanie...</span></p>
                <small>Odświeżanie danych co 5 sekund (AJAX).</small>
            </div>
            
            <!-- [NOWA SEKCJA] Lista autoryzowanych użytkowników -->
            <div class="status-box">
                <h2>Autoryzowani Użytkownicy:</h2>
                <div id="authorizedUsersList"><p>Ładowanie...</p></div>
            </div>

            <script>
                /**
                 * Funkcja pobierająca status z serwera i aktualizująca DOM.
                 */
                function updateStatus() {
                    fetch('/status/json')
                        .then(response => response.json())
                        .then(data => {
                            // 1. Ostatni meldunek
                            document.getElementById('lastReport').textContent = data.lastReportText;

                            // 2. IP RPi
                            const rpiSpan = document.getElementById('rpiIp');
                            rpiSpan.textContent = data.registeredRPiIp;
                            rpiSpan.className = data.registeredRPiIp.startsWith('Brak') ? 'alert' : 'ok';
                            
                            // 3. IP Androida
                            const androidSpan = document.getElementById('androidIp');
                            androidSpan.textContent = data.registeredAndroidIp;
                            androidSpan.className = data.registeredAndroidIp.startsWith('Brak') ? 'alert' : 'ok';

                            // [NOWY KOD] 4. Lista autoryzowanych użytkowników
                            const usersListDiv = document.getElementById('authorizedUsersList');
                            usersListDiv.innerHTML = ''; // Wyczyść starą listę
                            if (data.authorizedUsers && data.authorizedUsers.length > 0) {
                                const ul = document.createElement('ul');
                                data.authorizedUsers.forEach(email => {
                                    const li = document.createElement('li');
                                    li.textContent = email;
                                    ul.appendChild(li);
                                });
                                usersListDiv.appendChild(ul);
                            } else {
                                usersListDiv.innerHTML = '<p>Brak autoryzowanych użytkowników.</p>';
                            }

                            console.log('Status zaktualizowany.');
                        })
                        .catch(error => {
                            console.error('Błąd pobierania statusu:', error);
                            document.getElementById('lastReport').textContent = 'Błąd połączenia z serwerem!';
                            document.getElementById('authorizedUsersList').innerHTML = '<p>Błąd połączenia!</p>';
                        });
                }

                // Uruchomienie przy starcie
                updateStatus();

                // Odświeżanie co 5 sekund (5000 ms)
                setInterval(updateStatus, 5000);
            </script>
        </body>
        </html>
    `;
    res.send(htmlContent);
});


// --- ENDPOINT REJESTRACJI ANDROIDA ---
app.post('/register/android', (req, res) => {
    const { password, port } = req.body;
    if (password !== PASSWORD || !port) {
        return res.status(401).json({ error: 'Nieprawidlowe haslo lub brak portu.' });
    }
    const publicIp = req.header('X-Forwarded-For') || req.ip;
    const clientIp = publicIp.split(',')[0].trim();
    registeredAndroidIp = `${clientIp}:${port}`;
    console.log(`[Rejestracja] Zarejestrowano publiczny IP Androida: ${registeredAndroidIp}`);
    res.json({ success: true, message: 'IP zarejestrowane pomyslnie.' });
});

// --- ENDPOINT REJESTRACJI RPi ---
app.post('/register/rasp', (req, res) => {
    const { password, port } = req.body;
    if (password !== PASSWORD || !port) {
        return res.status(401).json({ error: 'Nieprawidlowe haslo lub brak portu.' });
    }
    const publicIp = req.header('X-Forwarded-For') || req.ip;
    const clientIp = publicIp.split(',')[0].trim();
    registeredRPiIp = `${clientIp}:${port}`;
    console.log(`[Rejestracja] Zarejestrowano publiczny IP RPi: ${registeredRPiIp}`);
    res.json({ success: true, message: 'IP zarejestrowane pomyslnie.' });
});

// --- ENDPOINT MELDUNKU (RPi) ---
app.post('/update', (req, res) => {
    const { password, message } = req.body; // Pamiętaj, RPi musi wysyłać 'message'
    if (password !== PASSWORD) {
        return res.status(401).json({ error: 'Nieprawidlowe haslo.' });
    }
    const now = DateTime.now().setZone('Europe/Warsaw').toFormat('yyyy-MM-dd HH:mm:ss');
    const senderIp = req.header('X-Forwarded-For') || req.ip;
    const sender = registeredRPiIp.includes(senderIp.split(',')[0].trim()) ? 'RPi' : 'Inne';
    if (message && typeof message === 'string') {
        lastReportText = `${now} (Nadawca: ${sender}): ${message}`;
        console.log(`[MELDUNEK] Otrzymano meldunek: ${message}`);
    } else {
        lastReportText = `${now} (Nadawca: ${sender})`;
        console.log(`[MELDUNEK] Otrzymano meldunek czasu.`);
    }
    res.json({ success: true, message: 'Meldunek odebrany pomyslnie.' });
});

// --- ENDPOINT DANYCH (RPi -> VPS) ---
app.post('/data', (req, res) => {
    const { password, sensors } = req.body;
    if (password !== PASSWORD || !sensors || !Array.isArray(sensors)) {
        return res.status(401).json({ error: 'Nieprawidlowe haslo lub brak danych czujnikow.' });
    }
    lastSensorData = { sensors: sensors };
    console.log(`[DANE CZUJNIKOW] Otrzymano ${sensors.length} wpisow.`);
    res.json({ success: true, message: 'Dane czujnikow zarejestrowane.' });
});

// --- ENDPOINT DANYCH (VPS -> Android) ---
app.get('/data/android', (req, res) => {
    // [ZMIANA] Teraz sprawdzamy token Google zamiast hasła
    const authHeader = req.header('Authorization'); // Oczekujemy nagłówka "Authorization: Bearer <ID_TOKEN>"
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        console.warn("[DANE ANDROID] Odrzucono: Brak tokena Bearer.");
        return res.status(401).json({ error: 'Brak tokena autoryzacyjnego.' });
    }
    
    const idToken = authHeader.split(' ')[1];

    // Weryfikujemy token Google (bardzo podobnie jak w /auth/google)
    verifyGoogleToken(idToken)
        .then(payload => {
            // Token poprawny, użytkownik zalogowany przez Google - wysyłamy dane
            console.log(`[DANE ANDROID] Wyslano dane dla ${payload.email} (Polling).`);
            res.json(lastSensorData);
        })
        .catch(error => {
            // Token niepoprawny lub użytkownik nieautoryzowany
            console.warn(`[DANE ANDROID] Odrzucono: ${error.message}`);
            res.status(403).json({ error: `Odmowa dostępu: ${error.message}` });
        });
});


// [NOWY KOD]
// --- FUNKCJA POMOCNICZA DO WERYFIKACJI TOKENA ---
// Wydzielona logika weryfikacji, żeby jej nie powtarzać
async function verifyGoogleToken(token) {
    try {
        const ticket = await client.verifyIdToken({
            idToken: token,
            audience: GOOGLE_CLIENT_ID,
        });
        const payload = ticket.getPayload();
        
        // Sprawdzamy, czy e-mail istnieje (na wszelki wypadek)
        if (!payload || !payload.email) {
            throw new Error('Nieprawidłowy payload tokena.');
        }

        // Zwracamy payload, jeśli wszystko jest OK
        return payload; 
        
    } catch (error) {
        // Rzucamy błąd dalej, żeby można go było złapać w catch endpointu
        throw new Error(`Nieprawidłowy token Google: ${error.message}`);
    }
}


// [NOWY KOD]
// --- ENDPOINT WERYFIKACJI LOGOWANIA GOOGLE (POST /auth/google) ---
app.post('/auth/google', async (req, res) => {
    const { token } = req.body;
    
    if (!token) {
        return res.status(400).json({ error: 'Brak tokena.' });
    }

    try {
        // Używamy nowej funkcji pomocniczej do weryfikacji
        const payload = await verifyGoogleToken(token);
        const userEmail = payload.email;
        console.log(`[LOGOWANIE] Próba logowania przez: ${userEmail}`);

        // [ZMIANA] Dopisywanie do listy, jeśli go nie ma
        if (!AUTHORIZED_USERS.includes(userEmail)) {
            AUTHORIZED_USERS.push(userEmail);
            console.log(`[LOGOWANIE] Dodano nowego użytkownika do listy: ${userEmail}`);
        }
        
        // Zawsze zwracamy sukces, jeśli token był poprawny
        console.log(`[LOGOWANIE] SUKCES: Użytkownik ${userEmail} jest autoryzowany (lub został dodany).`);
        res.json({ 
            success: true, 
            message: `Witaj, ${payload.name || userEmail}!`,
            email: userEmail
        });
        
    } catch (error) {
        // Błąd z verifyGoogleToken
        console.error("[LOGOWANIE] BŁĄD:", error.message);
        res.status(401).json({ error: error.message }); // Zwracamy bardziej szczegółowy błąd
    }
});
// [KONIEC NOWEGO KODU]


// --- URUCHOMIENIE SERWERA ---
app.listen(PORT, () => {
    console.log(`Serwer Bazunia działa na porcie ${PORT}`);
});

