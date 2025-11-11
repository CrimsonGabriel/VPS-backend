import { Box } from '@chakra-ui/react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar'; // Importujemy istniejący Navbar

function ProtectedLayout() {
  return (
    <Box>
      <Navbar /> {/* Navbar jest teraz częścią tego layoutu */}
      {/* Padding dla zawartości pod navbarem */}
      <Box p={4} mt="60px">
        <Outlet /> {/* Tutaj będą renderowane nasze chronione strony */}
      </Box>
    </Box>
  );
}

export default ProtectedLayout;
