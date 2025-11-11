import { Box, Heading, Text, VStack, Image } from '@chakra-ui/react';

function HomePage() {
  return (
    // Ten plik nie ma żadnych przycisków ani nagłówka!
    <Box minH="90vh"> 
      <VStack spacing={8} align="center" justify="center" minH="90vh" pt="80px" px={4}>
        <Heading as="h1" size={{ base: "xl", md: "2xl" }} textAlign="center">
          Witaj w Bazunia Serwer
        </Heading>
        
        <Image 
          src="https://images.unsplash.com/photo-1518770660439-4636190af475?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1170&q=80" 
          alt="Obrazek sensora"
          borderRadius="md"
          boxShadow="lg"
          maxW="600px"
          w="90%" 
        />
        
        <Text fontSize={{ base: "lg", md: "xl" }} color="gray.300" textAlign="center">
          System monitorowania i zarządzania.
        </Text>
      </VStack>
    </Box>
  );
}

export default HomePage;
