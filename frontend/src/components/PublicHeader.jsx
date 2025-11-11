import { Button, Flex } from '@chakra-ui/react';
import { Link as RouterLink } from 'react-router-dom';

function PublicHeader() {
  return (
    <Flex
      position="absolute"
      top={{ base: "15px", md: "20px" }}
      right={{ base: "15px", md: "20px" }}
      gap={3}
      zIndex={10}
    >
      <Button as={RouterLink} to="/login" colorScheme="blue" size={{ base: "sm", md: "md" }}>
        Zaloguj się
      </Button>
      <Button as={RouterLink} to="/register" variant="outline" size={{ base: "sm", md: "md" }}>
        Zarejestruj się
      </Button>
    </Flex>
  );
}

export default PublicHeader;
