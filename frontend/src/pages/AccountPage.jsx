import {
  Box, Heading, Text, Spinner, Alert, AlertIcon,
  VStack, Button, useToast, FormControl, FormLabel, Input,
  Divider, SimpleGrid,
  // ⭐️ DODANE IMPORTY DLA 2FA ⭐️
  Tag, 
  Image, 
  HStack, 
  useClipboard 
} from '@chakra-ui/react';
import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';

// Endpointy, których będziemy używać
const API_GET_USER_DETAILS = '/api/user/me';
const API_CHANGE_PASSWORD = '/api/user/change-password';

// ⭐️ ENDPOINTY DLA 2FA (z AuthController.java) ⭐️
const API_2FA_STATUS = '/api/auth/2fa/status';
const API_2FA_SETUP = '/api/auth/2fa/setup';
const API_2FA_VERIFY = '/api/auth/2fa/verify';
const API_2FA_DISABLE = '/api/auth/2fa/disable';


function AccountPage() {
  const { getToken, logout, userEmail: emailFromContext } = useAuth();
  const toast = useToast();

  // --- Stany dla Danych Użytkownika i Hasła (bez zmian) ---
  const [userData, setUserData] = useState({ email: emailFromContext, name: '' });
  const [isLoadingUser, setIsLoadingUser] = useState(true);
  const [errorUser, setErrorUser] = useState(null);
  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [isLoadingPassword, setIsLoadingPassword] = useState(false);

  // ------------------------------------
  // ⭐️⭐️ POCZĄTEK: NOWE STANY DLA 2FA ⭐️⭐️
  // ------------------------------------
  
  // Przechowuje aktualny status 2FA (true/false)
  const [twoFaStatus, setTwoFaStatus] = useState(false);
  // Pokazuje spinner obok statusu
  const [isLoadingTwoFaStatus, setIsLoadingTwoFaStatus] = useState(true);
  // Przechowuje dane z /setup (secret i otpauth_url)
  const [twoFaSetupData, setTwoFaSetupData] = useState(null);
  // Kod weryfikacyjny wpisywany przez użytkownika
  const [twoFaVerifyCode, setTwoFaVerifyCode] = useState('');
  // Loading dla przycisków (Włącz/Weryfikuj/Wyłącz)
  const [isProcessingTwoFa, setIsProcessingTwoFa] = useState(false);
  // Hook do kopiowania sekretu
  const { onCopy: onCopySecret, hasCopied: hasCopiedSecret } = useClipboard(twoFaSetupData?.secret || '');

  // ------------------------------------
  // ⭐️⭐️ KONIEC: NOWE STANY DLA 2FA ⭐️⭐️
  // ------------------------------------


  // --- 1. POBIERANIE DANYCH (UŻYTKOWNIKA ORAZ STATUSU 2FA) ---
  useEffect(() => {
    const token = getToken();
    
    // --- Pobieranie danych użytkownika (bez zmian) ---
    const fetchUserDetails = async () => {
      setIsLoadingUser(true);
      setErrorUser(null);
      try {
        const response = await fetch(API_GET_USER_DETAILS, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.status === 401) {
          toast({ title: 'Sesja wygasła', status: 'error', isClosable: true });
          logout();
          return;
        }
        if (!response.ok) throw new Error('Nie udało się pobrać danych użytkownika.');
        const data = await response.json();
        setUserData({ email: data.email, name: data.name || 'Brak' });
      } catch (err) {
        setErrorUser(err.message);
      } finally {
        setIsLoadingUser(false);
      }
    };

    // ⭐️ NOWA FUNKCJA: Pobieranie statusu 2FA ⭐️
    const fetchTwoFaStatus = async () => {
      setIsLoadingTwoFaStatus(true);
      try {
        const response = await fetch(API_2FA_STATUS, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error('Nie udało się pobrać statusu 2FA.');
        const data = await response.json();
        setTwoFaStatus(data.is2FAEnabled || false);
      } catch (err) {
        console.error(err.message);
        // Nie pokazuj błędu, po prostu zostaw jako wyłączone
      } finally {
        setIsLoadingTwoFaStatus(false);
      }
    };

    // Wywołaj obie funkcje
    fetchUserDetails();
    fetchTwoFaStatus();
    
  }, [getToken, logout, toast]);

  // --- 2. LOGIKA ZMIANY HASŁA (bez zmian) ---
  const handlePasswordChange = (e) => {
    // ... (bez zmian) ...
    const { name, value } = e.target;
    setPasswords(prev => ({ ...prev, [name]: value }));
  };

  const handlePasswordSubmit = async (e) => {
    // ... (bez zmian) ...
    e.preventDefault();
    setIsLoadingPassword(true);

    if (passwords.newPassword.length < 8) {
      toast({ title: 'Hasło musi mieć co najmniej 8 znaków', status: 'warning', isClosable: true });
      setIsLoadingPassword(false);
      return;
    }
    if (passwords.newPassword !== passwords.confirmPassword) {
      toast({ title: 'Nowe hasła nie są zgodne', status: 'warning', isClosable: true });
      setIsLoadingPassword(false);
      return;
    }

    const token = getToken();
    try {
      const response = await fetch(API_CHANGE_PASSWORD, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          currentPassword: passwords.currentPassword,
          newPassword: passwords.newPassword
        })
      });
      if (response.status === 403) throw new Error('Obecne hasło jest nieprawidłowe.');
      if (!response.ok) throw new Error('Wystąpił błąd serwera.');

      toast({ title: 'Hasło zmienione pomyślnie!', status: 'success', isClosable: true });
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      toast({ title: 'Błąd zmiany hasła', description: err.message, status: 'error', isClosable: true });
    } finally {
      setIsLoadingPassword(false);
    }
  };


  // ------------------------------------
  // ⭐️⭐️ POCZĄTEK: NOWE FUNKCJE 2FA ⭐️⭐️
  // ------------------------------------

  // Krok 1: Kliknięcie "Włącz 2FA" -> pobiera QR kod i sekret
  const handleSetup2FA = async () => {
    setIsProcessingTwoFa(true);
    setTwoFaSetupData(null); // Zresetuj stare dane
    try {
      const response = await fetch(API_2FA_SETUP, {
        method: 'POST', // Używamy POST, jak w Androidzie
        headers: { 'Authorization': `Bearer ${getToken()}` }
      });
      if (!response.ok) throw new Error('Nie udało się rozpocząć konfiguracji 2FA.');
      
      const data = await response.json();
      if (!data.success) throw new Error(data.message || 'Serwer odrzucił konfigurację.');

      setTwoFaSetupData({
        secret: data.secret,
        otpauth_url: data.otpauth_url
      });

    } catch (err) {
      toast({ title: 'Błąd konfiguracji 2FA', description: err.message, status: 'error', isClosable: true });
    } finally {
      setIsProcessingTwoFa(false);
    }
  };

  // Krok 2: Wpisanie kodu z aplikacji i kliknięcie "Weryfikuj"
  const handleVerify2FA = async (e) => {
    e.preventDefault();
    if (!twoFaVerifyCode || twoFaVerifyCode.length !== 6) {
      toast({ title: 'Kod 2FA musi mieć 6 cyfr', status: 'warning', isClosable: true });
      return;
    }
    setIsProcessingTwoFa(true);
    try {
      const response = await fetch(API_2FA_VERIFY, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`
        },
        body: JSON.stringify({ totpCode: twoFaVerifyCode })
      });

      if (!response.ok) {
         // Serwer zwraca 400 dla złego kodu
         throw new Error('Nieprawidłowy kod 2FA. Spróbuj ponownie.');
      }

      toast({ title: '2FA Włączone Pomyślnie!', status: 'success', isClosable: true });
      setTwoFaStatus(true); // Aktualizuj status
      setTwoFaSetupData(null); // Zamknij sekcję konfiguracji
      setTwoFaVerifyCode(''); // Wyczyść pole

    } catch (err) {
      toast({ title: 'Błąd weryfikacji 2FA', description: err.message, status: 'error', isClosable: true });
    } finally {
      setIsProcessingTwoFa(false);
    }
  };

  // Krok 3: Wyłączenie 2FA (wymaga kodu)
  const handleDisable2FA = async (e) => {
    e.preventDefault();
    if (!twoFaVerifyCode || twoFaVerifyCode.length !== 6) {
      toast({ title: 'Kod 2FA musi mieć 6 cyfr', status: 'warning', isClosable: true });
      return;
    }
    setIsProcessingTwoFa(true);
    try {
      const response = await fetch(API_2FA_DISABLE, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`
        },
        body: JSON.stringify({ totpCode: twoFaVerifyCode })
      });
      
      if (!response.ok) {
         throw new Error('Nieprawidłowy kod 2FA. Spróbuj ponownie.');
      }

      toast({ title: '2FA Wyłączone Pomyślnie!', status: 'info', isClosable: true });
      setTwoFaStatus(false); // Aktualizuj status
      setTwoFaSetupData(null); // Zamknij sekcję
      setTwoFaVerifyCode(''); // Wyczyść pole

    } catch (err) {
      toast({ title: 'Błąd wyłączania 2FA', description: err.message, status: 'error', isClosable: true });
    } finally {
      setIsProcessingTwoFa(false);
    }
  };

  // Anulowanie konfiguracji (zamyka QR kod)
  const cancelSetup = () => {
    setTwoFaSetupData(null);
    setTwoFaVerifyCode('');
  };

  // ------------------------------------
  // ⭐️⭐️ KONIEC: NOWE FUNKCJE 2FA ⭐️⭐️
  // ------------------------------------


  // --- 3. RENDEROWANIE WIDOKU ---
  
  const renderUserDetails = () => {
    // ... (bez zmian) ...
    if (isLoadingUser) return <Spinner />;
    if (errorUser) {
      return (
        <Alert status="error">
          <AlertIcon />
          {errorUser}
        </Alert>
      );
    }
    return (
      <VStack spacing={3} align="flex-start">
        <Box>
          <Text fontSize="sm" color="gray.400">Adres e-mail</Text>
          <Text fontSize="lg" fontWeight="bold">{userData.email}</Text>
        </Box>
        <Box>
          <Text fontSize="sm" color="gray.400">Imię / Nazwa</Text>
          <Text fontSize="lg" fontWeight="bold">{userData.name}</Text>
        </Box>
      </VStack>
    );
  };

  // ⭐️ NOWA FUNKCJA: Renderowanie statusu 2FA (jak w UsersPage) ⭐️
  const renderTwoFaStatus = () => {
    if (isLoadingTwoFaStatus) return <Spinner size="sm" />;
    
    return twoFaStatus ? (
      <Tag size="md" colorScheme="green">2FA Włączone</Tag>
    ) : (
      <Tag size="md" colorScheme="yellow">Brak 2FA</Tag>
    );
  };

  // ⭐️ NOWA FUNKCJA: Renderowanie sekcji konfiguracji lub wyłączania 2FA ⭐️
  const renderTwoFaSection = () => {
    // 1. Widok WŁĄCZANIA (po kliknięciu "Włącz 2FA")
    if (twoFaSetupData && !twoFaStatus) {
      return (
        <VStack as="form" spacing={4} align="stretch" p={4} borderWidth="1px" borderRadius="md" bg="gray.700" mt={4} onSubmit={handleVerify2FA}>
          <Text fontWeight="bold">Krok 1: Zeskanuj kod QR</Text>
          <Text fontSize="sm">Użyj aplikacji Google Authenticator (lub podobnej), aby zeskanować poniższy kod.</Text>
          <Image
            src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(twoFaSetupData.otpauth_url)}`}
            alt="QR Code"
            boxSize="200px"
            mx="auto"
            bg="white"
            p={2}
            borderRadius="md"
          />
          <Text fontSize="sm" textAlign="center">Lub wprowadź klucz ręcznie:</Text>
          <HStack>
            <Input
              value={twoFaSetupData.secret}
              isReadOnly
              fontFamily="monospace"
              size="sm"
            />
            <Button size="sm" onClick={onCopySecret}>
              {hasCopiedSecret ? 'Skopiowano!' : 'Kopiuj'}
            </Button>
          </HStack>
          
          <Divider my={2} />
          
          <Text fontWeight="bold">Krok 2: Wprowadź kod weryfikacyjny</Text>
          <FormControl isRequired>
            <FormLabel>Kod 2FA</FormLabel>
            <Input
              type="text"
              name="twoFaVerifyCode"
              value={twoFaVerifyCode}
              onChange={(e) => setTwoFaVerifyCode(e.target.value.replace(/\D/g, ''))} // Tylko cyfry
              placeholder="123456"
              maxLength={6}
            />
          </FormControl>
          <HStack>
            <Button
              type="submit"
              colorScheme="green"
              isLoading={isProcessingTwoFa}
            >
              Weryfikuj i Włącz
            </Button>
            <Button variant="ghost" onClick={cancelSetup} isDisabled={isProcessingTwoFa}>
              Anuluj
            </Button>
          </HStack>
        </VStack>
      );
    }

    // 2. Widok WYŁĄCZANIA (po kliknięciu "Wyłącz 2FA")
    if (twoFaSetupData && twoFaStatus) {
       return (
        <VStack as="form" spacing={4} align="stretch" p={4} borderWidth="1px" borderRadius="md" bg="gray.700" mt={4} onSubmit={handleDisable2FA}>
          <Text fontWeight="bold">Potwierdź wyłączenie 2FA</Text>
          <Text fontSize="sm">Aby potwierdzić, wprowadź kod 2FA ze swojej aplikacji.</Text>
          <FormControl isRequired>
            <FormLabel>Obecny kod 2FA</FormLabel>
            <Input
              type="text"
              name="twoFaVerifyCode"
              value={twoFaVerifyCode}
              onChange={(e) => setTwoFaVerifyCode(e.target.value.replace(/\D/g, ''))}
              placeholder="123456"
              maxLength={6}
            />
          </FormControl>
          <HStack>
            <Button
              type="submit"
              colorScheme="red"
              isLoading={isProcessingTwoFa}
            >
              Potwierdź i Wyłącz
            </Button>
            <Button variant="ghost" onClick={cancelSetup} isDisabled={isProcessingTwoFa}>
              Anuluj
            </Button>
          </HStack>
        </VStack>
      );
    }
    
    // 3. Widok domyślny (żaden)
    return null;
  };


  return (
    <Box p={5} shadow="md" borderWidth="1px" borderRadius="md" bg="gray.800" maxW="4xl" mx="auto">
      <Heading size="lg" mb={6}>Moje Konto</Heading>

      {/* --- Sekcja Dane i Hasło (bez zmian) --- */}
      <SimpleGrid columns={{ base: 1, md: 2 }} spacing={10}>
        
        {/* Kolumna 1: Dane Użytkownika */}
        <VStack spacing={4} align="flex-start">
          <Heading size="md" mb={2}>Twoje Dane</Heading>
          {renderUserDetails()}
        </VStack>

        {/* Kolumna 2: Zmiana Hasła */}
        <VStack as="form" spacing={4} align="flex-start" onSubmit={handlePasswordSubmit}>
          <Heading size="md" mb={2}>Zmień Hasło</Heading>
          {/* ... (reszta formularza zmiany hasła bez zmian) ... */}
          <FormControl isRequired>
            <FormLabel>Obecne hasło</FormLabel>
            <Input
              type="password"
              name="currentPassword"
              value={passwords.currentPassword}
              onChange={handlePasswordChange}
              placeholder="••••••••"
            />
          </FormControl>
          <FormControl isRequired>
            <FormLabel>Nowe hasło (min. 8 znaków)</FormLabel>
            <Input
              type="password"
              name="newPassword"
              value={passwords.newPassword}
              onChange={handlePasswordChange}
              placeholder="••••••••"
            />
          </FormControl>
          <FormControl isRequired>
            <FormLabel>Potwierdź nowe hasło</FormLabel>
            <Input
              type="password"
              name="confirmPassword"
              value={passwords.confirmPassword}
              onChange={handlePasswordChange}
              placeholder="••••••••"
            />
          </FormControl>
          <Button
            type="submit"
            colorScheme="blue"
            isLoading={isLoadingPassword}
          >
            Zaktualizuj hasło
          </Button>
        </VStack>
      </SimpleGrid>

      {/* // ------------------------------------
      // ⭐️⭐️ POCZĄTEK: NOWA SEKCJA 2FA ⭐️⭐️
      // ------------------------------------
      */}
      <Divider my={8} />

      <Box>
        <Heading size="md" mb={4}>Zarządzanie 2FA</Heading>
        <HStack spacing={4} mb={4}>
          <Text>Status Uwierzytelniania Dwuskładnikowego:</Text>
          {renderTwoFaStatus()}
        </HStack>

        {/* Główny przycisk Włącz/Wyłącz */}
        {!twoFaSetupData && !twoFaStatus && (
          <Button
            colorScheme="green"
            onClick={handleSetup2FA}
            isLoading={isProcessingTwoFa}
          >
            Włącz 2FA
          </Button>
        )}
        {!twoFaSetupData && twoFaStatus && (
          <Button
            colorScheme="red"
            onClick={() => setTwoFaSetupData({ disabling: true })} // Otwórz sekcję wyłączania
            isLoading={isProcessingTwoFa}
          >
            Wyłącz 2FA
          </Button>
        )}
        
        {/* Renderowanie sekcji Włączania lub Wyłączania.
          Ta funkcja zwróci null, jeśli żaden proces nie jest aktywny.
        */}
        {renderTwoFaSection()}
        
      </Box>
      {/* // ------------------------------------
      // ⭐️⭐️ KONIEC: NOWA SEKCJA 2FA ⭐️⭐️
      // ------------------------------------
      */}

    </Box>
  );
}

export default AccountPage;