// Upewnij się, że ten plik jest w folderze pages/
import { useState } from 'react'
import {
  Box, Heading, Button, VStack,
  Textarea, Code, useToast
} from '@chakra-ui/react'

// ⭐️ DODAJ IMPORT KONTEKSTU AUTORYZACJI ⭐️
import { useAuth } from '../context/AuthContext'

function ApiTestPage() {
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  
  // ⭐️ POBIERZ FUNKCJĘ getToken Z KONTEKSTU ⭐️
  const { getToken } = useAuth()

  const handleTest = async (endpoint) => {
    setLoading(true)
    setResult(null)
    try {
      // 1. POBIERZ TOKEN
      const token = getToken() 

      // 2. USTAW NAGŁÓWKI (dodaj Authorization)
      const headers = {}
      if (token) {
          headers['Authorization'] = `Bearer ${token}`
      }

      const response = await fetch(endpoint, {
        headers: headers // Użyj nowego obiektu nagłówków
      })
      
      if (!response.ok) {
        // Jeśli serwer zwróci błąd (np. 401 lub 403)
        let errorText = await response.text();
        try {
            // Spróbuj sparsować jako JSON, jeśli użyłeś handlerów
            const errorJson = JSON.parse(errorText);
            errorText = errorJson.error || errorJson.message || `Błąd ${response.status} (${response.statusText})`;
        } catch (e) {
            // Jeśli to nie jest JSON, użyj surowego tekstu
            errorText = `Błąd ${response.status} (${response.statusText}): ${errorText}`;
        }
        throw new Error(errorText)
      }

      const data = await response.json()
      
      setResult(JSON.stringify(data, null, 2)) // Ładnie sformatowany JSON
      toast({
        title: 'Test udany!',
        description: `Pobrano dane z ${endpoint}`,
        status: 'success',
        duration: 3000,
        isClosable: true,
      })
      
    } catch (err) {
      setResult(`Błąd: ${err.message}`)
      toast({
        title: 'Błąd testu!',
        description: err.message,
        status: 'error',
        duration: 5000,
        isClosable: true,
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <VStack spacing={4} align="stretch">
      <Heading size="lg">Testowanie Endpointów REST API</Heading>
      
      <Button
        colorScheme="blue"
        onClick={() => handleTest('/status/json')}
        isLoading={loading}
      >
        Testuj /status/json (GET - Publiczny)
      </Button>
      
      <Button
        colorScheme="teal"
        onClick={() => handleTest('/data/android')}
        isLoading={loading}
        // Ta metoda teraz wysyła token!
      >
        Testuj /data/android (GET - Wymaga Auth, powinno działać!)
      </Button>

      {/* Kontener na wynik */}
      <Box p={4} bg="gray.900" borderRadius="md">
        <Heading size="sm" mb={2}>Wynik</Heading>
        <Textarea
          readOnly
          value={result || 'Oczekuję na test...'}
          minHeight="200px"
          fontFamily="mono"
        />
      </Box>
    </VStack>
  )
}

export default ApiTestPage