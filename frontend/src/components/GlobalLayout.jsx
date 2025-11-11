import { Box } from '@chakra-ui/react';
import { Outlet, useLocation } from 'react-router-dom'; // <--- WAŻNY IMPORT
import { useAuth } from '../context/AuthContext';
import Navbar from './Navbar'; // Nasz pełny navbar
import PublicHeader from './PublicHeader'; // <--- WAŻNY IMPORT

function GlobalLayout() {
  const { isAuthenticated } = useAuth();
  const location = useLocation(); // <--- TA LINIA MUSI TU BYĆ
  
  let header = null;
  
  // 1. Jeśli zalogowany - ZAWSZE pokazuj pełny Navbar
  if (isAuthenticated) {
    header = <Navbar />;
  } 
  // 2. Jeśli gość I jest na stronie głównej - pokaż przyciski logowania
  else if (location.pathname === '/') { 
    header = <PublicHeader />;
  }
  // 3. Jeśli gość i na /login lub /register - nie pokazuj nic (header = null)

  // Padding jest potrzebny tylko, gdy jest pełny Navbar
  const padding = isAuthenticated ? "60px" : "0px";

  return (
    <Box>
      {header}
      <Box pt={padding}>
        <Outlet /> 
      </Box>
    </Box>
  );
}

export default GlobalLayout;
