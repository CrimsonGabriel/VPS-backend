// 💾 src/pages/RegisterPage.jsx (NOWA WERSJA)

import { useState } from 'react';
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
import { useNavigate, Link as RouterLink } from 'react-router-dom';

function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  
  const toast = useToast();
  const navigate = useNavigate();

  // Prosta walidacja e-mail
  const validateEmail = (email) => {
    return String(email)
      .toLowerCase()
      .match(
        /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
      );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    // 1. Walidacja po stronie klienta
    if (!validateEmail(email)) {
      setError('Proszę podać poprawny adres e-mail.');
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
      // 2. Używamy tego samego endpointu co Android
      const response = await fetch('/api/auth/android/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      // 3. Obsługa odpowiedzi
      if (response.ok) {
        // SUKCES (200 OK)
        toast({
          title: 'Weryfikacja konta',
          description: 'Wysłaliśmy link aktywacyjny na Twój adres e-mail. Sprawdź skrzynkę (i folder SPAM), aby dokończyć rejestrację.',
          status: 'success',
          duration: 9000,
          isClosable: true,
        });
        navigate('/login'); // Przekieruj na stronę logowania

      } else if (response.status === 409) {
        // KONFLIKT (409 Conflict)
        setError('Ten adres e-mail jest już zajęty. Proszę użyć innego.');
      } else {
        // Inne błędy serwera
        setError('Wystąpił błąd serwera. Spróbuj ponownie później.');
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
        Utwórz nowe konto
      </Heading>

      <form onSubmit={handleSubmit}>
        <VStack spacing={4}>
          <FormControl isRequired isInvalid={error?.includes('e-mail')}>
            <FormLabel>Adres e-mail</FormLabel>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="twoj.email@przyklad.com"
            />
          </FormControl>

          <FormControl isRequired isInvalid={error?.includes('hasło')}>
            <FormLabel>Hasło</FormLabel>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Minimum 8 znaków"
            />
          </FormControl>

          <FormControl isRequired isInvalid={error?.includes('zgodne')}>
            <FormLabel>Potwierdź hasło</FormLabel>
            <Input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Wpisz hasło ponownie"
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
          >
            Zarejestruj się
          </Button>

        </VStack>
      </form>

      <Text mt={6} textAlign="center">
        Masz już konto?{' '}
        <Button as={RouterLink} to="/login" colorScheme="blue" variant="link">
          Zaloguj się
        </Button>
      </Text>
    </Box>
  );
}

export default RegisterPage;