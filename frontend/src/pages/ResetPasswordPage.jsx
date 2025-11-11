import { useState, useEffect } from 'react';
import {
  Box,
  Heading,
  FormControl,
  FormLabel,
  Input,
  Button,
  Alert,
  AlertIcon,
  Text,
  useToast,
  VStack,
} from '@chakra-ui/react';
import { useNavigate, Link as RouterLink, useSearchParams } from 'react-router-dom';

function ResetPasswordPage() {
  // Stany dla formularza
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  
  // Stan dla tokena z URL
  const [token, setToken] = useState(null);

  const toast = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Krok 1: Pobierz token z parametrów URL po załadowaniu strony
  useEffect(() => {
    const tokenFromUrl = searchParams.get('token');
    if (tokenFromUrl) {
      setToken(tokenFromUrl);
    } else {
      setError('Brak tokena resetującego. Link jest nieprawidłowy lub niekompletny.');
      toast({
        title: 'Błąd linku',
        description: 'Nie znaleziono tokena w adresie URL.',
        status: 'error',
        isClosable: true,
      });
    }
  }, [searchParams, toast]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    // Krok 2: Walidacja po stronie klienta
    if (!token) {
        setError('Nie można kontynuować bez tokena resetującego.');
        return;
    }
    if (password.length < 8) {
      setError('Hasło musi mieć co najmniej 8 znaków.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Hasła nie są zgodne.');
      return;
    }

    setLoading(true);

    try {
      // Krok 3: Wyślij żądanie do nowego endpointu
      const response = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token: token, newPassword: password }),
      });

      // Krok 4: Obsługa odpowiedzi
      if (response.ok) {
        // SUKCES (200 OK)
        toast({
          title: 'Hasło zresetowane!',
          description: 'Twoje hasło zostało pomyślnie zmienione. Możesz się teraz zalogować.',
          status: 'success',
          duration: 5000,
          isClosable: true,
        });
        navigate('/login'); // Przekieruj na stronę logowania

      } else {
        // BŁĄD (np. 400 Bad Request - token wygasł/zły)
        const errorData = await response.json();
        setError(errorData.error || 'Wystąpił nieznany błąd.');
      }

    } catch (err) {
      setError('Błąd połączenia z serwerem. Sprawdź swoje połączenie internetowe.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box maxW="md" mx="auto" mt={20} p={8} borderWidth={1} borderRadius="lg" shadow="lg" bg="gray.800">
      <Heading as="h1" size="lg" textAlign="center" mb={6}>
        Ustaw nowe hasło
      </Heading>

      <form onSubmit={handleSubmit}>
        <VStack spacing={4}>
          <FormControl isRequired isInvalid={error?.includes('hasło')}>
            <FormLabel>Nowe hasło</FormLabel>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Minimum 8 znaków"
              isDisabled={!token} // Wyłącz, jeśli nie ma tokena
            />
          </FormControl>

          <FormControl isRequired isInvalid={error?.includes('zgodne')}>
            <FormLabel>Potwierdź nowe hasło</FormLabel>
            <Input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Wpisz hasło ponownie"
              isDisabled={!token} // Wyłącz, jeśli nie ma tokena
            />
          </FormControl>

          {error && (
            <Alert status="error" borderRadius="md">
              <AlertIcon />
              {error}
            </Alert>
          )}

          <Button
            type="submit"
            colorScheme="blue"
            width="full"
            mt={4}
            isLoading={loading}
            isDisabled={!token} // Wyłącz, jeśli nie ma tokena
          >
            Zapisz nowe hasło
          </Button>

        </VStack>
      </form>

      <Text mt={6} textAlign="center">
        Pamiętasz hasło?{' '}
        <Button as={RouterLink} to="/login" colorScheme="blue" variant="link">
          Wróć do logowania
        </Button>
      </Text>
    </Box>
  );
}

export default ResetPasswordPage;