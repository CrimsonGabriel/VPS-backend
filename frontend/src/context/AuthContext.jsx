// 💾 src/context/AuthContext.jsx (OSTATECZNA POPRAWIONA WERSJA)

import React, { createContext, useContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const AuthContext = createContext(null);

const TOKEN_STORAGE_KEY = 'jwtToken'; 
const getToken = () => localStorage.getItem(TOKEN_STORAGE_KEY);
const setToken = (token) => localStorage.setItem(TOKEN_STORAGE_KEY, token);
const removeToken = () => localStorage.removeItem(TOKEN_STORAGE_KEY);

// --- FUNKCJA POMOCNICZA DO DEKODOWANIA JWT (BEZ ZMIAN) ---
// Ta funkcja jest poprawna, dekoduje Base64URL niezależnie od algorytmu podpisu.
const decodeJwt = (jwtToken) => {
    try {
        const base64Url = jwtToken.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error("Błąd dekodowania JWT:", e);
        return null;
    }
};

export const AuthProvider = ({ children }) => {
    const [token, setTokenState] = useState(getToken());
    const [isAuthenticated, setIsAuthenticated] = useState(!!getToken());
    const [userRole, setUserRole] = useState(null); 
    const [userEmail, setUserEmail] = useState(null);
    const [loading, setLoading] = useState(true); 
    
    const navigate = useNavigate();
    
    // Funkcja wylogowania, używana teraz także przy wygaśnięciu tokena
    const logout = () => {
        removeToken();
        setTokenState(null);
        setIsAuthenticated(false);
        setUserRole(null);
        setUserEmail(null);
        navigate('/');
    };

    useEffect(() => {
        setLoading(true);
        const currentToken = getToken();

        if (currentToken) {
            const decodedPayload = decodeJwt(currentToken);
            
            if (decodedPayload) {
                
                // Sprawdzamy, czy token nie wygasł (ta logika jest poprawna i zostaje)
                if (decodedPayload.exp * 1000 < Date.now()) {
                    console.warn("Token JWT wygasł. Automatyczne wylogowywanie...");
                    logout(); 
                    setLoading(false); 
                    return; 
                }

                // ⭐️⭐️⭐️ POCZĄTEK KLUCZOWEJ POPRAWKI ⭐️⭐️⭐️
                // 
                // Usuwamy "agresywne skanowanie" ról. 
                // Ufamy TYLKO polu "role", które jest wysyłane przez JwtService.
                //
                
                let roleFromToken = decodedPayload.role; // np. "ADMIN" lub "USER"
                let finalRole = 'ROLE_USER'; // Domyślna rola, jeśli coś pójdzie nie tak

                if (typeof roleFromToken === 'string') {
                    // Normalizujemy dla pewności (np. jeśli backend wysłał "ROLE_ADMIN")
                    let normalized = roleFromToken.toUpperCase().replace('ROLE_', '');
                    
                    if (normalized.includes('ADMIN')) {
                        finalRole = 'ROLE_ADMIN';
                    } else if (normalized.includes('USER')) {
                        finalRole = 'ROLE_USER';
                    }
                }
                
                // ⭐️⭐️⭐️ KONIEC KLUCZOWEJ POPRAWKI ⭐️⭐️⭐️
                
                setUserRole(finalRole);
                setUserEmail(decodedPayload.sub || decodedPayload.email);
                setIsAuthenticated(true);

            } else {
                // Token był w localStorage, ale nie dało się go zdekodować
                logout(); 
            }
        } else {
            // Brak tokena w localStorage
            setIsAuthenticated(false);
            setUserRole(null);
            setUserEmail(null);
        }
        setLoading(false);
    }, [token, navigate]); // Dodano 'navigate' do zależności


    const login = async (username, password) => {
        
        const payload = JSON.stringify({ email: username, password: password });
        console.log('Payload wysyłany do Springa:', payload); 
        
        try {
            const response = await fetch('/api/auth/login', { 
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: payload 
            });

            if (!response.ok) { 
                const errorData = await response.json().catch(() => ({ error: 'Nieznany błąd serwera.' }));
                throw new Error(errorData.error || 'Błąd uwierzytelniania.');
            }

            const data = await response.json(); 
            const jwtToken = data.jwt; 

            if (!jwtToken) { throw new Error('Nie otrzymano tokena JWT z serwera (pole "jwt" było puste)'); }

            setToken(jwtToken);
            setTokenState(jwtToken); // To odpali ponowne uruchomienie useEffect
            
            console.log('Logowanie pomyślne, zapisano JWT.');
            navigate('/'); 
            return true;

        } catch (error) {
            console.error('Błąd logowania:', error.message);
            removeToken();
            setTokenState(null);
            setIsAuthenticated(false);
            return false;
        }
    };

    // Przeniosłem definicję 'logout' wyżej, aby była dostępna w useEffect

    // Ta logika jest teraz poprawna, bo 'userRole' jest ustawiane wiarygodnie
    const isAdmin = userRole && userRole.toUpperCase().includes('ADMIN');

    const value = {
        isAuthenticated,
        isAdmin, 
        userRole,
        userEmail,
        loading,
        login,
        logout,
        getToken,
    };
    
    // Zmieniono na prosty spinner lub null, aby uniknąć migotania
    if (loading) {
        return null; 
    }

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    return useContext(AuthContext);
};