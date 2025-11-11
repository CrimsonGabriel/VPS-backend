import { Box, Heading, Text, Button, VStack, Icon, Flex } from '@chakra-ui/react';
import { WarningIcon } from '@chakra-ui/icons';
import { Link as RouterLink } from 'react-router-dom';

function ForbiddenPage() {
  return (
    <VStack spacing={6} align="center" justify="center" minH="80vh" px={4}>
      <Icon as={WarningIcon} w={20} h={20} color="red.500" />
      <Heading as="h1" size="2xl">
        403 - Dostęp Zablokowany
      </Heading>
      <Text fontSize="xl" color="gray.300" textAlign="center">
        Nie masz **wymaganych uprawnień** (roli) by wyświetlić tę stronę.
      </Text>
      <Text color="gray.400">
        Ta strona wymaga roli Administratora (ADMIN).
      </Text>
      <Flex gap={4} mt={4}>
        <Button as={RouterLink} to="/" colorScheme="gray" variant="outline" size="lg">
          Strona główna
        </Button>
        {/* Usunięto link do /login, bo użytkownik JEST zalogowany, tylko ma za niską rolę */}
      </Flex>
    </VStack>
  );
}

export default ForbiddenPage;