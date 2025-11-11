
import { Box, Flex, Heading, Link, Spacer, Button, Text } from '@chakra-ui/react'
import { Link as RouterLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext' 

function Navbar() {
  // ⭐️ POBIERAMY userEmail, isAdmin I logout ⭐️
  const { userEmail, logout, isAdmin } = useAuth();

  // Opcjonalne: Możemy ukryć linki dla ADMINA, jeśli nie ma roli ADMIN
  const userLink = (
    <Link as={RouterLink} to="/users" color="blue.300" fontWeight="medium">
      Użytkownicy
    </Link>
  );

  return (
    <Box
      bg="gray.900"
      px={4}
      py={3}
      borderBottom="1px"
      borderColor="gray.700"
      position="fixed" 
      top={0}
      left={0}
      right={0}
      zIndex={10} 
    >
      <Flex align="center">
        
        {/* LEWA STRONA (Status Zalogowania) */}
        <Flex align="center" gap={3}>
          {/* ⭐️ UŻYCIE POBRANEGO EMAILA ⭐️ */}
          <Text color="white" fontWeight="bold" fontSize={{ base: "sm", md: "md" }}>
            Witaj, {userEmail || 'Użytkowniku'}
          </Text>
          <Button colorScheme="red" size="sm" onClick={logout}>
            Wyloguj
          </Button>
        </Flex>

        <Spacer />

        {/* PRAWA STRONA (Linki Nawigacyjne) */}
        <Flex gap={4}>
          <Link as={RouterLink} to="/" color="blue.300" fontWeight="medium">
            Strona Główna
          </Link>
          <Link as={RouterLink} to="/status-ip" color="blue.300" fontWeight="medium">
            Status IP
          </Link>
		  {/* ⭐️ DODANY LINK DO KONTA ⭐️ */}
          <Link as={RouterLink} to="/konto" color="blue.300" fontWeight="medium">
            Moje Konto
          </Link>
          {/* ⭐️ Wyświetlamy link /users tylko dla ADMINA ⭐️ */}
          {isAdmin && userLink} 
          <Link as={RouterLink} to="/api-test" color="blue.300" fontWeight="medium">
            Test API
          </Link>
	      <Link as={RouterLink} to="/upload" color="blue.300" fontWeight="medium">
            Wyślij Plik
          </Link>
          <Link as={RouterLink} to="/files" color="blue.300" fontWeight="medium">
            Pobrane Pliki
          </Link>
        </Flex>
      </Flex>
    </Box>
  )
}

export default Navbar