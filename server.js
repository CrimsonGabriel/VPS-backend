console.log("--- START WERSJI Z GOOGLE AUTH + OPCJONALNYM 2FA ---");

// --- Ustawienie Serwera Express (Node.js) ---
const express = require('express');
const bodyParser = require('body-parser');
const http = require('http');
const https = require('https');
const { DateTime } = require('luxon');
const { OAuth2Client } = require('google-auth-library');
// [NOWY KOD 2FA] Import biblioteki 2FA
const speakeasy = require('speakeasy');

const app = express();
const PORT = 3000;
const PASSWORD = "ZMIEN_TO_HASLO_XD"; // Hasło dla RPi

// Konfiguracja Google Auth
const GOOGLE_CLIENT_ID = "79063316759-iva8uesd0vlj3in6eaeralk2kdkgv5or.apps.googleusercontent.com"; // STARY WEB ID
const client = new OAuth2Client(GOOGLE_CLIENT_ID);

// Lista autoryzowanych użytkowników - startuje pusta!
let AUTHORIZED_USERS = [
    // Pusta na starcie
];

// [NOWY KOD 2FA] Przechowywanie sekretów 2FA (w pamięci - resetuje się!)
// Struktura: { "email@example.com": { secret: "...", verified: true/false, enabled: true/false, last_2fa_verified_at: 1678886400000 } }
let userSecrets2FA = {};

// DANE GLOBALNE: Przechowywanie stanów
let registeredRPiIp = 'Brak IP RPi';
let lastReportText = 'Brak ostatniego meldunku czasu';
let lastSensorData = { sensors: [] };

// ⭐️ NOWY STAN: Lista unikalnych IP Androida ⭐️
let uniqueAndroidIPs = new Set();

// Ustawienia middleware
app.use(bodyParser.json());

// --- ENDPOINT STATUSU (JSON) ---
app.get('/status/json', (req, res) => {
    // [ZMIANA 2FA] Dodajemy informację, czy 2FA jest skonfigurowane
    const users2FAStatus = {};
    for (const email in userSecrets2FA) {
        if (userSecrets2FA.hasOwnProperty(email) && userSecrets2FA[email]) {
             users2FAStatus[email] = {
                 enabled: userSecrets2FA[email].enabled || false
             };
        }
    }

    // === POPRAWKA TUTAJ ===
    // Pobieramy listę e-maili z obiektu userSecrets2FA,
    const activeUserEmails = Object.keys(userSecrets2FA);
    // =============================

    res.json({
        lastReportText: lastReportText,
        registeredRPiIp: registeredRPiIp,
        // ⭐️ Zwracamy listę unikalnych IP ⭐️
        registeredAndroidIps: Array.from(uniqueAndroidIPs),
        authorizedUsers: activeUserEmails,
        users2FAStatus: users2FAStatus 
    });
});

// --- ENDPOINT STRONY GŁÓWNEJ (HTML ze statusem) ---
app.get('/', (req, res) => {
    // [ZMIANA 2FA] Dodano wyświetlanie statusu 2FA na stronie
   // W app.get('/', (req, res) => { ...

// ... (początek kodu)

// W app.get('/', (req, res) => { ...

// ... (początek kodu)

const htmlContent = `
        <!DOCTYPE html>
        <html lang="pl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Status Bazunia Serwer</title>
            <style>
                /* ⭐️ NOWY CIEMNY MOTYW ⭐️ */
                body { 
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif;
                    padding: 20px; 
                    background: #1a1a1a; /* Bardzo ciemne tło */
                    color: #ffffff; /* Jasny tekst */
                }
                h1 { color: #f4f4f4; border-bottom: 1px solid #333; padding-bottom: 10px; }
                h2 { color: #aaaaaa; margin-top: 20px; font-weight: 500; font-size: 1.2em; }
                
                .status-box { 
                    background: #2a2a2a; /* Ciemne tło sekcji */
                    padding: 15px; 
                    border-radius: 8px; 
                    box-shadow: 0 4px 6px rgba(0,0,0,0.2); 
                    margin-bottom: 20px; 
                }
                .alert { color: #ff6b6b; font-weight: bold; } /* Czerwony alert */
                .ok { color: #48cfad; font-weight: bold; } /* Zielony sukces */
                
                ul { 
                    list-style: none; 
                    padding: 0; 
                    margin-top: 10px;
                }
                li { 
                    background: #3a3a3a; /* Tło dla elementów listy */
                    margin: 5px 0; 
                    padding: 10px; 
                    border-radius: 5px; 
                    border-left: 3px solid #666;
                }
                .user-2fa-enabled { color: #48cfad; font-weight: bold; }
                .user-2fa-disabled { color: #ffc66d; } /* Zmieniono na pomarańczowo-żółty */
            </style>
        </head>
        <body>
            <h1>Status Serwera Bazunia (v2 z Google Auth + Opcjonalnym 2FA)</h1>
            <div class="status-box">
                <p>Ostatni meldunek (czas): <strong id="lastReport">Ładowanie...</strong></p>
            </div>
            <div class="status-box">
                <p>Zarejestrowane RPi IP: <span id="rpiIp">Ładowanie...</span></p>
                <h2>Unikalne IP Androidów:</h2>
                <div id="androidIpsList"><p>Ładowanie...</p></div>
            </div>
            <div class="status-box">
                <h2>Autoryzowani Użytkownicy:</h2>
                <div id="authorizedUsersList"><p>Ładowanie...</p></div>
            </div>
            <script>
                function updateStatus() {
                    fetch('/status/json')
                        .then(response => response.json())
                        .then(data => {
                            document.getElementById('lastReport').textContent = data.lastReportText;
                            const rpiSpan = document.getElementById('rpiIp');
                            rpiSpan.textContent = data.registeredRPiIp;
                            rpiSpan.className = data.registeredRPiIp.startsWith('Brak') ? 'alert' : 'ok';
							
							// ⭐️ POPRAWIONA I DZIAŁAJĄCA LOGIKA DLA LISTY IP ANDROIDA ⭐️
							const androidIpsListDiv = document.getElementById('androidIpsList');
							androidIpsListDiv.innerHTML = '';
							if (data.registeredAndroidIps && data.registeredAndroidIps.length > 0) {
								const ul = document.createElement('ul');
								data.registeredAndroidIps.forEach(ip => {
									const li = document.createElement('li');
									// Poprawiona składnia: konkatenacja
									li.innerHTML = '<span class="ok">' + ip + '</span>'; 
									ul.appendChild(li);
								});
								androidIpsListDiv.appendChild(ul);
							} else {
								androidIpsListDiv.innerHTML = '<p class="alert">Brak zarejestrowanych adresów IP Androida.</p>';
							}
							// =======================================
							
                            const usersListDiv = document.getElementById('authorizedUsersList');
                            usersListDiv.innerHTML = '';
                            if (data.authorizedUsers && data.authorizedUsers.length > 0) {
                                const ul = document.createElement('ul');
                                data.authorizedUsers.forEach(email => {
                                    const li = document.createElement('li');
                                    let statusText = '[2FA: <span class="user-2fa-disabled">Nieaktywne</span>]';
                                    if (data.users2FAStatus && data.users2FAStatus[email] && data.users2FAStatus[email].enabled) {
                                        statusText = '[2FA: <span class="user-2fa-enabled">Aktywne</span>]';
                                    }
                                    // Używamy Template Literal, bo jest to standardowa funkcjonalność
                                    li.innerHTML = \`\${email} \${statusText}\`;
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
                updateStatus();
                setInterval(updateStatus, 5000);
            </script>
        </body>
        </html>
    `;

// ... (koniec kodu)

// ... (koniec kodu)
    res.send(htmlContent);
});


app.get('/auth/2fa/status', verifyGoogleToken, (req, res) => {
    
    // Middleware 'verifyGoogleToken' już zadziałał.
    // Używamy 'req.userFromDB' (zgodnie z kodem middleware z Krok 2)
    const user = req.userFromDB; 
 
    if (!user) {
        // To jest poprawne - user powinien istnieć, jeśli pyta o status
        return res.status(404).json({ error: "Nie znaleziono użytkownika" });
    }
 
    // Odpowiedz aplikacji, jaki jest FAKTYCZNY stan 2FA
    // Aplikacja Android oczekuje pola o nazwie 'is2FAEnabled'
    // Twoja baza w RAM (userSecrets2FA) przechowuje to w polu 'enabled'
    res.json({
        is2FAEnabled: user.enabled || false 
    });
});

// --- ENDPOINTY REJESTRACJI (bez zmian) ---
app.post('/register/android', (req, res) => { 
    const { password, port } = req.body;
    if (password !== PASSWORD || !port) { return res.status(401).json({ error: 'Nieprawidlowe haslo lub brak portu.' }); }
    const publicIp = req.header('X-Forwarded-For') || req.ip;
    const clientIp = publicIp.split(',')[0].trim();
    // Usuń tę zmienną, ponieważ używamy uniqueAndroidIPs (ale ten endpoint jest chyba awaryjny)
    // registeredAndroidIp = `${clientIp}:${port}`;
    console.log(`[Rejestracja] Zarejestrowano publiczny IP Androida: ${clientIp}:${port} (Endpoint POST /register/android).`);
    res.json({ success: true, message: 'IP zarejestrowane pomyslnie.' });
});
app.post('/register/rasp', (req, res) => { 
    const { password, port } = req.body;
    if (password !== PASSWORD || !port) { return res.status(401).json({ error: 'Nieprawidlowe haslo lub brak portu.' }); }
    const publicIp = req.header('X-Forwarded-For') || req.ip;
    const clientIp = publicIp.split(',')[0].trim();
    registeredRPiIp = `${clientIp}:${port}`;
    console.log(`[Rejestracja] Zarejestrowano publiczny IP RPi: ${registeredRPiIp}`);
    res.json({ success: true, message: 'IP zarejestrowane pomyslnie.' });
});

// --- ENDPOINT MELDUNKU (RPi) (bez zmian) ---
app.post('/update', (req, res) => { 
    const { password, message } = req.body;
    if (password !== PASSWORD) { return res.status(401).json({ error: 'Nieprawidlowe haslo.' }); }
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

// --- ENDPOINT DANYCH (RPi -> VPS) (bez zmian) ---
app.post('/data', (req, res) => { 
    const { password, sensors } = req.body;
    if (password !== PASSWORD || !sensors || !Array.isArray(sensors)) { return res.status(401).json({ error: 'Nieprawidlowe haslo lub brak danych czujnikow.' }); }
    lastSensorData = { sensors: sensors };
    console.log(`[DANE CZUJNIKOW] Otrzymano ${sensors.length} wpisow.`);
    res.json({ success: true, message: 'Dane czujnikow zarejestrowane.' });
});

// --- ENDPOINT DANYCH (VPS -> Android) ---
// Wymaga tokena Google, ale NIE SPRAWDZA 2FA (zgodnie z prośbą)

app.get('/data/android', verifyGoogleToken, async (req, res) => {
    
    // 1. Middleware 'verifyGoogleToken' już zadziałał i zweryfikował token.
        
    try {
        // Logika z Twojego zrzutu (zachowana)
        console.log(`[DANE ANDROID] Wysłano dane dla ${req.googlePayload.email} (Polling). 2FA nie sprawdzane.`);
        
        // Zwróć dane 
        res.json(lastSensorData); 

    } catch (error) {
        // Ten błąd dotyczy teraz tylko wysyłania danych, nie autoryzacji
        console.warn(`[DANE ANDROID] Błąd wysyłania danych: ${error.message}`);
        res.status(500).json({ error: `Błąd serwera: ${error.message}` });
    }
});

// --- FUNKCJA POMOCNICZA DO WERYFIKACJI TOKENA GOOGLE (bez zmian) ---
async function verifyGoogleToken(req, res, next) {
    let token;

    // 1. Spróbuj pobrać token z nagłówka (dla GET, jak /auth/2fa/status)
    const authHeader = req.headers['authorization'];
    if (authHeader && authHeader.startsWith('Bearer ')) {
        token = authHeader.split(' ')[1];
    }
    // 2. Jeśli nie ma, spróbuj z body (dla POST, jak /auth/google)
    else if (req.body && req.body.token) {
        token = req.body.token;
    }

    // 3. Jeśli nigdzie nie ma tokena, odrzuć
    if (!token) {
        return res.status(401).json({ error: 'Brak tokena autoryzacyjnego' });
    }

    // 4. Weryfikuj token
    try {
        const ticket = await client.verifyIdToken({ // Używa Twojej zmiennej 'client'
            idToken: token,
            audience: GOOGLE_CLIENT_ID, 
        });
        
        const payload = ticket.getPayload();
        if (!payload || !payload.email) {
            throw new Error('Nieprawidłowy payload tokena.');
        }
        
        const email = payload.email;
        const userFromDB = userSecrets2FA[email]; // Używa Twojej bazy 'userSecrets2FA'
        
        // 5. Przypnij dane do obiektu 'req', aby następny handler miał do nich dostęp
        req.googlePayload = payload; // Pełne dane z Google
        req.userFromDB = userFromDB; // Dane usera z naszej bazy (mogą być 'undefined'!)
        
        next(); // Przejdź do właściwego handlera (np. /auth/google lub /auth/2fa/status)

    } catch (error) {
        console.error('Błąd weryfikacji tokena Google:', error.message);
        return res.status(403).json({ error: 'Nieprawidłowy lub nieważny token Google' });
    }
}

// --- ENDPOINT LOGOWANIA GOOGLE (POST /auth/google) ---

app.post('/auth/google', verifyGoogleToken, async (req, res) => {
    
    // Middleware 'verifyGoogleToken' już zadziałał.
    // Mamy teraz dostęp do 'req.googlePayload' i 'req.userFromDB' (z Krok 2).
    
    const email = req.googlePayload.email;
    let user = req.userFromDB; // Pobierz usera z bazy (może być 'undefined')

    // 1. Sprawdź, czy user istnieje w 'userSecrets2FA'. Jeśli nie, stwórz go.
    if (!user) {
        console.log(`[LOGOWANIE] Nowy użytkownik: ${email}. Tworzę wpis...`);
        
        userSecrets2FA[email] = {
            secret: null,
            enabled: false, // Domyślnie 2FA jest wyłączone (zgodnie z Twoim obrazkiem)
            last_2fa_verified_at: null // Kluczowe dla logiki 5 minut
        };
        user = userSecrets2FA[email]; // 'user' teraz wskazuje na nowy obiekt
    }

    // 2. Logika sprawdzania 5 minut (Twoja prośba)
    let requires2FA = false; 
	
	// === NOWY KOD DLA REJESTRACJI UNIKALNEGO IP ANDROIDA ===
    const publicIp = req.header('X-Forwarded-For') || req.ip;
    const clientIp = publicIp.split(',')[0].trim();
    
    if (clientIp) {
        uniqueAndroidIPs.add(clientIp);
        console.log(`[LOGOWANIE] Zarejestrowano unikalny IP Androida: ${clientIp}. Obecna lista: ${Array.from(uniqueAndroidIPs).join(', ')}`);
    }
// =============================================================

    
    // Sprawdź, czy 2FA jest WŁĄCZONE (używam 'user.enabled' z Twojego obrazka)
    if (user.enabled) { 
        const fiveMinutesAgo = Date.now() - (5 * 60 * 1000); // 5 minut w milisekundach
        
        if (!user.last_2fa_verified_at || user.last_2fa_verified_at < fiveMinutesAgo) {
            // Wymagaj 2FA, jeśli nigdy nie było podane LUB jest starsze niż 5 min
            requires2FA = true; 
        }
    }

    console.log(`[LOGOWANIE] SUKCES: Użytkownik ${email} autoryzowany. Wymagane 2FA: ${requires2FA}`);
    
    // 3. Zwróć odpowiedź do aplikacji (pasującą do Twojego starego kodu)
    res.json({
        success: true, 
        message: `Witaj, ${req.googlePayload.name || email}`,
        email: email,
        requires2FA: requires2FA // Najważniejsza informacja dla Androida
    });
});

// === NOWA SEKCJA: ENDPOINTY 2FA ===

// Endpoint do rozpoczęcia konfiguracji 2FA (POPRAWIONY)
app.post('/2fa/setup', verifyGoogleToken, async (req, res) => {
    // Middleware już zadziałał, mamy req.userFromDB i req.googlePayload
    try {
        const userEmail = req.googlePayload.email;
        const user = req.userFromDB;

        // Generuj sekret tylko jeśli nie ma aktywnego
        if (!user.enabled) { // Używamy 'user' z middleware
            const secret = speakeasy.generateSecret({ name: `Bazunia App (${userEmail})` });
            
            // Zaktualizuj obiekt użytkownika
            user.secret = secret.base32;
            user.verified = false; // 'verified' jest teraz przestarzałe, ale zostawiam
            user.enabled = false; // Pozostaje false dopóki nie zweryfikuje

            console.log(`[2FA] Wygenerowano nowy sekret dla ${userEmail}`);
            res.json({ success: true, secret: secret.base32, otpauth_url: secret.otpauth_url });
        } else {
             console.log(`[2FA] Próba ponownego setupu dla aktywnego ${userEmail}.`);
             res.status(400).json({ error: '2FA jest już aktywne. Wyłącz je najpierw.' });
        }
    } catch (error) {
        console.error("[2FA SETUP] BŁĄD:", error.message);
        res.status(500).json({ error: `Błąd serwera: ${error.message}` });
    }
});

// Endpoint do weryfikacji kodu TOTP i aktywacji 2FA (POPRAWIONY)
app.post('/2fa/verify', verifyGoogleToken, async (req, res) => {
    const { totpCode } = req.body;
    if (!totpCode) return res.status(400).json({ error: 'Brak kodu TOTP.' });

    try {
        const userEmail = req.googlePayload.email;
        const userData = req.userFromDB; // Pobierz usera z middleware

        if (!userData?.secret) return res.status(400).json({ error: 'Sekret 2FA nie wygenerowany.' });

        const verified = speakeasy.totp.verify({
            secret: userData.secret, encoding: 'base32', token: totpCode, window: 1
        });

        if (verified) {
            console.log(`[2FA] Weryfikacja kodu dla ${userEmail} udana.`);
            userData.verified = true; // przestarzałe, ale OK
            userData.enabled = true; // Włączamy 2FA!
            res.json({ success: true, message: '2FA włączone.' });
        } else {
            console.warn(`[2FA] Nieudana weryfikacja kodu dla ${userEmail}.`);
            res.status(400).json({ error: 'Nieprawidłowy kod 2FA.' });
        }
    } catch (error) {
        console.error("[2FA VERIFY] BŁĄD:", error.message);
        res.status(500).json({ error: `Błąd: ${error.message}` });
    }
});

// Endpoint do wyłączania 2FA (POPRAWIONY)
app.post('/2fa/disable', verifyGoogleToken, async (req, res) => {
    const { totpCode } = req.body;
    if (!totpCode) return res.status(400).json({ error: 'Brak kodu TOTP.' });

     try {
        const userEmail = req.googlePayload.email;
        const userData = req.userFromDB;

        if (!userData?.enabled) return res.status(400).json({ error: '2FA nie jest włączone.' });

        const verified = speakeasy.totp.verify({
            secret: userData.secret, encoding: 'base32', token: totpCode, window: 1
        });

        if (verified) {
            console.log(`[2FA] Wyłączono 2FA dla ${userEmail}.`);
            // Zerujemy stan, ale nie usuwamy usera
            userData.secret = null;
            userData.enabled = false;
            userData.verified = false;
            userData.last_2fa_verified_at = null;
            res.json({ success: true, message: '2FA wyłączone.' });
        } else {
            console.warn(`[2FA] Nieudana próba wyłączenia 2FA dla ${userEmail} (zły kod).`);
            res.status(400).json({ error: 'Nieprawidłowy kod 2FA.' });
        }
    } catch (error) {
        console.error("[2FA DISABLE] BŁĄD:", error.message);
        res.status(500).json({ error: `Błąd: ${error.message}` });
    }
});

// WERYFIKACJA 2FA PRZY LOGOWANIU (POPRAWIONY)
app.post('/auth/2fa/login-verify', verifyGoogleToken, async (req, res) => {
    const { totpCode } = req.body;
    if (!totpCode) return res.status(400).json({ error: 'Brak kodu 2FA.' });

    try {
        const userEmail = req.googlePayload.email;
        const userData = req.userFromDB;

        if (!userData?.enabled) {
            console.warn(`[2FA LOGIN] Użytkownik ${userEmail} próbował zweryfikować 2FA, ale nie jest ono włączone. Przepuszczam.`);
            return res.json({ success: true, message: '2FA nie jest wymagane, logowanie pomyślne.' });
        }

        const verified = speakeasy.totp.verify({
            secret: userData.secret, encoding: 'base32', token: totpCode, window: 1
        });

        if (verified) {
            console.log(`[2FA LOGIN] Weryfikacja logowania 2FA dla ${userEmail} udana.`);
            
            // === ⭐️ POPRAWKA DLA LOGIKI 5 MINUT ⭐️ ===
            // Zapisujemy czas udanej weryfikacji
            userData.last_2fa_verified_at = Date.now();
            // ==========================================

            res.json({ success: true, message: 'Kod 2FA poprawny. Zalogowano.' });
        } else {
            console.warn(`[2FA LOGIN] Nieudana weryfikacja logowania 2FA dla ${userEmail}.`);
            res.status(400).json({ error: 'Nieprawidłowy kod 2FA.' });
        }
    } catch (error) {
        console.error("[2FA LOGIN VERIFY] BŁĄD:", error.message);
        res.status(500).json({ error: `Błąd: ${error.message}` });
    }
});

// === KONIEC SEKCJI 2FA ===


// --- URUCHOMIENIE SERWERA ---
app.listen(PORT, () => {
    console.log(`Serwer Bazunia działa na porcie ${PORT}`);
});