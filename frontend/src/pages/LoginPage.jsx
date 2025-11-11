import {
  Box, Button, FormControl, FormLabel, Input,
  VStack, Heading, useToast, Flex, Text,
  HStack 
} from '@chakra-ui/react';
import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link as RouterLink, useNavigate } from 'react-router-dom';

function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const auth = useAuth();
  
  const [view, setView] = useState('login');
  
  const [resetEmail, setResetEmail] = useState('');
  const [twoFaCode, setTwoFaCode] = useState('');
  const [tempToken, setTempToken] = useState(null); 

  const [isLoading, setIsLoading] = useState(false);
  const toast = useToast();

  const handleSubmitLogin = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: username, password: password })
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Nieprawidłowy login lub hasło.');
      }

      if (data.requires2FA) {
        setTempToken(data.jwt);
        setView('2fa'); 
        setIsLoading(false);
      } else {
        localStorage.setItem('jwtToken', data.jwt);
        // ⭐️ POPRAWKA 1 ⭐️
        window.location.href = '/'; // Zamiast navigate(0)
      }

    } catch (err) {
      toast({
        title: 'Błąd logowania',
        description: err.message,
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
      setIsLoading(false);
    }
  };

  const handleSubmit2FA = async (e) => {
    e.preventDefault();
    if (!twoFaCode || twoFaCode.length !== 6) {
      toast({ title: 'Kod 2FA musi mieć 6 cyfr', status: 'warning', isClosable: true });
      return;
    }
    
    setIsLoading(true);

    try {
      const response = await fetch('/api/auth/2fa/email-verify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${tempToken}`
        },
        body: JSON.stringify({ totpCode: twoFaCode }),
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Nieprawidłowy kod 2FA.');
      }

      const finalToken = data.jwt || data.token; 
      if (!finalToken) {
         throw new Error('Nie otrzymano finalnego tokena od serwera.');
      }

      localStorage.setItem('jwtToken', finalToken);
      // ⭐️ POPRAWKA 2 ⭐️
      window.location.href = '/'; // Zamiast navigate(0)

    } catch (err) {
      toast({
        title: 'Błąd weryfikacji 2FA',
        description: err.message,
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
      setIsLoading(false);
    }
  };

  // Funkcja resetowania hasła (bez zmian)
  const handleSubmitReset = async (e) => {
    e.preventDefault();
    if (!resetEmail) {
      toast({ title: 'Błąd', description: 'Proszę podać adres e-mail.', status: 'warning', isClosable: true });
      return;
    }
    
    setIsLoading(true);
    try {
      await fetch('/api/auth/request-password-reset', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: resetEmail }),
      });
      toast({
        title: 'Wysłano prośbę',
        description: 'Jeśli konto o podanym adresie e-mail istnieje, wysłaliśmy na nie link do resetowania hasła.',
        status: 'success',
        duration: 6000,
        isClosable: true,
      });
      setView('login');
      setResetEmail('');
    } catch (err) {
      toast({ title: 'Błąd sieci', description: 'Nie udało się wysłać prośby.', status: 'error', duration: 3000, isClosable: true });
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel2FA = () => {
    setView('login');
    setTempToken(null);
    setTwoFaCode('');
  };

  // Renderowanie (bez zmian)
  const renderForm = () => {
    if (view === 'login') {
      return (
        <VStack as="form" spacing={4} onSubmit={handleSubmitLogin}>
          <Heading mb={4}>Logowanie</Heading>
          <FormControl isRequired>
            <FormLabel>Login (Email)</FormLabel>
            <Input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="admin@example.com"
            />
          </FormControl>
          <FormControl isRequired>
            <FormLabel>Hasło</FormLabel>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </FormControl>
          <Button
            type="submit"
            colorScheme="blue"
            width="full"
            isLoading={isLoading}
          >
            Zaloguj
          </Button>
          <Button
            variant="link"
            colorScheme="blue"
            size="sm"
            onClick={() => setView('reset')}
          >
            Zapomniałeś hasła?
          </Button>
        </VStack>
      );
    }

    if (view === 'reset') {
      return (
        <VStack as="form" spacing={4} onSubmit={handleSubmitReset}>
          <Heading mb={4} size="lg">Zresetuj hasło</Heading>
          <Text textAlign="center" fontSize="sm" color="gray.300">
            Podaj e-mail powiązany z kontem. Wyślemy link do resetowania hasła.
          </Text>
          <FormControl isRequired>
            <FormLabel>E-mail</FormLabel>
            <Input
              type="email"
              value={resetEmail}
              onChange={(e) => setResetEmail(e.target.value)}
              placeholder="twoj.email@przyklad.com"
            />
          </FormControl>
          <Button
            type="submit"
            colorScheme="blue"
            width="full"
            isLoading={isLoading}
          >
            Wyślij link
          </Button>
          <Button
            variant="link"
            colorScheme="gray"
            size="sm"
            onClick={() => setView('login')}
          >
            Wróć do logowania
          </Button>
        </VStack>
      );
    }

    if (view === '2fa') {
      return (
        <VStack as="form" spacing={4} onSubmit={handleSubmit2FA}>
          <Heading mb={2} size="lg">Weryfikacja 2FA</Heading>
          <Text textAlign="center" fontSize="sm" color="gray.300">
            Otwórz aplikację uwierzytelniającą i wpisz 6-cyfrowy kod.
          </Text>
          <FormControl isRequired>
            <FormLabel>Kod 2FA</FormLabel>
            <Input
              type="text" 
              value={twoFaCode}
              onChange={(e) => setTwoFaCode(e.target.value.replace(/\D/g, ''))}
              placeholder="123456"
              maxLength={6}
              textAlign="center"
              letterSpacing="0.5em"
            />
          </FormControl>
          <HStack width="full">
             <Button
              variant="ghost"
              colorScheme="gray"
              width="full"
              onClick={handleCancel2FA}
              isDisabled={isLoading}
            >
              Anuluj
            </Button>
            <Button
              type="submit"
              colorScheme="blue"
              width="full"
              isLoading={isLoading}
            >
              Zatwierdź
            </Button>
          </HStack>
        </VStack>
      );
    }
  };

  return (
    <Flex align="center" justify="center" minH="80vh">
      <Box p={8} maxW="md" borderWidth={1} borderRadius="lg" boxShadow="lg" bg="gray.800" minW="400px">
        
        {renderForm()}

        {view === 'login' && (
          <Text mt={6} textAlign="center">
            Nie masz konta?{' '}
            <Button as={RouterLink} to="/register" colorScheme="blue" variant="link">
              Zarejestruj się
            </Button>
          </Text>
        )}

      </Box>
    </Flex>
  );
}

export default LoginPage;